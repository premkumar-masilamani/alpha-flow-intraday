package com.alphaflow.engine.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class SchedulerUtilsTest {

  @Test
  void testFormatDuration() {
    String formatted = SchedulerUtils.formatDuration(65432);
    assertNotNull(formatted);
    assertEquals("1 m 5 s (65432 ms)", formatted);

    String zero = SchedulerUtils.formatDuration(0);
    assertEquals("0 m 0 s (0 ms)", zero);
  }

  @Test
  void testParseQuoteTimestamp() {
    OffsetDateTime fallback = OffsetDateTime.now();

    OffsetDateTime parsed =
        SchedulerUtils.parseQuoteTimestamp("2026-10-16T09:30:00+05:30", fallback);
    assertEquals(OffsetDateTime.parse("2026-10-16T09:30:00+05:30"), parsed);

    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp("16-Oct-2026 12:00:00", fallback));
    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp(null, fallback));
    assertEquals(fallback, SchedulerUtils.parseQuoteTimestamp("   ", fallback));
  }
}
