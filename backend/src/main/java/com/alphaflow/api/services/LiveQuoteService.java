package com.alphaflow.api.services;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.common.mappers.QuoteMapper;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class LiveQuoteService {

  private final LiveQuoteRepository liveQuoteRepository;

  public LiveQuoteService(LiveQuoteRepository liveQuoteRepository) {
    this.liveQuoteRepository = liveQuoteRepository;
  }

  public QuoteDto getQuote(Ticker ticker) {
    Optional<LiveQuote> liveQuoteOptional = liveQuoteRepository.findByTicker(ticker);
    if (liveQuoteOptional.isPresent()) {
      return QuoteMapper.toDto(liveQuoteOptional.get());
    } else {
      log.debug("No live quote found in database for ticker: {}", ticker.getTickerSymbol());
      return QuoteMapper.defaultZeroDto();
    }
  }
}
