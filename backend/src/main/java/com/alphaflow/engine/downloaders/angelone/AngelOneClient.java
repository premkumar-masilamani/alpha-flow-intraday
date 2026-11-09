package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.common.enums.IntradayTimeframe;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.configs.MarketConfig;
import com.alphaflow.engine.downloaders.RateLimiter;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneCandle;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AngelOneClient {

  private static final ZoneId IST_ZONE = MarketConfig.IST_ZONE;
  private static final DateTimeFormatter CANDLE_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

  private final AngelOneConfig config;
  private final AngelOneAuthManager authManager;
  private final ObjectMapper objectMapper;
  private final AngelOneNetworkHelper networkHelper;
  private final RateLimiter rateLimiter;
  private final RestClient restClient;

  public AngelOneClient(
      AngelOneConfig config,
      AngelOneAuthManager authManager,
      ObjectMapper objectMapper,
      AngelOneNetworkHelper networkHelper,
      RateLimiter rateLimiter,
      @Autowired(required = false) RestClient.Builder restClientBuilder) {
    this.config = config;
    this.authManager = authManager;
    this.objectMapper = objectMapper;
    this.networkHelper = networkHelper;
    this.rateLimiter = rateLimiter;
    this.restClient =
        (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build();
  }

  public static String resolveToken(String symbol) {
    if ("NIFTY50".equalsIgnoreCase(symbol)) {
      return "99926000";
    }
    return symbol;
  }

  public static String mapToAngelOneInterval(IntradayTimeframe timeframe) {
    if (timeframe == IntradayTimeframe._15M) {
      return "FIFTEEN_MINUTE";
    }
    return timeframe != null ? timeframe.getValue() : "";
  }

  public static final int ANGEL_ONE_15M_MAX_CHUNK_DAYS = 200;
  public static final int MAX_RATE_LIMIT_RETRIES = 3;
  public static final long INITIAL_BACKOFF_MS = 2000L;
  public static final double BACKOFF_MULTIPLIER = 2.0;

  private static final DateTimeFormatter CHUNK_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public List<AngelOneCandle> fetch15mCandles(
      String exchange,
      String symbolToken,
      OffsetDateTime startWindow,
      OffsetDateTime expectedCompletedTime) {
    if (exchange != null
        && symbolToken != null
        && startWindow != null
        && expectedCompletedTime != null) {
      Map<OffsetDateTime, AngelOneCandle> candleMap = new LinkedHashMap<>();
      OffsetDateTime currentWindowStart = startWindow;
      while (currentWindowStart.isBefore(expectedCompletedTime)) {
        OffsetDateTime candidateWindowEnd =
            currentWindowStart.plusDays(ANGEL_ONE_15M_MAX_CHUNK_DAYS);
        OffsetDateTime currentWindowEnd =
            candidateWindowEnd.isBefore(expectedCompletedTime)
                ? candidateWindowEnd
                : expectedCompletedTime;

        String fromDateStr = currentWindowStart.format(CHUNK_DATE_TIME_FORMATTER);
        String toDateStr = currentWindowEnd.format(CHUNK_DATE_TIME_FORMATTER);

        log.info(
            "Syncing 15m candles for token {} from {} to {}",
            symbolToken,
            fromDateStr,
            toDateStr);

        List<AngelOneCandle> chunk =
            getCandleData(exchange, symbolToken, IntradayTimeframe._15M, fromDateStr, toDateStr);
        if (chunk != null) {
          for (AngelOneCandle candle : chunk) {
            candleMap.putIfAbsent(candle.timestamp(), candle);
          }
        }

        currentWindowStart = currentWindowEnd.plusMinutes(1);
      }
      return new ArrayList<>(candleMap.values());
    } else {
      log.warn("Cannot fetch 15m candles: missing required parameters.");
      return Collections.emptyList();
    }
  }

  public List<AngelOneCandle> getCandleData(
      String exchange,
      String symbolToken,
      IntradayTimeframe timeframe,
      String fromDate,
      String toDate) {

    String angelOneInterval = mapToAngelOneInterval(timeframe);

    Map<String, String> body = new HashMap<>();
    body.put("exchange", exchange);
    body.put("symboltoken", symbolToken);
    body.put("interval", angelOneInterval);
    body.put("fromdate", fromDate);
    body.put("todate", toDate);

    String jwt = authManager.getValidJwtToken();
    if (jwt == null || jwt.isBlank()) {
      log.warn("Cannot fetch candles: no valid Angel One JWT token.");
      return Collections.emptyList();
    }

    int maxAttempts = MAX_RATE_LIMIT_RETRIES + 1;
    long currentBackoffMs = INITIAL_BACKOFF_MS;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        rateLimiter.throttleAngelOneCandles();
        ResponseEntity<String> response =
            restClient
                .post()
                .uri(config.getCandleUrl())
                .headers(
                    headers ->
                        networkHelper.applyAuthenticatedHeaders(headers, config.getApiKey(), jwt))
                .body(body)
                .retrieve()
                .toEntity(String.class);

        String responseBody = response.getBody();
        if (responseBody != null) {
          JsonNode root = objectMapper.readTree(responseBody);
          if (root.path("status").asBoolean(false)) {
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
              List<AngelOneCandle> candles = new ArrayList<>();
              for (JsonNode row : dataNode) {
                if (row.isArray() && row.size() >= 6) {
                  OffsetDateTime ts = parseTimestamp(row.get(0).asText());
                  BigDecimal open = new BigDecimal(row.get(1).asText());
                  BigDecimal high = new BigDecimal(row.get(2).asText());
                  BigDecimal low = new BigDecimal(row.get(3).asText());
                  BigDecimal close = new BigDecimal(row.get(4).asText());
                  BigDecimal volume = new BigDecimal(row.get(5).asText());
                  candles.add(new AngelOneCandle(ts, open, high, low, close, volume));
                }
              }
              return candles;
            }
          } else {
            String message = root.path("message").asText();
            String errorCode = root.path("errorcode").asText();
            if (isRateLimitError(message, errorCode) && attempt < maxAttempts) {
              log.warn(
                  "Angel One rate limit encountered in response (attempt {}/{}): {} (code: {}). Backing off for {} ms...",
                  attempt,
                  maxAttempts,
                  message,
                  errorCode,
                  currentBackoffMs);
              sleepBackoff(currentBackoffMs);
              currentBackoffMs = (long) (currentBackoffMs * BACKOFF_MULTIPLIER);
              continue;
            }
            log.warn(
                "Angel One getCandleData returned status false: {} (code: {})", message, errorCode);
            if ("AG8001".equalsIgnoreCase(errorCode) || message.toLowerCase().contains("token")) {
              authManager.invalidateSession();
            }
          }
        } else {
          log.warn("Empty response body from getCandleData");
        }
        return Collections.emptyList();
      } catch (HttpStatusCodeException exception) {
        if (isRateLimitException(exception) && attempt < maxAttempts) {
          log.warn(
              "Angel One rate limit exceeded (HTTP {}): {} (attempt {}/{}). Backing off for {} ms...",
              exception.getStatusCode().value(),
              exception.getMessage(),
              attempt,
              maxAttempts,
              currentBackoffMs);
          sleepBackoff(currentBackoffMs);
          currentBackoffMs = (long) (currentBackoffMs * BACKOFF_MULTIPLIER);
        } else {
          log.warn("Failed to fetch candle data from Angel One: {}", exception.getMessage());
          return Collections.emptyList();
        }
      } catch (RestClientException | JsonProcessingException exception) {
        log.warn("Failed to fetch candle data from Angel One: {}", exception.getMessage());
        return Collections.emptyList();
      }
    }
    return Collections.emptyList();
  }

  public Map<String, AngelOneQuote> getMarketQuotes(String exchange, List<String> symbolTokens) {
    if (symbolTokens != null && !symbolTokens.isEmpty()) {
      Map<String, Object> body = new HashMap<>();
      body.put("mode", "FULL");
      body.put("exchangeTokens", Map.of(exchange, symbolTokens));

      String jwt = authManager.getValidJwtToken();
      if (jwt == null || jwt.isBlank()) {
        log.warn("Cannot fetch market quotes: no valid Angel One JWT token.");
        return Collections.emptyMap();
      }

      int maxAttempts = MAX_RATE_LIMIT_RETRIES + 1;
      long currentBackoffMs = INITIAL_BACKOFF_MS;

      for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
          rateLimiter.throttleAngelOneLiveQuote();
          ResponseEntity<String> response =
              restClient
                  .post()
                  .uri(config.getQuoteUrl())
                  .headers(
                      headers ->
                          networkHelper.applyAuthenticatedHeaders(headers, config.getApiKey(), jwt))
                  .body(body)
                  .retrieve()
                  .toEntity(String.class);

          String responseBody = response.getBody();
          if (responseBody != null) {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.path("status").asBoolean(false)) {
              JsonNode fetchedList = root.path("data").path("fetched");
              if (fetchedList.isArray()) {
                Map<String, AngelOneQuote> quotes = new HashMap<>();
                for (JsonNode item : fetchedList) {
                  String token = item.path("symbolToken").asText();
                  if (!token.isBlank()) {
                    BigDecimal ltp =
                        item.hasNonNull("ltp")
                            ? new BigDecimal(item.path("ltp").asText())
                            : BigDecimal.ZERO;
                    BigDecimal open =
                        item.hasNonNull("open")
                            ? new BigDecimal(item.path("open").asText())
                            : ltp;
                    BigDecimal high =
                        item.hasNonNull("high")
                            ? new BigDecimal(item.path("high").asText())
                            : ltp;
                    BigDecimal low =
                        item.hasNonNull("low")
                            ? new BigDecimal(item.path("low").asText())
                            : ltp;
                    BigDecimal close =
                        item.hasNonNull("close")
                            ? new BigDecimal(item.path("close").asText())
                            : ltp;
                    BigDecimal changePercent =
                        item.hasNonNull("changePercent")
                            ? new BigDecimal(item.path("changePercent").asText())
                            : (item.hasNonNull("pChange")
                                ? new BigDecimal(item.path("pChange").asText())
                                : BigDecimal.ZERO);
                    String timestamp =
                        item.hasNonNull("exchFeedTime")
                            ? item.path("exchFeedTime").asText()
                            : OffsetDateTime.now(IST_ZONE).toString();
                    quotes.put(
                        token,
                        new AngelOneQuote(ltp, open, high, low, close, changePercent, timestamp));
                  }
                }
                return quotes;
              }
            } else {
              String message = root.path("message").asText();
              String errorCode = root.path("errorcode").asText();
              if (isRateLimitError(message, errorCode) && attempt < maxAttempts) {
                log.warn(
                    "Angel One quote rate limit encountered in response (attempt {}/{}): {}. Backing off for {} ms...",
                    attempt,
                    maxAttempts,
                    message,
                    currentBackoffMs);
                sleepBackoff(currentBackoffMs);
                currentBackoffMs = (long) (currentBackoffMs * BACKOFF_MULTIPLIER);
                continue;
              }
              if ("AG8001".equalsIgnoreCase(errorCode)) {
                authManager.invalidateSession();
              }
            }
          }
          return Collections.emptyMap();
        } catch (HttpStatusCodeException exception) {
          if (isRateLimitException(exception) && attempt < maxAttempts) {
            log.warn(
                "Angel One quote rate limit exceeded (HTTP {}): {} (attempt {}/{}). Backing off for {} ms...",
                exception.getStatusCode().value(),
                exception.getMessage(),
                attempt,
                maxAttempts,
                currentBackoffMs);
            sleepBackoff(currentBackoffMs);
            currentBackoffMs = (long) (currentBackoffMs * BACKOFF_MULTIPLIER);
          } else {
            log.warn("Failed to fetch quotes from Angel One: {}", exception.getMessage());
            return Collections.emptyMap();
          }
        } catch (RestClientException | JsonProcessingException exception) {
          log.warn("Failed to fetch quotes from Angel One: {}", exception.getMessage());
          return Collections.emptyMap();
        }
      }
      return Collections.emptyMap();
    } else {
      return Collections.emptyMap();
    }
  }

  boolean isRateLimitException(HttpStatusCodeException exception) {
    int statusCode = exception.getStatusCode().value();
    if (statusCode == 429) {
      return true;
    }
    if (statusCode == 403) {
      String responseBody = exception.getResponseBodyAsString();
      String message = exception.getMessage();
      return (responseBody != null && responseBody.toLowerCase().contains("exceeding access rate"))
          || (message != null && message.toLowerCase().contains("exceeding access rate"));
    }
    return false;
  }

  boolean isRateLimitError(String message, String errorCode) {
    if (message != null) {
      String lower = message.toLowerCase();
      if (lower.contains("exceeding access rate")
          || lower.contains("too many requests")
          || lower.contains("rate limit")) {
        return true;
      }
    }
    return "AG8003".equalsIgnoreCase(errorCode);
  }

  void sleepBackoff(long durationMs) {
    long effectiveSleep = (config.getDelayMilliseconds() == 0) ? 0 : durationMs;
    if (effectiveSleep > 0) {
      try {
        Thread.sleep(effectiveSleep);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        log.warn("Rate limit backoff sleep interrupted: {}", exception.getMessage());
      }
    }
  }

  OffsetDateTime parseTimestamp(String raw) {
    try {
      return OffsetDateTime.parse(raw, CANDLE_TIMESTAMP_FORMATTER);
    } catch (Exception e) {
      try {
        DateTimeFormatter noOffset = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        LocalDateTime ldt = LocalDateTime.parse(raw, noOffset);
        return ldt.atZone(IST_ZONE).toOffsetDateTime();
      } catch (Exception ex) {
        return OffsetDateTime.now(IST_ZONE);
      }
    }
  }
}
