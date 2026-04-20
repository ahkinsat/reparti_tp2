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

public class OutboxProcessor {
    private final int boNumber;
    private com.rabbitmq.client.Channel channel;

    public OutboxProcessor(int boNumber) {
        this.boNumber = boNumber;
    }

    public void run() {
        System.err.println("BO" + boNumber + " processor starting...");

        // Infinite retry for RabbitMQ connection
        while (true) {
            try {
                channel = RabbitMqManager.createChannel();
                RabbitMqManager.declareQueue(channel);
                System.err.println("BO" + boNumber + " connected to RabbitMQ");
                break;
            } catch (Exception e) {
                System.err.println("BO" + boNumber + " waiting for RabbitMQ: " + e.getMessage());
                try { Thread.sleep(10000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }

        // Main processing loop
        while (true) {
            try (Connection conn = Database.getBoConnection(boNumber)) {
                conn.setAutoCommit(false);
                String selectSql = "SELECT event_id, sale_id, event_type, event_time, payload, retry_count FROM outbox WHERE sent = false ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED";
                try (PreparedStatement ps = conn.prepareStatement(selectSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID eventId = (UUID) rs.getObject("event_id");
                        UUID saleId = (UUID) rs.getObject("sale_id");
                        String eventType = rs.getString("event_type");
                        java.sql.Timestamp ts = rs.getObject("event_time", java.sql.Timestamp.class);
                        Instant eventTime = ts != null ? ts.toInstant() : null;
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
                            System.err.println("BO"+boNumber+" sending event " + eventId + " to queue " + Config.rmqQueue());
                            String json = msg.toJson();
                            System.err.println("Message: " + json);
                            channel.basicPublish("", Config.rmqQueue(), null, json.getBytes());
                            // Success: delete from outbox
                            try (PreparedStatement del = conn.prepareStatement("DELETE FROM outbox WHERE event_id = ?")) {
                                del.setObject(1, eventId);
                                del.executeUpdate();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();  // ← ADD THIS to see real error
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
                System.err.println("BO" + boNumber + " error in main loop: " + e.getMessage());
                try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java tp2.bo.OutboxProcessor <bo_number>");
            System.exit(1);
        }
        int bo = Integer.parseInt(args[0]);
        new OutboxProcessor(bo).run();
    }
}
