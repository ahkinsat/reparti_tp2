package tp2.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;

public record Message(
    UUID eventId,
    String type,      // "WRITE" or "DELETE"
    Instant eventTime,
    UUID saleId,
    Sale sale         // null for DELETE
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String toJson() throws JsonProcessingException {
        return MAPPER.writeValueAsString(this);
    }

    public static Message fromJson(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, Message.class);
    }

    public static Message forWrite(UUID eventId, UUID saleId, Instant eventTime, Sale sale) {
        return new Message(eventId, "WRITE", eventTime, saleId, sale);
    }

    public static Message forDelete(UUID eventId, UUID saleId, Instant eventTime) {
        return new Message(eventId, "DELETE", eventTime, saleId, null);
    }
}
