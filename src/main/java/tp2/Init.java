package tp2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Init {
    private static final Path MARKER = Path.of("/tmp/tp2_init.done");

    public static void run() throws SQLException, IOException {
        if (Files.exists(MARKER)) {
            System.out.println("Already initialized. Skipping.");
            return;
        }

        // HO tables
        try (Connection conn = Database.getHoConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                "sale_id UUID PRIMARY KEY, sale_date DATE, region VARCHAR(50), product VARCHAR(100), " +
                "quantity INT, cost DECIMAL(10,2), amount DECIMAL(10,2), tax DECIMAL(10,2), total DECIMAL(10,2))");
            stmt.execute("CREATE TABLE IF NOT EXISTS events (" +
                "event_id UUID PRIMARY KEY, sale_id UUID NOT NULL, event_type VARCHAR(10), " +
                "event_time TIMESTAMPTZ NOT NULL, applied_at TIMESTAMPTZ)");
            stmt.execute("CREATE TABLE IF NOT EXISTS syncs (" +
                "sale_id UUID PRIMARY KEY, last_event_time TIMESTAMPTZ NOT NULL, last_event_id UUID NOT NULL)");
        }

        // BO1 tables
        try (Connection conn = Database.getBoConnection(1); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                "sale_id UUID PRIMARY KEY, sale_date DATE, region VARCHAR(50), product VARCHAR(100), " +
                "quantity INT, cost DECIMAL(10,2), amount DECIMAL(10,2), tax DECIMAL(10,2), total DECIMAL(10,2))");
            stmt.execute("CREATE TABLE IF NOT EXISTS outbox (" +
                "event_id UUID PRIMARY KEY, sale_id UUID NOT NULL, event_type VARCHAR(10), " +
                "event_time TIMESTAMPTZ NOT NULL, payload JSONB, sent BOOLEAN DEFAULT FALSE, " +
                "retry_count INT DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW())");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON outbox(sent, created_at)");
        }

        // BO2 tables (same as BO1)
        try (Connection conn = Database.getBoConnection(2); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sales (" +
                "sale_id UUID PRIMARY KEY, sale_date DATE, region VARCHAR(50), product VARCHAR(100), " +
                "quantity INT, cost DECIMAL(10,2), amount DECIMAL(10,2), tax DECIMAL(10,2), total DECIMAL(10,2))");
            stmt.execute("CREATE TABLE IF NOT EXISTS outbox (" +
                "event_id UUID PRIMARY KEY, sale_id UUID NOT NULL, event_type VARCHAR(10), " +
                "event_time TIMESTAMPTZ NOT NULL, payload JSONB, sent BOOLEAN DEFAULT FALSE, " +
                "retry_count INT DEFAULT 0, created_at TIMESTAMPTZ DEFAULT NOW())");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON outbox(sent, created_at)");
        }

        // Declare RabbitMQ queue
        try (var channel = RabbitMqManager.createChannel()) {
            RabbitMqManager.declareQueue(channel);
        }

        Files.createFile(MARKER);
        System.out.println("Initialization complete.");
    }
}
