package tp2.ho;

import java.time.Instant;
import java.util.UUID;

public record SyncRecord(
    UUID saleId,
    Instant lastEventTime,
    UUID lastEventId
) {}
