package com.alphaflow.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "intraday_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IntradayPrice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "intraday_price_id")
  private Long intradayPriceId;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @EqualsAndHashCode.Include
  @Column(name = "timeframe", nullable = false, length = 20)
  private String timeframe;

  @EqualsAndHashCode.Include
  @Column(name = "price_time", nullable = false)
  private OffsetDateTime priceTime;

  @Column(name = "price_open", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceOpen;

  @Column(name = "price_high", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceHigh;

  @Column(name = "price_low", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceLow;

  @Column(name = "price_close", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceClose;

  @Column(name = "volume", nullable = false, precision = 18, scale = 4)
  private BigDecimal volume;
}
