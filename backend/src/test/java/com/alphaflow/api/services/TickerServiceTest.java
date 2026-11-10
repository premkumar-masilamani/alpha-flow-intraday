package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.common.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TickerServiceTest {

  private TickerRepository repo;
  private Ticker niftyTicker;
  private Ticker bankNiftyTicker;
  private TickerService service;

  @BeforeEach
  void setUp() {
    repo = mock(TickerRepository.class);
    niftyTicker =
        Ticker.builder()
            .tickerId(1L)
            .tickerSymbol("NIFTY50")
            .tickerName("NIFTY 50")
            .token("99926000")
            .scripSymbol("Nifty 50")
            .isActive(true)
            .build();
    bankNiftyTicker =
        Ticker.builder()
            .tickerId(2L)
            .tickerSymbol("BANKNIFTY")
            .tickerName("BANK NIFTY")
            .token("99926009")
            .scripSymbol("Nifty Bank")
            .isActive(true)
            .build();

    when(repo.findByIsActiveTrue()).thenReturn(List.of(niftyTicker, bankNiftyTicker));
    service = new TickerService(repo);
  }

  @Test
  void testGetTickerSuccessFromCache() {
    Ticker result = service.getTicker("NIFTY50");
    assertNotNull(result);
    assertEquals("NIFTY50", result.getTickerSymbol());
    assertEquals("NIFTY 50", result.getTickerName());

    // Case insensitive lookup
    Ticker lowercaseResult = service.getTicker("nifty50");
    assertEquals("NIFTY50", lowercaseResult.getTickerSymbol());
  }

  @Test
  void testGetTickerNotFound() {
    assertThrows(ResourceNotFoundException.class, () -> service.getTicker("UNKNOWN"));
    assertThrows(ResourceNotFoundException.class, () -> service.getTicker(null));
    assertThrows(ResourceNotFoundException.class, () -> service.getTicker("   "));
  }

  @Test
  void testGetTickerBySymbol() {
    TickerDto dto = service.getTickerBySymbol("BANKNIFTY");
    assertNotNull(dto);
    assertEquals("BANKNIFTY", dto.symbol());
    assertEquals("BANK NIFTY", dto.name());
  }

  @Test
  void testGetAllTickers() {
    List<TickerDto> all = service.getAllTickers();
    assertEquals(2, all.size());
    assertEquals("NIFTY50", all.getFirst().symbol());
    assertEquals("BANKNIFTY", all.getLast().symbol());
  }

  @Test
  void testGetCachedActiveTickers() {
    List<Ticker> active = service.getCachedActiveTickers();
    assertEquals(2, active.size());
  }

  @Test
  void testRefreshCache() {
    when(repo.findByIsActiveTrue()).thenReturn(List.of(niftyTicker));
    service.refreshCache();
    assertEquals(1, service.getAllTickers().size());
    verify(repo, times(2)).findByIsActiveTrue();
  }

  @Test
  void testNullRepositoryHandling() {
    TickerService nullRepoService = new TickerService(null);
    assertThrows(ResourceNotFoundException.class, () -> nullRepoService.getTicker("NIFTY50"));
    assertTrue(nullRepoService.getAllTickers().isEmpty());
  }
}
