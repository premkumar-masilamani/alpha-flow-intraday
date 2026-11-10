package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.RateLimiterConfig;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  @Test
  void testAllEndpointThrottlesWithZeroDelay() {
    RateLimiterConfig rateLimiterConfig =
        RateLimiterConfig.builder()
            .delayMilliseconds(0L)
            .interChunkDelayMilliseconds(0L)
            .build();

    RateLimiter rateLimiter = new RateLimiter(rateLimiterConfig);

    assertDoesNotThrow(rateLimiter::throttleAngelOneLogin);
    assertDoesNotThrow(rateLimiter::throttleAngelOneCandles);
    assertDoesNotThrow(rateLimiter::throttleAngelOneLiveQuote);
    assertDoesNotThrow(rateLimiter::throttleInterChunk);
  }

  @Test
  void testThrottlePositiveDelay() {
    RateLimiterConfig rateLimiterConfig =
        RateLimiterConfig.builder()
            .delayMilliseconds(20L)
            .build();

    RateLimiter rateLimiter = new RateLimiter(rateLimiterConfig);

    long start = System.currentTimeMillis();
    rateLimiter.throttleAngelOneCandles();
    rateLimiter.throttleAngelOneCandles();
    long elapsed = System.currentTimeMillis() - start;

    assertTrue(elapsed >= 15, "Expected elapsed time to be at least ~15ms, but was " + elapsed);
  }

  @Test
  void testThrottleExplicitDelayElapsedBranch() {
    RateLimiterConfig rateLimiterConfig =
        RateLimiterConfig.builder()
            .delayMilliseconds(0L)
            .build();

    RateLimiter rateLimiter = new RateLimiter(rateLimiterConfig);
    AtomicLong tracker = new AtomicLong(0);

    rateLimiter.throttle(tracker, 5);
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
      // Ignored
    }
    assertDoesNotThrow(() -> rateLimiter.throttle(tracker, 5));
  }

  @Test
  void testThrottleInterrupted() {
    RateLimiterConfig rateLimiterConfig =
        RateLimiterConfig.builder()
            .delayMilliseconds(1000L)
            .build();

    RateLimiter rateLimiter = new RateLimiter(rateLimiterConfig);
    AtomicLong tracker = new AtomicLong(System.currentTimeMillis() + 500);

    Thread.currentThread().interrupt();
    assertDoesNotThrow(() -> rateLimiter.throttle(tracker, 500));
    assertTrue(Thread.interrupted(), "Expected thread interrupt flag to be preserved");
  }

  @Test
  void testCalculateBackoffWithJitter() {
    RateLimiterConfig rateLimiterConfig =
        RateLimiterConfig.builder()
            .initialBackoffMs(5000L)
            .backoffMultiplier(2.0)
            .jitterRangeMs(1000L)
            .maxRateLimitRetries(4)
            .build();

    RateLimiter rateLimiter = new RateLimiter(rateLimiterConfig);

    assertEquals(4, rateLimiter.getMaxRateLimitRetries());

    long backoffAttempt1 = rateLimiter.calculateBackoffMs(1);
    assertTrue(
        backoffAttempt1 >= 5000L && backoffAttempt1 <= 6000L,
        "Attempt 1 backoff must be between 5000 and 6000ms, got " + backoffAttempt1);

    long backoffAttempt2 = rateLimiter.calculateBackoffMs(2);
    assertTrue(
        backoffAttempt2 >= 10000L && backoffAttempt2 <= 11000L,
        "Attempt 2 backoff must be between 10000 and 11000ms, got " + backoffAttempt2);

    long backoffAttempt3 = rateLimiter.calculateBackoffMs(3);
    assertTrue(
        backoffAttempt3 >= 20000L && backoffAttempt3 <= 21000L,
        "Attempt 3 backoff must be between 20000 and 21000ms, got " + backoffAttempt3);
  }

  @Test
  void testApplyBackoffZeroDelayDoesNotBlock() {
    RateLimiterConfig rateLimiterConfig =
        RateLimiterConfig.builder()
            .initialBackoffMs(0L)
            .jitterRangeMs(1L)
            .maxRateLimitRetries(4)
            .build();

    RateLimiter rateLimiter = new RateLimiter(rateLimiterConfig);
    assertDoesNotThrow(() -> rateLimiter.applyBackoff(1, "testContext"));
  }
}
