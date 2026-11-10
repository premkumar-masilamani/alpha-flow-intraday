package com.alphaflow.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.common.exceptions.ResourceNotFoundException;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.TickerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TickerServiceTest {

  @Test
  void testGetTickerSuccess() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("NIFTY50");
    ticker.setTickerName("NIFTY 50");
    ticker.setToken("99926000");
    ticker.setScripSymbol("NIFTY");
    ticker.setIsActive(true);

    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("NIFTY50")).thenReturn(Optional.of(ticker));

    TickerService service = new TickerService(repo);
    Ticker result = service.getTicker("NIFTY50");

    assertEquals("NIFTY50", result.getTickerSymbol());
    assertEquals("NIFTY 50", result.getTickerName());
    assertEquals("99926000", result.getToken());
    assertEquals("NIFTY", result.getScripSymbol());
  }

  @Test
  void testGetTickerNotFound() {
    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("MSFT")).thenReturn(Optional.empty());

    TickerService service = new TickerService(repo);

    assertThrows(ResourceNotFoundException.class, () -> service.getTicker("MSFT"));
  }

  @Test
  void testGetTickerBySymbolSuccess() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("NIFTY50");
    ticker.setTickerName("NIFTY 50");
    ticker.setToken("99926000");
    ticker.setScripSymbol("NIFTY");
    ticker.setIsActive(true);

    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("NIFTY50")).thenReturn(Optional.of(ticker));

    TickerService service = new TickerService(repo);
    TickerDto dto = service.getTickerBySymbol("NIFTY50");

    assertEquals("NIFTY50", dto.tickerSymbol());
    assertEquals("NIFTY 50", dto.tickerName());
    assertEquals("99926000", dto.token());
  }

  @Test
  void testGetTickerBySymbolNotFound() {
    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByTickerSymbolIgnoreCase("MSFT")).thenReturn(Optional.empty());

    TickerService service = new TickerService(repo);

    assertThrows(ResourceNotFoundException.class, () -> service.getTickerBySymbol("MSFT"));
  }

  @Test
  void testGetAllTickers() {
    Ticker ticker = new Ticker();
    ticker.setTickerId(1L);
    ticker.setTickerSymbol("NIFTY50");
    ticker.setTickerName("NIFTY 50");
    ticker.setToken("99926000");
    ticker.setScripSymbol("NIFTY");
    ticker.setIsActive(true);

    TickerRepository repo = mock(TickerRepository.class);
    when(repo.findByIsActiveTrue()).thenReturn(List.of(ticker));

    TickerService service = new TickerService(repo);
    List<TickerDto> list = service.getAllTickers();

    assertEquals(1, list.size());
    assertEquals("NIFTY50", list.getFirst().tickerSymbol());
  }
}
