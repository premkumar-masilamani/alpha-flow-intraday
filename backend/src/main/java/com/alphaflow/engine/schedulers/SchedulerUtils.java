package com.alphaflow.engine.schedulers;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SchedulerUtils {

  public static String formatDuration(long durationMilliseconds) {
    Duration duration = Duration.ofMillis(durationMilliseconds);
    return String.format(
        "%d m %d s (%d ms)", duration.toMinutes(), duration.toSecondsPart(), durationMilliseconds);
  }

  public static OffsetDateTime parseQuoteTimestamp(
      String rawTimestamp, OffsetDateTime fallbackTimestamp) {
    if (rawTimestamp != null && !rawTimestamp.isBlank()) {
      try {
        return OffsetDateTime.parse(rawTimestamp);
      } catch (DateTimeParseException exception) {
        log.debug(
            "Falling back to pipeline timestamp {} for quote timestamp '{}'",
            fallbackTimestamp,
            rawTimestamp);
        return fallbackTimestamp;
      }
    } else {
      return fallbackTimestamp;
    }
  }
}
