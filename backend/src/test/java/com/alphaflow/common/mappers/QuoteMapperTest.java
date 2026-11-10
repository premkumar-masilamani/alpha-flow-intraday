package com.alphaflow.common.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.persistence.entities.LiveQuote;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class QuoteMapperTest {

  @Test
  void testToDtoSuccess() {
    OffsetDateTime now = OffsetDateTime.now();
    LiveQuote quote =
        LiveQuote.builder()
            .lastPrice(new BigDecimal("25000.50"))
            .open(new BigDecimal("24900.00"))
            .high(new BigDecimal("25100.00"))
            .low(new BigDecimal("24850.00"))
            .close(new BigDecimal("24950.00"))
            .changePercent(new BigDecimal("0.55"))
            .quoteTime(now)
            .build();

    QuoteDto dto = QuoteMapper.toDto(quote);
    assertNotNull(dto);
    assertEquals(new BigDecimal("25000.50"), dto.currentPrice());
    assertEquals(new BigDecimal("24900.00"), dto.open());
    assertEquals(new BigDecimal("25100.00"), dto.high());
    assertEquals(new BigDecimal("24850.00"), dto.low());
    assertEquals(new BigDecimal("24950.00"), dto.close());
    assertEquals(new BigDecimal("0.55"), dto.changePercent());
    assertEquals(now.toString(), dto.timestamp());
  }

  @Test
  void testToDtoNull() {
    QuoteDto dto = QuoteMapper.toDto(null);
    assertNotNull(dto);
    assertEquals(BigDecimal.ZERO, dto.currentPrice());
    assertEquals("", dto.timestamp());
  }

  @Test
  void testDefaultZeroDto() {
    QuoteDto dto = QuoteMapper.defaultZeroDto();
    assertNotNull(dto);
    assertEquals(BigDecimal.ZERO, dto.currentPrice());
    assertEquals(BigDecimal.ZERO, dto.open());
    assertEquals(BigDecimal.ZERO, dto.high());
    assertEquals(BigDecimal.ZERO, dto.low());
    assertEquals(BigDecimal.ZERO, dto.close());
    assertEquals(BigDecimal.ZERO, dto.changePercent());
    assertEquals("", dto.timestamp());
  }
}
