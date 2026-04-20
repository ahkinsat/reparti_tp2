package tp2.bo;

import tp2.Database;
import tp2.Util;
import tp2.models.Sale;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public class BranchOfficeApi {
    private final int boNumber;

    public BranchOfficeApi(int boNumber) { this.boNumber = boNumber; }

    public record WriteResult(UUID saleId, UUID eventId) {}

    public WriteResult writeSale(Sale sale) throws SQLException {
        try (Connection conn = Database.getBoConnection(boNumber)) {
            conn.setAutoCommit(false);
            UUID eventId = UUID.randomUUID();
            // Insert sale
            String insertSale = "INSERT INTO sales (sale_id, sale_date, region, product, quantity, cost, amount, tax, total) VALUES (?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSale)) {
                ps.setObject(1, sale.saleId());
                ps.setObject(2, sale.saleDate());
                ps.setString(3, sale.region());
                ps.setString(4, sale.product());
                ps.setInt(5, sale.quantity());
                ps.setBigDecimal(6, sale.cost());
                ps.setBigDecimal(7, sale.amount());
                ps.setBigDecimal(8, sale.tax());
                ps.setBigDecimal(9, sale.total());
                ps.executeUpdate();
            }
            // Insert outbox
            String insertOutbox = "INSERT INTO outbox (event_id, sale_id, event_type, event_time, payload, sent, retry_count) VALUES (?,?,?,?,?::jsonb,false,0)";
            try (PreparedStatement ps = conn.prepareStatement(insertOutbox)) {
                ps.setObject(1, eventId);
                ps.setObject(2, sale.saleId());
                ps.setString(3, "WRITE");
                ps.setObject(4, java.sql.Timestamp.from(Instant.now()));
                ps.setString(5, Util.toJson(sale));
                ps.executeUpdate();
            }
            conn.commit();
            return new WriteResult(sale.saleId(), eventId);
        }
    }

    public void deleteSale(UUID saleId) throws SQLException {
        try (Connection conn = Database.getBoConnection(boNumber)) {
            conn.setAutoCommit(false);
            // Delete from sales (even if not exists)
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sales WHERE sale_id = ?")) {
                ps.setObject(1, saleId);
                ps.executeUpdate();
            }
            // Insert DELETE outbox event
            UUID eventId = UUID.randomUUID();
            String insertOutbox = "INSERT INTO outbox (event_id, sale_id, event_type, event_time, payload, sent, retry_count) VALUES (?,?,?,?,?::jsonb,false,0)";
            try (PreparedStatement ps = conn.prepareStatement(insertOutbox)) {
                ps.setObject(1, eventId);
                ps.setObject(2, saleId);
                ps.setString(3, "DELETE");
                ps.setObject(4, java.sql.Timestamp.from(Instant.now()));
                ps.setString(5, "{}");
                ps.executeUpdate();
            }
            conn.commit();
        }
    }

    public Optional<Sale> getSale(UUID saleId) throws SQLException {
        try (Connection conn = Database.getBoConnection(boNumber);
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM sales WHERE sale_id = ?")) {
            ps.setObject(1, saleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(new Sale(
                    (UUID) rs.getObject("sale_id"),
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

    public List<Sale> listSales(int limit) throws SQLException {
        List<Sale> list = new ArrayList<>();
        try (Connection conn = Database.getBoConnection(boNumber);
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM sales ORDER BY sale_date DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Sale(
                    (UUID) rs.getObject("sale_id"),
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

    public List<OutboxRecord> listEvents(int limit) throws SQLException {
        List<OutboxRecord> list = new ArrayList<>();
        try (Connection conn = Database.getBoConnection(boNumber);
             PreparedStatement ps = conn.prepareStatement("SELECT event_id, sale_id, event_type, event_time, payload, retry_count, sent FROM outbox ORDER BY created_at DESC LIMIT ?")) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new OutboxRecord(
                    (UUID) rs.getObject("event_id"),
                    (UUID) rs.getObject("sale_id"),
                    rs.getString("event_type"),
                    rs.getObject("event_time", java.sql.Timestamp.class).toInstant(),
                    rs.getString("payload"),
                    rs.getInt("retry_count"),
                    rs.getBoolean("sent")
                ));
            }
        }
        return list;
    }

    public OutboxRecord getEvent(UUID eventId) throws SQLException {
        try (Connection conn = Database.getBoConnection(boNumber);
             PreparedStatement ps = conn.prepareStatement("SELECT event_id, sale_id, event_type, event_time, payload, retry_count, sent FROM outbox WHERE event_id = ?")) {
            ps.setObject(1, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new OutboxRecord(
                    (UUID) rs.getObject("event_id"),
                    (UUID) rs.getObject("sale_id"),
                    rs.getString("event_type"),
                    rs.getObject("event_time", java.sql.Timestamp.class).toInstant(),
                    rs.getString("payload"),
                    rs.getInt("retry_count"),
                    rs.getBoolean("sent")
                );
            }
            return null;
        }
    }

    public String listSalesToCsv(int limit) throws SQLException {
        List<Sale> sales = listSales(limit);
        StringBuilder sb = new StringBuilder();
        sb.append("sale_id,sale_date,region,product,quantity,cost,amount,tax,total\n");
        for (Sale s : sales) {
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
        Sale s = sale.get();
        return String.format("%s,%s,%s,%s,%d,%.2f,%.2f,%.2f,%.2f\n",
            s.saleId(), s.saleDate(), s.region(), s.product(), s.quantity(),
            s.cost(), s.amount(), s.tax(), s.total());
    }

    public String listEventsToCsv(int limit) throws SQLException {
        List<OutboxRecord> events = listEvents(limit);
        StringBuilder sb = new StringBuilder();
        sb.append("event_id,sale_id,event_type,event_time,payload,retry_count\n");
        for (OutboxRecord e : events) {
            sb.append(String.format("%s,%s,%s,%s,%s,%d\n",
                e.eventId(), e.saleId(), e.eventType(), e.eventTime(), 
                e.payload(), e.retryCount()));
        }
        return sb.toString();
    }

    public String getEventToCsv(UUID eventId) throws SQLException {
        OutboxRecord ev = getEvent(eventId);
        if (ev == null) {
            return "NOT FOUND\n";
        }
        return String.format("%s,%s,%s,%s,%s,%d\n",
            ev.eventId(), ev.saleId(), ev.eventType(), ev.eventTime(), 
            ev.payload(), ev.retryCount());
    }
}
