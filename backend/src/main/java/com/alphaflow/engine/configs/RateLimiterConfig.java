package com.alphaflow.engine.configs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.ratelimiter")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimiterConfig {

  @Builder.Default
  private long delayMilliseconds = 1000L;

  @Builder.Default
  private long interChunkDelayMilliseconds = 1000L;

  @Builder.Default
  private int maxRateLimitRetries = 4;

  @Builder.Default
  private long initialBackoffMs = 5000L;

  @Builder.Default
  private double backoffMultiplier = 2.0;

  @Builder.Default
  private long jitterRangeMs = 1000L;
}
