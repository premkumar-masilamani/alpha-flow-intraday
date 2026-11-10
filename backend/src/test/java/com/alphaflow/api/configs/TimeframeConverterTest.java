package com.alphaflow.api.configs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alphaflow.common.enums.Timeframe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeframeConverterTest {

  private TimeframeConverter converter;

  @BeforeEach
  void setUp() {
    converter = new TimeframeConverter();
  }

  @Test
  void testConvert() {
    assertEquals(Timeframe._15M, converter.convert("15m"));
    assertEquals(Timeframe._15M, converter.convert("15M"));
    assertEquals(Timeframe._15M, converter.convert("_15M"));
  }
}
