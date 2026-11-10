package com.alphaflow.api.dtos;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record MarketHolidayDto(Long id, String country, LocalDate holidayDate, String name) {}
