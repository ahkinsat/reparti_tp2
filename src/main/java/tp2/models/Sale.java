package tp2.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Sale(
    @JsonProperty UUID saleId,
    @JsonProperty LocalDate saleDate,
    @JsonProperty String region,
    @JsonProperty String product,
    @JsonProperty int quantity,
    @JsonProperty BigDecimal cost,
    @JsonProperty BigDecimal amount,
    @JsonProperty BigDecimal tax,
    @JsonProperty BigDecimal total
) {
    // Aliases for Jackson
    public UUID getSaleId() { return saleId; }
    public LocalDate getSaleDate() { return saleDate; }
    public String getRegion() { return region; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public BigDecimal getCost() { return cost; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getTotal() { return total; }
}
