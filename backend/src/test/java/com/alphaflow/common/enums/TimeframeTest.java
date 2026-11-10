package com.alphaflow.common.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TimeframeTest {

  @Test
  void testFromValid() {
    assertEquals(Timeframe._15M, Timeframe.from("15m"));
    assertEquals(Timeframe._15M, Timeframe.from("15M"));
    assertEquals(Timeframe._15M, Timeframe.from("_15M"));
  }

  @Test
  void testFromNullAndBlank() {
    assertNull(Timeframe.from(null));
    assertNull(Timeframe.from(""));
    assertNull(Timeframe.from("   "));
  }

  @Test
  void testFromInvalid() {
    assertThrows(IllegalArgumentException.class, () -> Timeframe.from("DAILY"));
    assertThrows(IllegalArgumentException.class, () -> Timeframe.from("5M"));
  }

  @Test
  void testGetValue() {
    assertEquals("15M", Timeframe._15M.getValue());
  }
}
