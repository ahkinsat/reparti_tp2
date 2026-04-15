package tp2.bo;

import java.time.Instant;
import java.util.UUID;

public record OutboxRecord(
    UUID eventId,
    UUID saleId,
    String eventType,
    Instant eventTime,
    String payload,   // JSON of Sale for WRITE, "{}" for DELETE
    int retryCount
) {}
