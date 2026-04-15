package tp2.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Sale(
    UUID saleId,
    LocalDate saleDate,
    String region,
    String product,
    int quantity,
    BigDecimal cost,
    BigDecimal amount,
    BigDecimal tax,
    BigDecimal total
) {}
