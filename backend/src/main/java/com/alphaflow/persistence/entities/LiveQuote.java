package com.alphaflow.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "live_quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LiveQuote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "live_quote_id")
  @EqualsAndHashCode.Include
  private Long liveQuoteId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false, unique = true)
  private Ticker ticker;

  @Column(name = "ltp", nullable = false, precision = 18, scale = 4)
  private BigDecimal ltp;

  @Column(name = "high", nullable = false, precision = 18, scale = 4)
  private BigDecimal high;

  @Column(name = "low", nullable = false, precision = 18, scale = 4)
  private BigDecimal low;

  @Column(name = "close", nullable = false, precision = 18, scale = 4)
  private BigDecimal close;

  @Column(name = "open", nullable = false, precision = 18, scale = 4)
  private BigDecimal open;

  @Column(name = "quote_time", nullable = false)
  private OffsetDateTime quoteTime;

  @Column(name = "change_percent", nullable = false, precision = 18, scale = 4)
  private BigDecimal changePercent;

  public BigDecimal getLastPrice() {
    return ltp;
  }

  public void setLastPrice(BigDecimal lastPrice) {
    this.ltp = lastPrice;
  }

  public BigDecimal getVolume() {
    return BigDecimal.ZERO;
  }

  public static class LiveQuoteBuilder {
    public LiveQuoteBuilder lastPrice(BigDecimal lastPrice) {
      this.ltp = lastPrice;
      return this;
    }

    public LiveQuoteBuilder volume(BigDecimal volume) {
      return this;
    }

    public LiveQuote build() {
      BigDecimal effectiveLtp = this.ltp != null ? this.ltp : BigDecimal.ZERO;
      BigDecimal effectiveOpen = this.open != null ? this.open : effectiveLtp;
      BigDecimal effectiveHigh = this.high != null ? this.high : effectiveLtp;
      BigDecimal effectiveLow = this.low != null ? this.low : effectiveLtp;
      BigDecimal effectiveClose = this.close != null ? this.close : effectiveLtp;
      BigDecimal effectiveChangePercent =
          this.changePercent != null ? this.changePercent : BigDecimal.ZERO;
      OffsetDateTime effectiveQuoteTime =
          this.quoteTime != null ? this.quoteTime : OffsetDateTime.now();

      return new LiveQuote(
          this.liveQuoteId,
          this.ticker,
          effectiveLtp,
          effectiveHigh,
          effectiveLow,
          effectiveClose,
          effectiveOpen,
          effectiveQuoteTime,
          effectiveChangePercent);
    }
  }
}
