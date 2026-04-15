package tp2;

import com.fasterxml.jackson.databind.ObjectMapper;
import tp2.models.Sale;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Util {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Convert ResultSet to CSV string (with or without header)
    public static String resultSetToCsv(ResultSet rs, boolean includeHeader) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        StringBuilder sb = new StringBuilder();

        if (includeHeader) {
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) sb.append(",");
                sb.append(meta.getColumnName(i));
            }
            sb.append("\n");
        }

        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) sb.append(",");
                String value = rs.getString(i);
                if (value != null) sb.append(value);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public static Sale saleFromArgs(String[] args, int startIndex) {
        return new Sale(
            UUID.randomUUID(),
            java.time.LocalDate.parse(args[startIndex]),
            args[startIndex+1],
            args[startIndex+2],
            Integer.parseInt(args[startIndex+3]),
            new java.math.BigDecimal(args[startIndex+4]),
            new java.math.BigDecimal(args[startIndex+5]),
            new java.math.BigDecimal(args[startIndex+6]),
            new java.math.BigDecimal(args[startIndex+7])
        );
    }

    public static UUID parseUuid(String s) {
        return UUID.fromString(s);
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static Sale saleFromJson(String json) {
        try {
            return MAPPER.readValue(json, Sale.class);
        } catch (Exception e) {
            return null;
        }
    }
}
