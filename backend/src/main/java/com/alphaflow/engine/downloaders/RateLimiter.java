package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.RateLimiterConfig;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimiter {

  private final RateLimiterConfig rateLimiterConfig;

  private final AtomicLong angelOneLoginNextAllowedTimeMs = new AtomicLong(0);
  private final AtomicLong angelOneCandlesNextAllowedTimeMs = new AtomicLong(0);
  private final AtomicLong angelOneLiveQuoteNextAllowedTimeMs = new AtomicLong(0);

  public void throttleAngelOneLogin() {
    throttle(angelOneLoginNextAllowedTimeMs, rateLimiterConfig.getDelayMilliseconds());
  }

  public void throttleAngelOneCandles() {
    throttle(angelOneCandlesNextAllowedTimeMs, rateLimiterConfig.getDelayMilliseconds());
  }

  public void throttleAngelOneLiveQuote() {
    throttle(angelOneLiveQuoteNextAllowedTimeMs, rateLimiterConfig.getDelayMilliseconds());
  }

  public void throttleInterChunk() {
    throttle(angelOneCandlesNextAllowedTimeMs, rateLimiterConfig.getInterChunkDelayMilliseconds());
  }

  public void throttle(AtomicLong nextAllowedTimeMs, long minDelayMs) {
    if (minDelayMs > 0) {
      long now = System.currentTimeMillis();
      long scheduledTime =
          nextAllowedTimeMs.updateAndGet(current -> Math.max(current, now) + minDelayMs);
      long waitMs = scheduledTime - minDelayMs - now;
      if (waitMs <= 0) {
        log.debug("No rate limit throttle required");
      } else {
        sleep(waitMs);
      }
    }
  }

  public long calculateBackoffMs(int attempt) {
    double factor = Math.pow(rateLimiterConfig.getBackoffMultiplier(), Math.max(0, attempt - 1));
    long baseBackoff = (long) (rateLimiterConfig.getInitialBackoffMs() * factor);
    long jitterRange = Math.max(1, rateLimiterConfig.getJitterRangeMs());
    long jitter = ThreadLocalRandom.current().nextLong(0, jitterRange);
    return baseBackoff + jitter;
  }

  public void applyBackoff(int attempt, String context) {
    long backoffMs = calculateBackoffMs(attempt);
    log.warn(
        "Angel One rate limit backoff for {} (attempt {}/{}): backing off for {} ms...",
        context,
        attempt,
        getMaxRateLimitRetries() + 1,
        backoffMs);
    sleep(backoffMs);
  }

  public int getMaxRateLimitRetries() {
    return rateLimiterConfig.getMaxRateLimitRetries();
  }

  protected void sleep(long waitMs) {
    if (waitMs > 0) {
      try {
        Thread.sleep(waitMs);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        log.warn("Rate limiter sleep interrupted: {}", exception.getMessage());
      }
    }
  }
}
