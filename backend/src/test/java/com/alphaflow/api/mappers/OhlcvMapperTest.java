package com.alphaflow.api.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.Country;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class OhlcvMapperTest {

  @Test
  void testIntradayPriceMapping() {
    Ticker ticker = Ticker.builder().tickerSymbol("NIFTY50").country(Country.IN).build();
    OffsetDateTime time =
        LocalDate.of(2026, 9, 23).atTime(9, 15).atZone(MarketConfig.IST_ZONE).toOffsetDateTime();
    IntradayPrice intradayPrice =
        IntradayPrice.builder()
            .ticker(ticker)
            .priceTime(time)
            .priceOpen(new BigDecimal("25000.0000"))
            .priceHigh(new BigDecimal("25050.0000"))
            .priceLow(new BigDecimal("24990.0000"))
            .priceClose(new BigDecimal("25040.0000"))
            .volume(BigDecimal.ZERO)
            .build();

    OhlcvDto dto = OhlcvMapper.toDto(intradayPrice);

    assertNotNull(dto);
    assertEquals(time, dto.priceDate());
    assertEquals(new BigDecimal("25000.0000"), dto.priceOpen());
    assertEquals(new BigDecimal("25050.0000"), dto.priceHigh());
    assertEquals(new BigDecimal("24990.0000"), dto.priceLow());
    assertEquals(new BigDecimal("25040.0000"), dto.priceClose());
    assertEquals(BigDecimal.ZERO, dto.volume());
  }

  @Test
  void testNullIntradayPriceMapping() {
    assertNull(OhlcvMapper.toDto((IntradayPrice) null));
  }
}
