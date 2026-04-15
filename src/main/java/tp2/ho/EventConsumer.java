package tp2.ho;

import com.rabbitmq.client.*;
import tp2.Config;
import tp2.Database;
import tp2.RabbitMqManager;
import tp2.models.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventConsumer implements Runnable {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Channel channel;
    private String consumerTag;

    public void start() {
        if (running.compareAndSet(false, true)) {
            new Thread(this, "EventConsumer-HO").start();
        }
    }

    public void stop() { running.set(false); }
    public boolean isRunning() { return running.get(); }

    @Override
    public void run() {
        try {
            channel = RabbitMqManager.createChannel();
            RabbitMqManager.declareQueue(channel);
            channel.basicQos(1);
            DeliverCallback deliverCallback = (tag, delivery) -> {
                try {
                    String json = new String(delivery.getBody());
                    Message msg = Message.fromJson(json);
                    processMessage(msg);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    Map<String, Object> headers = delivery.getProperties().getHeaders();
                    int retryCount = (headers != null && headers.containsKey("x-retry-count")) ?
                                     (int) headers.get("x-retry-count") : 0;
                    if (retryCount >= 3) {
                        System.err.println("Event failed after 3 retries, discarding: " + new String(delivery.getBody()));
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    } else {
                        Map<String, Object> newHeaders = new HashMap<>(headers != null ? headers : Map.of());
                        newHeaders.put("x-retry-count", retryCount + 1);
                        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                                .headers(newHeaders).build();
                        channel.basicPublish("", Config.rmqQueue(), props, delivery.getBody());
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                }
            };
            consumerTag = channel.basicConsume(Config.rmqQueue(), false, deliverCallback, consumerTag -> {});
            while (running.get()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            if (running.get()) e.printStackTrace();
        }
    }

    private void processMessage(Message msg) throws Exception {
        try (Connection conn = Database.getHoConnection()) {
            conn.setAutoCommit(false);
            // 1. Idempotency
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM events WHERE event_id = ?")) {
                ps.setObject(1, msg.eventId());
                ResultSet rs = ps.executeQuery();
                exists = rs.next();
            }
            if (exists) {
                conn.commit();
                return;
            }

            // 2. Get current sync state
            Instant lastTime = null;
            UUID lastId = null;
            try (PreparedStatement ps = conn.prepareStatement("SELECT last_event_time, last_event_id FROM syncs WHERE sale_id = ?")) {
                ps.setObject(1, msg.saleId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    lastTime = rs.getObject("last_event_time", Instant.class);
                    lastId = (UUID) rs.getObject("last_event_id");
                }
            }

            boolean isNewer;
            if (lastTime == null) {
                isNewer = true;
            } else if (msg.eventTime().isAfter(lastTime)) {
                isNewer = true;
            } else if (msg.eventTime().equals(lastTime)) {
                isNewer = msg.eventId().compareTo(lastId) > 0;
            } else {
                isNewer = false;
            }

            if (isNewer) {
                if ("WRITE".equals(msg.type())) {
                    // upsert
                    String upsert = "INSERT INTO sales (sale_id, sale_date, region, product, quantity, cost, amount, tax, total) " +
                                    "VALUES (?,?,?,?,?,?,?,?,?) ON CONFLICT (sale_id) DO UPDATE SET " +
                                    "sale_date = EXCLUDED.sale_date, region = EXCLUDED.region, product = EXCLUDED.product, " +
                                    "quantity = EXCLUDED.quantity, cost = EXCLUDED.cost, amount = EXCLUDED.amount, " +
                                    "tax = EXCLUDED.tax, total = EXCLUDED.total";
                    try (PreparedStatement ps = conn.prepareStatement(upsert)) {
                        ps.setObject(1, msg.sale().saleId());
                        ps.setObject(2, msg.sale().saleDate());
                        ps.setString(3, msg.sale().region());
                        ps.setString(4, msg.sale().product());
                        ps.setInt(5, msg.sale().quantity());
                        ps.setBigDecimal(6, msg.sale().cost());
                        ps.setBigDecimal(7, msg.sale().amount());
                        ps.setBigDecimal(8, msg.sale().tax());
                        ps.setBigDecimal(9, msg.sale().total());
                        ps.executeUpdate();
                    }
                } else if ("DELETE".equals(msg.type())) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sales WHERE sale_id = ?")) {
                        ps.setObject(1, msg.saleId());
                        ps.executeUpdate();
                    }
                }
                // Update syncs
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO syncs (sale_id, last_event_time, last_event_id) VALUES (?,?,?) " +
                        "ON CONFLICT (sale_id) DO UPDATE SET last_event_time = EXCLUDED.last_event_time, last_event_id = EXCLUDED.last_event_id")) {
                    ps.setObject(1, msg.saleId());
                    ps.setObject(2, msg.eventTime());
                    ps.setObject(3, msg.eventId());
                    ps.executeUpdate();
                }
                // Insert event with applied_at
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO events (event_id, sale_id, event_type, event_time, applied_at) VALUES (?,?,?,?,?)")) {
                    ps.setObject(1, msg.eventId());
                    ps.setObject(2, msg.saleId());
                    ps.setString(3, msg.type());
                    ps.setObject(4, msg.eventTime());
                    ps.setObject(5, Instant.now());
                    ps.executeUpdate();
                }
            } else {
                // Older: record event without applied_at
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO events (event_id, sale_id, event_type, event_time, applied_at) VALUES (?,?,?,?,?)")) {
                    ps.setObject(1, msg.eventId());
                    ps.setObject(2, msg.saleId());
                    ps.setString(3, msg.type());
                    ps.setObject(4, msg.eventTime());
                    ps.setNull(5, java.sql.Types.TIMESTAMP);
                    ps.executeUpdate();
                }
            }
            conn.commit();
        }
    }
}
