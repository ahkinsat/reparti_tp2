package tp2.bo;

import tp2.Config;
import tp2.Database;
import tp2.RabbitMqManager;
import tp2.models.Message;
import tp2.models.Sale;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class OutboxProcessor implements Runnable {
    private final int boNumber;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private com.rabbitmq.client.Channel channel;

    public OutboxProcessor(int boNumber) { this.boNumber = boNumber; }

    public void start() {
        if (running.compareAndSet(false, true)) {
            new Thread(this, "OutboxProcessor-BO"+boNumber).start();
        }
    }

    public void stop() { running.set(false); }
    public boolean isRunning() { return running.get(); }

    @Override
    public void run() {
        try {
            channel = RabbitMqManager.createChannel();
            RabbitMqManager.declareQueue(channel);
        } catch (Exception e) {
            System.err.println("BO"+boNumber+" failed to connect to RabbitMQ: "+e.getMessage());
            running.set(false);
            return;
        }

        while (running.get()) {
            try (Connection conn = Database.getBoConnection(boNumber)) {
                conn.setAutoCommit(false);
                // Fetch unsent events
                String selectSql = "SELECT event_id, sale_id, event_type, event_time, payload, retry_count FROM outbox WHERE sent = false ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED";
                try (PreparedStatement ps = conn.prepareStatement(selectSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        if (!running.get()) break;
                        UUID eventId = (UUID) rs.getObject("event_id");
                        UUID saleId = (UUID) rs.getObject("sale_id");
                        String eventType = rs.getString("event_type");
                        Instant eventTime = rs.getObject("event_time", Instant.class);
                        String payload = rs.getString("payload");
                        int retryCount = rs.getInt("retry_count");

                        try {
                            Message msg;
                            if ("WRITE".equals(eventType)) {
                                Sale sale = tp2.Util.saleFromJson(payload);
                                msg = Message.forWrite(eventId, saleId, eventTime, sale);
                            } else {
                                msg = Message.forDelete(eventId, saleId, eventTime);
                            }
                            channel.basicPublish("", Config.rmqQueue(), null, msg.toJson().getBytes());
                            // Success: delete from outbox
                            try (PreparedStatement del = conn.prepareStatement("DELETE FROM outbox WHERE event_id = ?")) {
                                del.setObject(1, eventId);
                                del.executeUpdate();
                            }
                        } catch (Exception e) {
                            // Send failed – increment retry
                            try (PreparedStatement upd = conn.prepareStatement("UPDATE outbox SET retry_count = retry_count + 1 WHERE event_id = ?")) {
                                upd.setObject(1, eventId);
                                upd.executeUpdate();
                            }
                            if (retryCount + 1 >= 3) {
                                System.err.println("BO"+boNumber+" event "+eventId+" failed 3 times, leaving for manual inspection");
                            }
                        }
                    }
                }
                conn.commit();
                Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            } catch (Exception e) {
                if (running.get()) e.printStackTrace();
            }
        }
    }
}
