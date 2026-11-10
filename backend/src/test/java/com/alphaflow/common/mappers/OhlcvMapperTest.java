package com.alphaflow.common.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.persistence.entities.IntradayPrice;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class OhlcvMapperTest {

  @Test
  void testToDtoSuccess() {
    OffsetDateTime now = OffsetDateTime.now();
    IntradayPrice price =
        IntradayPrice.builder()
            .priceTime(now)
            .priceOpen(new BigDecimal("25000.00"))
            .priceHigh(new BigDecimal("25100.00"))
            .priceLow(new BigDecimal("24950.00"))
            .priceClose(new BigDecimal("25050.00"))
            .volume(new BigDecimal("15000"))
            .build();

    OhlcvDto dto = OhlcvMapper.toDto(price);
    assertNotNull(dto);
    assertEquals(now, dto.priceDate());
    assertEquals(new BigDecimal("25000.00"), dto.priceOpen());
    assertEquals(new BigDecimal("25100.00"), dto.priceHigh());
    assertEquals(new BigDecimal("24950.00"), dto.priceLow());
    assertEquals(new BigDecimal("25050.00"), dto.priceClose());
    assertEquals(new BigDecimal("15000"), dto.volume());
  }

  @Test
  void testToDtoNull() {
    assertNull(OhlcvMapper.toDto(null));
  }
}
