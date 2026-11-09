package com.alphaflow.engine.downloaders.angelone.dtos;

import java.math.BigDecimal;

public record AngelOneQuote(
    BigDecimal lastPrice,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal changePercent,
    String timestamp) {

  public AngelOneQuote(BigDecimal lastPrice, String timestamp) {
    this(lastPrice, lastPrice, lastPrice, lastPrice, lastPrice, BigDecimal.ZERO, timestamp);
  }
}
