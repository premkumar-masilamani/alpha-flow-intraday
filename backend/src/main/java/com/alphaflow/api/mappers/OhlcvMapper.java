package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.OhlcvDto;
import com.alphaflow.persistence.entities.IntradayPrice;

public class OhlcvMapper {

  public static OhlcvDto toDto(IntradayPrice intradayPrice) {
    if (intradayPrice == null) {
      return null;
    }
    return OhlcvDto.builder()
        .priceDate(intradayPrice.getPriceTime())
        .priceOpen(intradayPrice.getPriceOpen())
        .priceHigh(intradayPrice.getPriceHigh())
        .priceLow(intradayPrice.getPriceLow())
        .priceClose(intradayPrice.getPriceClose())
        .volume(intradayPrice.getVolume())
        .build();
  }
}
