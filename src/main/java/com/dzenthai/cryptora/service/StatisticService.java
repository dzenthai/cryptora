package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.model.dto.*;
import com.dzenthai.cryptora.model.entity.Quote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;


@Slf4j
@Service
public class StatisticService {

    private final AnalysisService analysisService;

    private final QuoteService quoteService;

    public StatisticService(AnalysisService analysisService, QuoteService quoteService) {
        this.analysisService = analysisService;
        this.quoteService = quoteService;
    }

    public Statistic calculateStatisticReport(String ticker, String interval) {
        List<Quote> quotes = quoteService.getQuotesByTicker(ticker);
        var beginTime = getBeginTime(quotes, interval);
        var endTime = getEndTime(quotes);
        var filteredQuotes = quotes.stream()
                .filter(q -> !q.getCloseTime().isBefore(beginTime) && !q.getOpenTime().isAfter(endTime))
                .toList();

        if (filteredQuotes.isEmpty()) {
            log.warn("No quotes found in interval {} for ticker {}", interval, ticker);
            throw new IllegalArgumentException("No data found for ticker: " + ticker);
        }

        return Statistic.builder()
                .analysis(analysisService.getAnalysis(ticker))
                .current(getCurrent(quotes))
                .average(getAverage(filteredQuotes))
                .max(calculateMaxValues(filteredQuotes))
                .min(calculateMinValues(filteredQuotes))
                .total(getTotal(filteredQuotes))
                .info(getInfo(filteredQuotes, beginTime, endTime))
                .build();
    }

    private Current getCurrent(List<Quote> quotes) {
        var lastQuote = quotes.getLast();
        return Current.builder()
                .openPrice(lastQuote.getOpenPrice())
                .closePrice(lastQuote.getClosePrice())
                .highPrice(lastQuote.getHighPrice())
                .lowPrice(lastQuote.getLowPrice())
                .volume(lastQuote.getVolume())
                .amount(lastQuote.getAmount())
                .openTime(lastQuote.getOpenTime())
                .closeTime(lastQuote.getCloseTime())
                .trades(lastQuote.getTrades())
                .build();
    }

    private BigDecimal calculateAverage(List<Quote> quotes, Function<Quote, BigDecimal> function) {
        return quotes.stream()
                .map(function).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(quotes.size()), 2, RoundingMode.HALF_UP);
    }

    private Average getAverage(List<Quote> quotes) {
        var totalVolume = calculateTotal(quotes, Quote::getVolume);
        return Average.builder()
                .openPrice(calculateAverage(quotes, Quote::getOpenPrice))
                .closePrice(calculateAverage(quotes, Quote::getClosePrice))
                .highPrice(calculateAverage(quotes, Quote::getHighPrice))
                .lowPrice(calculateAverage(quotes, Quote::getLowPrice))
                .tradePrice(getTradePrice(quotes, totalVolume))
                .priceRange(calculateAverage(quotes, quote ->
                        quote.getHighPrice().subtract(quote.getLowPrice())))
                .build();


    }

    private BigDecimal getTradePrice(List<Quote> quotes, BigDecimal totalVolume) {
        var weightedPriceSum = quotes.stream()
                .map(q -> {
                    var midPrice = q.getHighPrice()
                            .add(q.getLowPrice())
                            .add(q.getClosePrice())
                            .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);
                    return midPrice.multiply(q.getVolume());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("VMAP");
        return totalVolume.compareTo(BigDecimal.ZERO) > 0
                ? weightedPriceSum.divide(totalVolume, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private Max calculateMaxValues(List<Quote> quotes) {
        return Max.builder()
                .openPrice(calculateExtremum(quotes, Quote::getOpenPrice, true))
                .closePrice(calculateExtremum(quotes, Quote::getClosePrice, true))
                .highPrice(calculateExtremum(quotes, Quote::getHighPrice, true))
                .lowPrice(calculateExtremum(quotes, Quote::getLowPrice, true))
                .priceRange(calculateExtremum(quotes, quote ->
                        quote.getHighPrice().subtract(quote.getLowPrice()), true))
                .volume(calculateExtremum(quotes, Quote::getVolume, true))
                .amount(calculateExtremum(quotes, Quote::getAmount, true))
                .build();
    }

    private Min calculateMinValues(List<Quote> quotes) {
        return Min.builder()
                .openPrice(calculateExtremum(quotes, Quote::getOpenPrice, false))
                .closePrice(calculateExtremum(quotes, Quote::getClosePrice, false))
                .highPrice(calculateExtremum(quotes, Quote::getHighPrice, false))
                .lowPrice(calculateExtremum(quotes, Quote::getLowPrice, false))
                .priceRange(calculateExtremum(quotes, quote ->
                        quote.getHighPrice().subtract(quote.getLowPrice()), false))
                .volume(calculateExtremum(quotes, Quote::getVolume, false))
                .amount(calculateExtremum(quotes, Quote::getAmount, false))
                .build();
    }

    private BigDecimal calculateExtremum(List<Quote> quotes, Function<Quote, BigDecimal> extractor, boolean isMax) {
        return quotes.stream()
                .map(extractor)
                .reduce((a, b) -> isMax ? a.max(b) : a.min(b))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateTotal(List<Quote> quotes, Function<Quote, BigDecimal> function) {
        return quotes.stream()
                .map(function).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Total getTotal(List<Quote> quotes) {
        return Total.builder()
                .volume(calculateTotal(quotes, Quote::getVolume))
                .amount(calculateTotal(quotes, Quote::getAmount))
                .build();
    }

    private Info getInfo(List<Quote> quotes, Instant beginTime, Instant endTime) {
        return Info.builder()
                .quoteEntriesCount(quotes.size())
                .beginTime(beginTime)
                .endTime(endTime)
                .interval(Duration.between(beginTime, endTime))
                .build();
    }

    private Instant getEndTime(List<Quote> quotes) {
        return quotes.getLast().getCloseTime();
    }

    private Instant getBeginTime(List<Quote> quotes, String interval) {
        var endTime = getEndTime(quotes);

        long value = Long.parseLong(interval.substring(0, interval.length() - 1));
        String unit = interval.substring(interval.length() - 1).toLowerCase();

        return switch (unit) {
            case "d" -> endTime.minus(value, ChronoUnit.DAYS);
            case "h" -> endTime.minus(value, ChronoUnit.HOURS);
            case "m" -> endTime.minus(value, ChronoUnit.MINUTES);
            case "s" -> endTime.minus(value, ChronoUnit.SECONDS);
            default -> throw new IllegalArgumentException("Unknown interval unit: " + unit);
        };
    }
}