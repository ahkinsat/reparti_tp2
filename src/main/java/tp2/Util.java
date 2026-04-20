package tp2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import tp2.models.Sale;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.UUID;

public class Util {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static Sale saleFromArgs(String[] args, int startIndex) {
        return new Sale(
            UUID.randomUUID(),
            java.time.LocalDate.parse(args[startIndex]),
            args[startIndex + 1],
            args[startIndex + 2],
            Integer.parseInt(args[startIndex + 3]),
            new java.math.BigDecimal(args[startIndex + 4]),
            new java.math.BigDecimal(args[startIndex + 5]),
            new java.math.BigDecimal(args[startIndex + 6]),
            new java.math.BigDecimal(args[startIndex + 7])
        );
    }

    public static UUID parseUuid(String s) {
        return UUID.fromString(s);
    }

    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            System.err.println("JSON serialization error: " + e.getMessage());
            return "{}";
        }
    }

    public static Sale saleFromJson(String json) {
        try {
            return MAPPER.readValue(json, Sale.class);
        } catch (Exception e) {
            System.err.println("JSON deserialization error: " + e.getMessage());
            return null;
        }
    }
}
