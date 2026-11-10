package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.persistence.entities.Ticker;

public class TickerMapper {

  public static TickerDto toDto(Ticker ticker) {
    if (ticker == null) {
      return null;
    }
    return TickerDto.builder()
        .id(ticker.getTickerId())
        .symbol(ticker.getTickerSymbol())
        .name(ticker.getTickerName())
        .token(ticker.getToken())
        .scripSymbol(ticker.getScripSymbol())
        .isActive(ticker.getIsActive())
        .build();
  }
}
