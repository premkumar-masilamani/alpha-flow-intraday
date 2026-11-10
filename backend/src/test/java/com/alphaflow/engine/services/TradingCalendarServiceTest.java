package com.alphaflow.engine.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.persistence.entities.MarketHoliday;
import com.alphaflow.persistence.repositories.MarketHolidayRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradingCalendarServiceTest {

  private final ZoneId istZone = ZoneId.of("Asia/Kolkata");
  private MarketHolidayRepository marketHolidayRepository;

  @BeforeEach
  void setUp() {
    marketHolidayRepository = mock(MarketHolidayRepository.class);

    List<MarketHoliday> holidays =
        List.of(
            MarketHoliday.builder()
                .country("IN")
                .holidayDate(LocalDate.of(2026, 1, 26))
                .name("Republic Day")
                .build(),
            MarketHoliday.builder()
                .country("IN")
                .holidayDate(LocalDate.of(2026, 8, 15))
                .name("Independence Day")
                .build(),
            MarketHoliday.builder()
                .country("IN")
                .holidayDate(LocalDate.of(2026, 10, 2))
                .name("Mahatma Gandhi Jayanti")
                .build());

    when(marketHolidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(holidays);

    TradingCalendarService calendar = new TradingCalendarService(marketHolidayRepository);
    calendar.initializeHolidays();
  }

  @Test
  void testNullMarketConfig() {
    TradingCalendarService calendar = new TradingCalendarService(null);
    calendar.initializeHolidays();
    assertTrue(TradingCalendarService.getHolidays().isEmpty());
    setUp();
  }

  @Test
  void testHolidaysConfiguration() {
    assertEquals(
        Set.of(
            LocalDate.of(2026, 1, 26),
            LocalDate.of(2026, 8, 15),
            LocalDate.of(2026, 10, 2)),
        TradingCalendarService.getHolidays());
  }

  @Test
  void testGetHolidayName() {
    assertEquals(
        Optional.of("Republic Day"),
        TradingCalendarService.getHolidayName(LocalDate.of(2026, 1, 26)));
    assertEquals(
        Optional.of("Independence Day"),
        TradingCalendarService.getHolidayName(LocalDate.of(2026, 8, 15)));
    assertEquals(
        Optional.of("Mahatma Gandhi Jayanti"),
        TradingCalendarService.getHolidayName(LocalDate.of(2026, 10, 2)));
    assertEquals(
        Optional.empty(),
        TradingCalendarService.getHolidayName(LocalDate.of(2026, 1, 27)));
  }

  @Test
  void testSetHolidays() {
    Map<LocalDate, String> custom =
        Map.of(LocalDate.of(2026, 12, 25), "Christmas");
    TradingCalendarService.setHolidays(custom);
    assertEquals(Set.of(LocalDate.of(2026, 12, 25)), TradingCalendarService.getHolidays());
    assertEquals(
        Optional.of("Christmas"),
        TradingCalendarService.getHolidayName(LocalDate.of(2026, 12, 25)));

    TradingCalendarService.setHolidays(null);
    assertTrue(TradingCalendarService.getHolidays().isEmpty());
    setUp();
  }

  @Test
  void testIsTradingDay() {
    // Normal Wednesday
    assertTrue(TradingCalendarService.isTradingDay(LocalDate.of(2026, 10, 7)));

    // Saturday & Sunday
    assertFalse(TradingCalendarService.isTradingDay(LocalDate.of(2026, 10, 10)));
    assertFalse(TradingCalendarService.isTradingDay(LocalDate.of(2026, 10, 11)));

    // Indian holiday (Republic Day)
    assertFalse(TradingCalendarService.isTradingDay(LocalDate.of(2026, 1, 26)));
  }

  @Test
  void testIsMarketSessionOpen() {
    LocalTime start = LocalTime.of(9, 15);
    LocalTime end = LocalTime.of(15, 30);

    // During open session
    ZonedDateTime openTime =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(11, 0), istZone);
    assertTrue(TradingCalendarService.isMarketSessionOpen(openTime, start, end));

    // Before session
    ZonedDateTime beforeTime =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 0), istZone);
    assertFalse(TradingCalendarService.isMarketSessionOpen(beforeTime, start, end));

    // After session
    ZonedDateTime afterTime =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(16, 0), istZone);
    assertFalse(TradingCalendarService.isMarketSessionOpen(afterTime, start, end));

    // On weekend
    ZonedDateTime weekendTime =
        ZonedDateTime.of(LocalDate.of(2026, 10, 10), LocalTime.of(11, 0), istZone);
    assertFalse(TradingCalendarService.isMarketSessionOpen(weekendTime, start, end));

    // On holiday
    ZonedDateTime holidayTime =
        ZonedDateTime.of(LocalDate.of(2026, 1, 26), LocalTime.of(11, 0), istZone);
    assertFalse(TradingCalendarService.isMarketSessionOpen(holidayTime, start, end));
  }

  @Test
  void testGetPreviousTradingDay() {
    // Wednesday -> Tuesday
    assertEquals(
        LocalDate.of(2026, 10, 6),
        TradingCalendarService.getPreviousTradingDay(LocalDate.of(2026, 10, 7)));

    // Monday -> Thursday (because Friday 2026-10-02 is Mahatma Gandhi Jayanti)
    assertEquals(
        LocalDate.of(2026, 10, 1),
        TradingCalendarService.getPreviousTradingDay(LocalDate.of(2026, 10, 5)));

    // Normal Monday -> Friday (Monday 2026-10-12 -> Friday 2026-10-09)
    assertEquals(
        LocalDate.of(2026, 10, 9),
        TradingCalendarService.getPreviousTradingDay(LocalDate.of(2026, 10, 12)));

    // Day after India holiday (Tuesday 2026-01-27 after Republic Day Monday 2026-01-26) -> Friday 2026-01-23
    assertEquals(
        LocalDate.of(2026, 1, 23),
        TradingCalendarService.getPreviousTradingDay(LocalDate.of(2026, 1, 27)));
  }

  @Test
  void testGetExpectedLatestCompletedCandleTime() {
    LocalTime sessionStart = LocalTime.of(9, 15);
    LocalTime sessionEnd = LocalTime.of(15, 30);
    int candleMinutes = 15;

    // Wednesday 09:30:00 -> 09:15 candle completed
    ZonedDateTime nowAt0930 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(9, 30), istZone);
    assertEquals(
        nowAt0930.toLocalDate().atTime(9, 15).atZone(istZone).toOffsetDateTime(),
        TradingCalendarService.getExpectedLatestCompletedCandleTime(
            nowAt0930, sessionStart, sessionEnd, candleMinutes));

    // Wednesday 15:30:00 -> 15:15 candle completed
    ZonedDateTime nowAt1530 =
        ZonedDateTime.of(LocalDate.of(2026, 10, 7), LocalTime.of(15, 30), istZone);
    assertEquals(
        nowAt1530.toLocalDate().atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        TradingCalendarService.getExpectedLatestCompletedCandleTime(
            nowAt1530, sessionStart, sessionEnd, candleMinutes));

    // Weekend (Saturday) -> previous Friday 15:15 candle
    ZonedDateTime saturday =
        ZonedDateTime.of(LocalDate.of(2026, 10, 10), LocalTime.of(12, 0), istZone);
    assertEquals(
        LocalDate.of(2026, 10, 9).atTime(15, 15).atZone(istZone).toOffsetDateTime(),
        TradingCalendarService.getExpectedLatestCompletedCandleTime(
            saturday, sessionStart, sessionEnd, candleMinutes));
  }
}
