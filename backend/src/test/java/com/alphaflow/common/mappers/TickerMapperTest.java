package com.alphaflow.common.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.persistence.entities.Ticker;
import org.junit.jupiter.api.Test;

class TickerMapperTest {

  @Test
  void testToDtoSuccess() {
    Ticker ticker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .token("99926000")
            .scripSymbol("Nifty 50")
            .isActive(true)
            .build();

    TickerDto dto = TickerMapper.toDto(ticker);
    assertNotNull(dto);
    assertEquals(1L, dto.id());
    assertEquals("NIFTY50", dto.symbol());
    assertEquals("NIFTY 50", dto.name());
    assertEquals("99926000", dto.token());
    assertEquals("Nifty 50", dto.scripSymbol());
    assertEquals(true, dto.isActive());
  }

  @Test
  void testToDtoNull() {
    assertNull(TickerMapper.toDto(null));
  }
}
