package com.alphaflow.common.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.alphaflow.api.dtos.MarketHolidayDto;
import com.alphaflow.persistence.entities.MarketHoliday;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MarketHolidayMapperTest {

  @Test
  void testToDtoSuccess() {
    MarketHoliday holiday =
        MarketHoliday.builder()
            .marketHolidayId(1L)
            .country("IN")
            .holidayDate(LocalDate.of(2026, 1, 26))
            .name("Republic Day")
            .build();

    MarketHolidayDto dto = MarketHolidayMapper.toDto(holiday);
    assertNotNull(dto);
    assertEquals(1L, dto.id());
    assertEquals("IN", dto.country());
    assertEquals(LocalDate.of(2026, 1, 26), dto.holidayDate());
    assertEquals("Republic Day", dto.name());
  }

  @Test
  void testToDtoNull() {
    assertNull(MarketHolidayMapper.toDto(null));
  }
}
