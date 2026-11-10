package com.alphaflow.engine.configs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimiterConfig {

  @Builder.Default
  private long delayMilliseconds = 2500L;

  @Builder.Default
  private long interChunkDelayMilliseconds = 0L;

  @Builder.Default
  private int maxRateLimitRetries = 4;

  @Builder.Default
  private long initialBackoffMs = 10000L;

  @Builder.Default
  private double backoffMultiplier = 2.0;

  @Builder.Default
  private long jitterRangeMs = 2000L;
}
