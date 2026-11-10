package com.alphaflow.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tickers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticker {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ticker_id")
  @EqualsAndHashCode.Include
  private Long tickerId;

  @Column(name = "ticker_symbol", length = 50, nullable = false, unique = true)
  private String tickerSymbol;

  @Column(name = "ticker_name", length = 255, nullable = false)
  private String tickerName;

  @Column(name = "token", length = 50, nullable = false)
  private String token;

  @Column(name = "scrip_symbol", length = 50, nullable = false)
  private String scripSymbol;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;
}
