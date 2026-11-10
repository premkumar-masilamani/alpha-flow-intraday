package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record TickerDto(
    @JsonProperty("id") Long id,
    @JsonProperty("symbol") String symbol,
    @JsonProperty("name") String name,
    @JsonProperty("token") String token,
    @JsonProperty("scripSymbol") String scripSymbol,
    @JsonProperty("isActive") Boolean isActive) {

  public Long tickerId() {
    return id;
  }

  public String tickerSymbol() {
    return symbol;
  }

  public String tickerName() {
    return name;
  }

  public static class TickerDtoBuilder {
    public TickerDtoBuilder tickerId(Long tickerId) {
      this.id = tickerId;
      return this;
    }

    public TickerDtoBuilder tickerSymbol(String tickerSymbol) {
      this.symbol = tickerSymbol;
      return this;
    }

    public TickerDtoBuilder tickerName(String tickerName) {
      this.name = tickerName;
      return this;
    }
  }
}
