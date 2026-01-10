package com.dzenthai.cryptora.service;

import com.binance.connector.client.spot.rest.model.KlinesItem;
import com.binance.connector.client.spot.rest.model.KlinesResponse;
import com.dzenthai.cryptora.model.entity.Quote;
import com.dzenthai.cryptora.repository.QuoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;


@Slf4j
@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;

    public QuoteService(QuoteRepository quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    public List<Quote> getAllQuotes() {
        log.debug("QuoteService | Receiving all quotes");
        return quoteRepository.findAll();
    }

    public void addAllQuotes(String ticker, KlinesResponse klinesResponse) {
        if (klinesResponse == null || klinesResponse.isEmpty()) return;

        String fullTicker = ticker.endsWith("USDT") ? ticker : ticker + "USDT";
        int count = 0;

        for (KlinesItem item : klinesResponse) {
            long closeTime = Long.parseLong(item.get(6));
            Instant closeInstant = Instant.ofEpochMilli(closeTime);

            if (!quoteRepository.existsByTickerAndCloseTime(fullTicker, closeInstant)) {
                Quote quote = buildQuote(fullTicker, item);
                quoteRepository.save(quote);
                count++;
            }
        }
        if (count > 0) {
            log.info("QuoteService | Saved {} new bars for {}", count, fullTicker);
        }
    }

    public List<Quote> getQuotesByTicker(String ticker) {
        String searchTicker = ticker.endsWith("USDT") ? ticker : ticker + "USDT";
        log.debug("QuoteService | Receiving quotes for: {}", searchTicker);
        return quoteRepository.findAll().stream()
                .filter(quote ->
                        quote.getTicker().equalsIgnoreCase(searchTicker))
                .toList();
    }

    private Quote buildQuote(String ticker, KlinesItem kline) {
        long openTime = Long.parseLong(kline.get(0));
        long closeTime = Long.parseLong(kline.get(6));

        Instant beginTime = Instant.ofEpochMilli(openTime);
        Instant endTime = Instant.ofEpochMilli(closeTime);

        return Quote.builder()
                .ticker(ticker)
                .openTime(beginTime)
                .closeTime(endTime)
                .openPrice(new BigDecimal(kline.get(1)))
                .highPrice(new BigDecimal(kline.get(2)))
                .lowPrice(new BigDecimal(kline.get(3)))
                .closePrice(new BigDecimal(kline.get(4)))
                .volume(new BigDecimal(kline.get(5)))
                .amount(new BigDecimal(kline.get(7)))
                .trades(Long.parseLong(kline.get(8)))
                .timePeriod(Duration.between(beginTime, endTime))
                .build();
    }
}
