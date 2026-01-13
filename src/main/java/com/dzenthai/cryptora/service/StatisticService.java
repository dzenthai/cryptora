package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.model.dto.*;
import com.dzenthai.cryptora.model.entity.Candle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;


@Slf4j
@Service
public class StatisticService {

    private final AnalysisService analysisService;

    private final CandleService candleService;

    public StatisticService(AnalysisService analysisService, CandleService candleService) {
        this.analysisService = analysisService;
        this.candleService = candleService;
    }

    public Statistic calculateStatisticReport(String baseAsset, String duration) {
        log.debug("StatisticService | Calculating statistic, base asset: {}", baseAsset);
        List<Candle> candles = candleService.getCandleBySymbol(baseAsset);
        if (candles == null || candles.isEmpty()) {
            throw new NoSuchElementException("No data found, base asset: %s, duration: %s"
                    .formatted(baseAsset, duration));
        }
        try {
            log.debug("StatisticService | Filtering list of candles by duration, base asset: {}, duration: {}",
                    baseAsset, duration);
            var beginTime = getBeginTime(candles, duration);
            var endTime = getEndTime(candles);
            var filteredCandles = candles.stream()
                    .filter(q ->
                            !q.getCloseTime().isBefore(beginTime) && !q.getOpenTime().isAfter(endTime))
                    .toList();

            return Statistic.builder()
                    .analysis(analysisService.getAnalysis(baseAsset))
                    .current(getCurrent(candles))
                    .average(getAverage(filteredCandles))
                    .max(calculateMaxValues(filteredCandles))
                    .min(calculateMinValues(filteredCandles))
                    .total(getTotal(filteredCandles))
                    .info(getInfo(filteredCandles, beginTime, endTime))
                    .build();

        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid duration value: %s"
                    .formatted(duration));
        }
    }


    private BigDecimal calculateAverage(List<Candle> candles, Function<Candle, BigDecimal> function) {
        return candles.stream()
                .map(function).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(candles.size()), 2, RoundingMode.HALF_UP);
    }

    private Max calculateMaxValues(List<Candle> candles) {
        return Max.builder()
                .openPrice(calculateExtremum(candles, Candle::getOpenPrice, true))
                .closePrice(calculateExtremum(candles, Candle::getClosePrice, true))
                .highPrice(calculateExtremum(candles, Candle::getHighPrice, true))
                .lowPrice(calculateExtremum(candles, Candle::getLowPrice, true))
                .priceRange(calculateExtremum(candles, candle ->
                        candle.getHighPrice().subtract(candle.getLowPrice()), true))
                .volume(calculateExtremum(candles, Candle::getVolume, true))
                .amount(calculateExtremum(candles, Candle::getAmount, true))
                .build();
    }

    private Min calculateMinValues(List<Candle> candles) {
        return Min.builder()
                .openPrice(calculateExtremum(candles, Candle::getOpenPrice, false))
                .closePrice(calculateExtremum(candles, Candle::getClosePrice, false))
                .highPrice(calculateExtremum(candles, Candle::getHighPrice, false))
                .lowPrice(calculateExtremum(candles, Candle::getLowPrice, false))
                .priceRange(calculateExtremum(candles, candle ->
                        candle.getHighPrice().subtract(candle.getLowPrice()), false))
                .volume(calculateExtremum(candles, Candle::getVolume, false))
                .amount(calculateExtremum(candles, Candle::getAmount, false))
                .build();
    }

    private BigDecimal calculateExtremum(List<Candle> candles, Function<Candle, BigDecimal> extractor, boolean isMax) {
        return candles.stream()
                .map(extractor)
                .reduce((a, b) -> isMax ? a.max(b) : a.min(b))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateTotal(List<Candle> candles, Function<Candle, BigDecimal> function) {
        return candles.stream()
                .map(function).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Current getCurrent(List<Candle> candles) {
        log.trace("StatisticService | Receiving candle current values");
        var lastCandle = candles.getLast();
        return Current.builder()
                .openPrice(lastCandle.getOpenPrice())
                .closePrice(lastCandle.getClosePrice())
                .highPrice(lastCandle.getHighPrice())
                .lowPrice(lastCandle.getLowPrice())
                .volume(lastCandle.getVolume())
                .amount(lastCandle.getAmount())
                .openTime(lastCandle.getOpenTime())
                .closeTime(lastCandle.getCloseTime())
                .trades(lastCandle.getTrades())
                .build();
    }

    private Average getAverage(List<Candle> candles) {
        log.trace("StatisticService | Receiving candle average values");
        var totalVolume = calculateTotal(candles, Candle::getVolume);
        return Average.builder()
                .openPrice(calculateAverage(candles, Candle::getOpenPrice))
                .closePrice(calculateAverage(candles, Candle::getClosePrice))
                .highPrice(calculateAverage(candles, Candle::getHighPrice))
                .lowPrice(calculateAverage(candles, Candle::getLowPrice))
                .tradePrice(getTradePrice(candles, totalVolume))
                .priceRange(calculateAverage(candles, candle ->
                        candle.getHighPrice().subtract(candle.getLowPrice())))
                .build();


    }

    private BigDecimal getTradePrice(List<Candle> candles, BigDecimal totalVolume) {
        log.trace("StatisticService | Receiving candle trade price");
        var weightedPriceSum = candles.stream()
                .map(q -> {
                    var midPrice = q.getHighPrice()
                            .add(q.getLowPrice())
                            .add(q.getClosePrice())
                            .divide(BigDecimal.valueOf(3), 10, RoundingMode.HALF_UP);
                    return midPrice.multiply(q.getVolume());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalVolume.compareTo(BigDecimal.ZERO) > 0
                ? weightedPriceSum.divide(totalVolume, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private Total getTotal(List<Candle> candles) {
        log.trace("StatisticService | Receiving candle total values");
        return Total.builder()
                .volume(calculateTotal(candles, Candle::getVolume))
                .amount(calculateTotal(candles, Candle::getAmount))
                .build();
    }

    private Info getInfo(List<Candle> candles, Instant beginTime, Instant endTime) {
        log.trace("StatisticService | Receiving candle info");
        return Info.builder()
                .entriesCount(candles.size())
                .beginTime(beginTime)
                .endTime(endTime)
                .duration(Duration.between(beginTime, endTime))
                .build();
    }

    private Instant getEndTime(List<Candle> candles) {
        log.trace("StatisticService | Receiving candle end time");
        return candles.getLast().getCloseTime();
    }

    private Instant getBeginTime(List<Candle> candles, String duration) {
        log.trace("StatisticService | Receiving candle begin time");
        var endTime = getEndTime(candles);

        long value = Long.parseLong(duration.substring(0, duration.length() - 1));
        String unit = duration.substring(duration.length() - 1).toLowerCase();

        return switch (unit) {
            case "d" -> endTime.minus(value, ChronoUnit.DAYS);
            case "h" -> endTime.minus(value, ChronoUnit.HOURS);
            case "m" -> endTime.minus(value, ChronoUnit.MINUTES);
            case "s" -> endTime.minus(value, ChronoUnit.SECONDS);
            default -> throw new IllegalArgumentException("Unknown duration unit: %s"
                    .formatted(unit));
        };
    }
}