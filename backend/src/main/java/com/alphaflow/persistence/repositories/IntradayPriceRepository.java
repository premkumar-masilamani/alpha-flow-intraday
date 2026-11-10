package com.alphaflow.persistence.repositories;

import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IntradayPriceRepository extends JpaRepository<IntradayPrice, Long> {

  @Query(
      """
        SELECT tk, MAX(ip.priceTime)
        FROM Ticker tk
        LEFT JOIN IntradayPrice ip ON ip.ticker = tk AND ip.timeframe = :timeframe
        WHERE tk.isActive = true
        GROUP BY tk
      """)
  List<Object[]> findLatestPriceTimesForActiveTickersQuery(@Param("timeframe") String timeframe);

  default Map<Ticker, OffsetDateTime> findLatestPriceTimesForActiveTickers(String timeframe) {
    Map<Ticker, OffsetDateTime> result = new LinkedHashMap<>();
    for (Object[] row : findLatestPriceTimesForActiveTickersQuery(timeframe)) {
      result.put((Ticker) row[0], (OffsetDateTime) row[1]);
    }
    return result;
  }

  @Query(
      """
        SELECT ip FROM IntradayPrice ip
        JOIN FETCH ip.ticker tk
        WHERE ip.ticker = :ticker
          AND ip.timeframe = :timeframe
          AND ip.priceTime <= :endDateTime
        ORDER BY ip.priceTime DESC
      """)
  List<IntradayPrice> findLatestByTickerAndTimeframeAndEndDate(
      @Param("ticker") Ticker ticker,
      @Param("timeframe") String timeframe,
      @Param("endDateTime") OffsetDateTime endDateTime,
      Pageable pageable);

  List<IntradayPrice> findByTickerAndTimeframeAndPriceTimeBetweenOrderByPriceTimeAsc(
      Ticker ticker, String timeframe, OffsetDateTime start, OffsetDateTime end);
}
