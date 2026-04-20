package tp2.ho;

import tp2.Database;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class HeadOfficeApi {
    public List<SaleView> listSales(int limit) throws SQLException {
        List<SaleView> list = new ArrayList<>();
        try (Connection conn = Database.getHoConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT sale_id, sale_date, region, product, quantity, cost, amount, tax, total FROM sales ORDER BY sale_date DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new SaleView(
                    rs.getObject("sale_id", java.util.UUID.class),
                    rs.getObject("sale_date", java.time.LocalDate.class),
                    rs.getString("region"),
                    rs.getString("product"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("cost"),
                    rs.getBigDecimal("amount"),
                    rs.getBigDecimal("tax"),
                    rs.getBigDecimal("total")
                ));
            }
        }
        return list;
    }

    public Optional<SaleView> getSale(UUID saleId) throws SQLException {
        try (Connection conn = Database.getHoConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT sale_id, sale_date, region, product, quantity, cost, amount, tax, total FROM sales WHERE sale_id = ?")) {
            ps.setObject(1, saleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new SaleView(
                    rs.getObject("sale_id", java.util.UUID.class),
                    rs.getObject("sale_date", java.time.LocalDate.class),
                    rs.getString("region"),
                    rs.getString("product"),
                    rs.getInt("quantity"),
                    rs.getBigDecimal("cost"),
                    rs.getBigDecimal("amount"),
                    rs.getBigDecimal("tax"),
                    rs.getBigDecimal("total")
                ));
            }
            return Optional.empty();
        }
    }

    public List<EventView> listEvents(int limit) throws SQLException {
        List<EventView> list = new ArrayList<>();
        try (Connection conn = Database.getHoConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT event_id, sale_id, event_type, event_time, applied_at FROM events ORDER BY event_time DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp appliedAtTs = rs.getObject("applied_at", java.sql.Timestamp.class);
                Instant appliedAt = appliedAtTs != null ? appliedAtTs.toInstant() : null;
                list.add(new EventView(
                    rs.getObject("event_id", java.util.UUID.class),
                    rs.getObject("sale_id", java.util.UUID.class),
                    rs.getString("event_type"),
                    rs.getObject("event_time", java.sql.Timestamp.class).toInstant(),
                    appliedAt
                ));
            }
        }
        return list;
    }

    public Optional<EventView> getEvent(UUID eventId) throws SQLException {
        try (Connection conn = Database.getHoConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT event_id, sale_id, event_type, event_time, applied_at FROM events WHERE event_id = ?")) {
            ps.setObject(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp appliedAtTs = rs.getObject("applied_at", java.sql.Timestamp.class);
                Instant appliedAt = appliedAtTs != null ? appliedAtTs.toInstant() : null;
                return Optional.of(new EventView(
                    rs.getObject("event_id", java.util.UUID.class),
                    rs.getObject("sale_id", java.util.UUID.class),
                    rs.getString("event_type"),
                    rs.getObject("event_time", java.sql.Timestamp.class).toInstant(),
                    appliedAt
                ));
            }
            return Optional.empty();
        }
    }

    public List<SyncRecord> listSyncs(int limit) throws SQLException {
        List<SyncRecord> list = new ArrayList<>();
        try (Connection conn = Database.getHoConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT sale_id, last_event_time, last_event_id FROM syncs ORDER BY last_event_time DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new SyncRecord(
                    rs.getObject("sale_id", java.util.UUID.class),
                    rs.getObject("last_event_time", java.sql.Timestamp.class).toInstant(),
                    rs.getObject("last_event_id", java.util.UUID.class)
                ));
            }
        }
        return list;
    }

    public Optional<SyncRecord> getSync(UUID saleId) throws SQLException {
        try (Connection conn = Database.getHoConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT sale_id, last_event_time, last_event_id FROM syncs WHERE sale_id = ?")) {
            ps.setObject(1, saleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new SyncRecord(
                    rs.getObject("sale_id", java.util.UUID.class),
                    rs.getObject("last_event_time", java.sql.Timestamp.class).toInstant(),
                    rs.getObject("last_event_id", java.util.UUID.class)
                ));
            }
            return Optional.empty();
        }   
    }

    public String listSalesToCsv(int limit) throws SQLException {
        List<SaleView> sales = listSales(limit);
        StringBuilder sb = new StringBuilder();
        sb.append("sale_id,sale_date,region,product,quantity,cost,amount,tax,total\n");
        for (SaleView s : sales) {
            sb.append(String.format("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f\n",
                s.saleId(), s.saleDate(), s.region(), s.product(), s.quantity(),
                s.cost(), s.amount(), s.tax(), s.total()));
        }
        return sb.toString();
    }

    public String getSaleToCsv(UUID saleId) throws SQLException {
        var sale = getSale(saleId);
        if (sale.isEmpty()) {
            return "NOT FOUND\n";
        }
        SaleView s = sale.get();
        return String.format("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f\n",
            s.saleId(), s.saleDate(), s.region(), s.product(), s.quantity(),
            s.cost(), s.amount(), s.tax(), s.total());
    }

    public String listEventsToCsv(int limit) throws SQLException {
        List<EventView> events = listEvents(limit);
        StringBuilder sb = new StringBuilder();
        sb.append("event_id,sale_id,event_type,event_time,applied_at\n");
        for (EventView e : events) {
            sb.append(String.format("%s,%s,%s,%s,%s\n",
                e.eventId(), e.saleId(), e.eventType(), e.eventTime(), e.appliedAt()));
        }
        return sb.toString();
    }

    public String getEventToCsv(UUID eventId) throws SQLException {
        var ev = getEvent(eventId);
        if (ev.isEmpty()) {
            return "NOT FOUND\n";
        }
        EventView e = ev.get();
        return String.format("%s,%s,%s,%s,%s\n",
            e.eventId(), e.saleId(), e.eventType(), e.eventTime(), e.appliedAt());
    }

    public String listSyncsToCsv(int limit) throws SQLException {
        List<SyncRecord> syncs = listSyncs(limit);
        StringBuilder sb = new StringBuilder();
        sb.append("sale_id,last_event_time,last_event_id\n");
        for (SyncRecord s : syncs) {
            sb.append(String.format("%s,%s,%s\n", s.saleId(), s.lastEventTime(), s.lastEventId()));
        }
        return sb.toString();
    }

    public String getSyncToCsv(UUID saleId) throws SQLException {
        var sync = getSync(saleId);
        if (sync.isEmpty()) {
            return "NOT FOUND\n";
        }
        SyncRecord s = sync.get();
        return String.format("%s,%s,%s\n", s.saleId(), s.lastEventTime(), s.lastEventId());
    }

    // View records for CSV output
    public record SaleView(UUID saleId, java.time.LocalDate saleDate, String region, String product,
                           int quantity, java.math.BigDecimal cost, java.math.BigDecimal amount,
                           java.math.BigDecimal tax, java.math.BigDecimal total) {}
    public record EventView(UUID eventId, UUID saleId, String eventType, java.time.Instant eventTime, java.time.Instant appliedAt) {}
    public record SyncRecord(UUID saleId, java.time.Instant lastEventTime, UUID lastEventId) {}
}
