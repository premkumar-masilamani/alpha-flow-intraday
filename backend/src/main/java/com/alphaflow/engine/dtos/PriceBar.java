package com.alphaflow.engine.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record PriceBar(
    LocalDate date,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume) {}
