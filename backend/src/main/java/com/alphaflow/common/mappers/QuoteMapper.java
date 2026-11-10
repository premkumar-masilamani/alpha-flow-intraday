package com.alphaflow.common.mappers;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.persistence.entities.LiveQuote;
import java.math.BigDecimal;

public class QuoteMapper {

  public static QuoteDto toDto(LiveQuote liveQuote) {
    if (liveQuote != null) {
      return QuoteDto.builder()
          .currentPrice(liveQuote.getLastPrice())
          .open(liveQuote.getOpen())
          .high(liveQuote.getHigh())
          .low(liveQuote.getLow())
          .close(liveQuote.getClose())
          .changePercent(liveQuote.getChangePercent())
          .timestamp(liveQuote.getQuoteTime() != null ? liveQuote.getQuoteTime().toString() : "")
          .build();
    } else {
      return defaultZeroDto();
    }
  }

  public static QuoteDto defaultZeroDto() {
    return QuoteDto.builder()
        .currentPrice(BigDecimal.ZERO)
        .open(BigDecimal.ZERO)
        .high(BigDecimal.ZERO)
        .low(BigDecimal.ZERO)
        .close(BigDecimal.ZERO)
        .changePercent(BigDecimal.ZERO)
        .timestamp("")
        .build();
  }
}
