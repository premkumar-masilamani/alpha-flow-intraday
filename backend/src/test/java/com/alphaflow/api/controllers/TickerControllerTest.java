package com.alphaflow.api.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.api.dtos.TickerDto;
import com.alphaflow.api.services.TickerService;
import java.util.List;
import org.junit.jupiter.api.Test;

class TickerControllerTest {

  @Test
  void testGetAllTickers() {
    TickerService service = mock(TickerService.class);

    TickerDto dto =
        TickerDto.builder()
            .id(1L)
            .symbol("NIFTY50")
            .name("NIFTY 50")
            .token("99926000")
            .scripSymbol("NIFTY")
            .isActive(true)
            .build();

    when(service.getAllTickers()).thenReturn(List.of(dto));

    TickerController controller = new TickerController(service);
    List<TickerDto> res = controller.getAllTickers();

    assertEquals(1, res.size());
    assertEquals("NIFTY50", res.getFirst().tickerSymbol());
    assertEquals("NIFTY 50", res.getFirst().tickerName());
  }

  @Test
  void testGetTickerBySymbol() {
    TickerService service = mock(TickerService.class);

    TickerDto dto =
        TickerDto.builder()
            .id(1L)
            .symbol("NIFTY50")
            .name("NIFTY 50")
            .token("99926000")
            .scripSymbol("NIFTY")
            .isActive(true)
            .build();

    when(service.getTickerBySymbol("NIFTY50")).thenReturn(dto);

    TickerController controller = new TickerController(service);
    TickerDto res = controller.getTickerBySymbol("NIFTY50");

    assertEquals("NIFTY50", res.tickerSymbol());
    assertEquals("NIFTY 50", res.tickerName());
  }
}
