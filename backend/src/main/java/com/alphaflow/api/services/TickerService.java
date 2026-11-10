package com.alphaflow.api.services;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.common.exceptions.ResourceNotFoundException;
import com.alphaflow.common.mappers.TickerMapper;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class TickerService {

  private final TickerRepository tickerRepository;
  private final Map<String, Ticker> tickerCache = new ConcurrentHashMap<>();
  private final List<Ticker> cachedActiveTickers = new CopyOnWriteArrayList<>();

  public TickerService(TickerRepository tickerRepository) {
    this.tickerRepository = tickerRepository;
    if (tickerRepository != null) {
      refreshCache();
    }
  }

  @PostConstruct
  public void initializeCache() {
    refreshCache();
  }

  public synchronized void refreshCache() {
    if (tickerRepository == null) {
      return;
    }
    List<Ticker> activeTickers = tickerRepository.findByIsActiveTrue();
    tickerCache.clear();
    cachedActiveTickers.clear();
    for (Ticker ticker : activeTickers) {
      if (ticker.getTickerSymbol() != null) {
        tickerCache.put(ticker.getTickerSymbol().toUpperCase(Locale.ROOT), ticker);
      }
    }
    cachedActiveTickers.addAll(activeTickers);
    log.info("Initialized ticker cache with {} active tickers.", cachedActiveTickers.size());
  }

  public Ticker getTicker(String symbol) {
    if (symbol == null || symbol.isBlank()) {
      log.warn("Ticker lookup requested with blank symbol");
      throw new ResourceNotFoundException("Ticker not found: " + symbol);
    }

    Ticker cached = tickerCache.get(symbol.toUpperCase(Locale.ROOT));
    if (cached != null) {
      return cached;
    }

    log.warn("Ticker not found in cache for symbol: {}", symbol);
    throw new ResourceNotFoundException("Ticker not found: " + symbol);
  }

  public TickerDto getTickerBySymbol(String symbol) {
    log.debug("Fetching ticker DTO for symbol: {}", symbol);
    return TickerMapper.toDto(getTicker(symbol));
  }

  public List<TickerDto> getAllTickers() {
    return cachedActiveTickers.stream().map(TickerMapper::toDto).toList();
  }

  public List<Ticker> getCachedActiveTickers() {
    return Collections.unmodifiableList(cachedActiveTickers);
  }
}
