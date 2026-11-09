package com.alphaflow.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum IntradayTimeframe {
  _15M("15M");

  private final String value;

  IntradayTimeframe(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static IntradayTimeframe from(String text) {
    if (text == null || text.trim().isEmpty()) {
      return null;
    }
    String normalized = text.trim().toUpperCase();
    for (IntradayTimeframe timeframe : values()) {
      if (timeframe.name().equals(normalized) || timeframe.value.equalsIgnoreCase(normalized)) {
        return timeframe;
      }
    }
    throw new IllegalArgumentException("Unknown intraday timeframe: " + text);
  }
}
