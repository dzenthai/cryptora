package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.entity.Quote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Slf4j
@Service
public class StatisticService {

    private final QuoteService quoteService;

    public StatisticService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    public Map<String, ?> calculateStatisticReport(String ticker) {

        List<Quote> quotes = quoteService.getQuotesByTicker(ticker);

        log.debug("StatisticService | Generate statistic report for the ticker: {}", ticker);

        if (quotes.isEmpty()) {
            var warn = String.format("The ticker with the name %s does not exist or information about it is unavailable.", ticker);
            log.warn("StatisticService | {}", warn);
            throw new IllegalArgumentException(warn);
        }

        return Map.of("ticker", ticker,
                "average", Map.of(
                        "openPrice", calculateAverageOpenPrice(quotes),
                        "closePrice", calculateAverageClosePrice(quotes),
                        "highPrice", calculateAverageHighPrice(quotes),
                        "lowPrice", calculateAverageLowPrice(quotes),
                        "tradePrice", calculateAverageTradePrice(quotes),
                        "priceRange", calculateAveragePriceRange(quotes),
                        "total", Map.of(
                                "volume", calculateTotalVolume(quotes),
                                "amount", calculateTotalAmount(quotes))),
                "additionalInfo", Map.of(
                        "quoteEntriesCount", quotes.size(),
                        "currentDateTime", LocalDateTime.now(),
                        "initDateTime", quotes.stream()
                                .map(Quote::getDatetime)
                                .findFirst()
                ));
    }

    private BigDecimal calculateAverageOpenPrice(List<Quote> quotes) {
        return calculateAverage(quotes, Quote::getOpenPrice);
    }

    private BigDecimal calculateAverageClosePrice(List<Quote> quotes) {
        return calculateAverage(quotes, Quote::getClosePrice);
    }

    private BigDecimal calculateAverageHighPrice(List<Quote> quotes) {
        return calculateAverage(quotes, Quote::getHighPrice);
    }

    private BigDecimal calculateAverageLowPrice(List<Quote> quotes) {
        return calculateAverage(quotes, Quote::getLowPrice);
    }

    private BigDecimal calculateAveragePriceRange(List<Quote> quotes) {
        return calculateAverage(quotes, quote ->
                quote.getHighPrice().subtract(quote.getLowPrice()));
    }

    private BigDecimal calculateTotalVolume(List<Quote> quotes) {
        return calculateTotal(quotes, Quote::getVolume);
    }

    private BigDecimal calculateTotalAmount(List<Quote> quotes) {
        return calculateTotal(quotes, Quote::getAmount);
    }

    private BigDecimal calculateAverageTradePrice(List<Quote> quotes) {
        BigDecimal totalAmount = calculateTotalAmount(quotes);
        BigDecimal totalVolume = calculateTotalVolume(quotes);
        return quotes.isEmpty() ? BigDecimal.ZERO :
                totalAmount.divide(totalVolume, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(List<Quote> quotes, Function<Quote, BigDecimal> function) {
        return quotes.isEmpty() ? BigDecimal.ZERO : quotes.stream()
                .map(function).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(quotes.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotal(List<Quote> quotes, Function<Quote, BigDecimal> function) {
        return quotes.isEmpty() ? BigDecimal.ZERO : quotes.stream()
                .map(function)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
