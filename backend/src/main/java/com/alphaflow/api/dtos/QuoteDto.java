package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record QuoteDto(
    @JsonProperty("currentPrice") BigDecimal currentPrice,
    @JsonProperty("open") BigDecimal open,
    @JsonProperty("high") BigDecimal high,
    @JsonProperty("low") BigDecimal low,
    @JsonProperty("close") BigDecimal close,
    @JsonProperty("changePercent") BigDecimal changePercent,
    @JsonProperty("timestamp") String timestamp) {

  public QuoteDto(BigDecimal currentPrice, String timestamp) {
    this(
        currentPrice,
        currentPrice,
        currentPrice,
        currentPrice,
        currentPrice,
        BigDecimal.ZERO,
        timestamp);
  }
}
