package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.model.dto.Analysis;
import com.dzenthai.cryptora.model.entity.Candle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AnalysisService {

    private final Integer shortTimePeriod;

    private final Integer longTimePeriod;

    private final Integer period;

    private final Integer overbought;

    private final Integer oversold;

    private final Integer atrPeriod;

    private final Double atrMultiplier;

    private final CandleService candleService;


    public AnalysisService(
            @Value("${cryptora.short.time.period}") Integer shortTimePeriod,
            @Value("${cryptora.long.time.period}") Integer longTimePeriod,
            @Value("${cryptora.rsi.period}") Integer period,
            @Value("${cryptora.rsi.overbought}") Integer overbought,
            @Value("${cryptora.rsi.oversold}") Integer oversold,
            @Value("${cryptora.atr.period}") Integer atrPeriod,
            @Value("${cryptora.atr.multiplier}") Double atrMultiplier,
            CandleService candleService
    ) {
        this.shortTimePeriod = shortTimePeriod;
        this.longTimePeriod = longTimePeriod;
        this.period = period;
        this.overbought = overbought;
        this.oversold = oversold;
        this.atrPeriod = atrPeriod;
        this.atrMultiplier = atrMultiplier;
        this.candleService = candleService;
    }

    public Analysis getAnalysis(String baseAsset) {
        log.trace("AnalysisService | Receiving analysis via API for base asset: {}", baseAsset);
        return analyzeSymbolCandles(baseAsset, candleService.getCandleBySymbol(baseAsset), false);
    }

    public void getAnalysis() {
        log.info("AnalysisService | Receiving analysis via logs");
        candleService.getAllCandles().stream()
                .collect(Collectors.groupingBy(Candle::getSymbol))
                .forEach((baseAsset, candles) ->
                        analyzeSymbolCandles(baseAsset, candles, true));
    }

    private Analysis analyzeSymbolCandles(String baseAsset, List<Candle> candles, boolean shouldLog) {
        String symbol = baseAsset.toUpperCase().endsWith("USDT")
                ? baseAsset.toUpperCase()
                : baseAsset.toUpperCase() + "USDT".toUpperCase();
        log.debug("AnalysisService | Analyze symbol candles, symbol: {}, logging:{}", symbol, shouldLog);
        var sortedCandles = candles.stream()
                .sorted(Comparator.comparing(Candle::getOpenTime))
                .toList();

        var series = buildBarSeries(sortedCandles);
        int requiredBars = Math.max(longTimePeriod, Math.max(period, atrPeriod));

        if (series.getBarCount() < requiredBars) {
            var warn = String.format("Insufficient data: %d bars available, %d required",
                    series.getBarCount(), requiredBars);
            if (shouldLog) {
                log.warn("AnalysisService | Symbol: {}, Action: {}", symbol, warn);
            }
            return Analysis.builder()
                    .symbol(symbol)
                    .action(warn)
                    .build();
        }
        return evaluateSignals(series, symbol, shouldLog);
    }

    private Analysis evaluateSignals(BarSeries series, String symbol, boolean shouldLog) {
        log.trace("AnalysisService | Evaluating signals for base asset: {}", symbol);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortTermSMA = new SMAIndicator(closePrice, shortTimePeriod);
        SMAIndicator longTermSMA = new SMAIndicator(closePrice, longTimePeriod);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);
        ATRIndicator atr = new ATRIndicator(series, atrPeriod);
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator avgVolume = new SMAIndicator(volume, 20);

        int endIndex = series.getEndIndex();

        Num latestPrice = series.getLastBar().getClosePrice();
        Num shortTermValue = shortTermSMA.getValue(endIndex);
        Num longTermValue = longTermSMA.getValue(endIndex);
        Num rsiValue = rsi.getValue(endIndex);
        Num atrValue = atr.getValue(endIndex);
        Num currentVolume = volume.getValue(endIndex);
        Num averageVolume = avgVolume.getValue(endIndex);

        var thresholds = calculateThresholds(series, longTermValue);
        Num thresholdUpper = thresholds[0];
        Num thresholdLower = thresholds[1];

        String volatility = calculateVolatility(atrValue, latestPrice);
        String trendStrength = calculateTrendStrength(shortTermValue, longTermValue, rsiValue);
        String marketState = calculateMarketState(latestPrice, shortTermValue, longTermValue,
                thresholdUpper, thresholdLower, volatility);
        String liquidity = calculateLiquidity(currentVolume, averageVolume);
        String riskLevel = calculateRiskLevel(volatility, trendStrength, liquidity);
        Integer confidenceScore = calculateConfidenceScore(rsiValue, trendStrength, volatility, liquidity);
        String action = determineAction(latestPrice, shortTermValue, rsiValue,
                thresholdUpper, thresholdLower);

        log.debug("AnalysisService | Analysis details, base asset: {}, price: {}, RSI: {}, volatility: {}, trend strength:{}, action: {}",
                symbol, latestPrice, rsiValue, volatility, trendStrength, action);

        return buildAnalysis(symbol, action, marketState, volatility, trendStrength,
                liquidity, riskLevel, confidenceScore, shouldLog);
    }

    private String calculateVolatility(Num atr, Num price) {
        log.trace("AnalysisService | Calculating volatility using ATR");
        double atrPercent = atr.dividedBy(price).multipliedBy(DecimalNum.valueOf(100)).doubleValue();

        if (atrPercent < 1.0) return "LOW";
        if (atrPercent < 3.0) return "MEDIUM";
        return "HIGH";
    }

    private String calculateTrendStrength(Num shortSMA, Num longSMA, Num rsi) {
        log.trace("AnalysisService | Calculating trend strength using RSI");
        Num smaDiff = shortSMA.minus(longSMA).abs();
        double diffPercent = smaDiff.dividedBy(longSMA).multipliedBy(DecimalNum.valueOf(100)).doubleValue();

        double rsiValue = rsi.doubleValue();
        boolean strongRSI = rsiValue < 30 || rsiValue > 70;

        if (diffPercent > 2.0 && strongRSI) return "STRONG";
        if (diffPercent > 0.5) return "MODERATE";
        return "WEAK";
    }

    private String calculateMarketState(Num price, Num shortSMA, Num longSMA,
                                        Num thresholdUpper, Num thresholdLower,
                                        String volatility) {
        log.trace("AnalysisService | Calculating market state using using price");
        Num smaDiff = shortSMA.minus(longSMA).abs();
        double diffPercent = smaDiff.dividedBy(longSMA).multipliedBy(DecimalNum.valueOf(100)).doubleValue();

        boolean nearUpperThreshold = price.isGreaterThan(thresholdUpper.multipliedBy(DecimalNum.valueOf(0.98)));
        boolean nearLowerThreshold = price.isLessThan(thresholdLower.multipliedBy(DecimalNum.valueOf(1.02)));

        if (nearUpperThreshold || nearLowerThreshold) {
            return "BREAKOUT_ATTEMPT";
        }

        if (diffPercent < 0.5 && "LOW".equals(volatility)) {
            return "CONSOLIDATION";
        }

        if (diffPercent > 1.0) {
            return "TRENDING";
        }

        return "CONSOLIDATION";
    }

    private String calculateLiquidity(Num currentVolume, Num averageVolume) {
        log.trace("AnalysisService | Calculating liquidity using current volume");
        if (averageVolume.isZero()) return "NORMAL";

        double volumeRatio = currentVolume.dividedBy(averageVolume).doubleValue();

        if (volumeRatio < 0.5) return "LOW";
        if (volumeRatio > 1.5) return "HIGH";
        return "NORMAL";
    }

    private String calculateRiskLevel(String volatility, String trendStrength, String liquidity) {
        log.trace("AnalysisService | Calculating risk level using volatility");
        int riskScore = 0;

        riskScore += switch (volatility) {
            case "HIGH" -> 3;
            case "LOW" -> 1;
            default -> 2;
        };

        riskScore += switch (trendStrength) {
            case "WEAK" -> 3;
            case "STRONG" -> 1;
            default -> 2;
        };

        riskScore += switch (liquidity) {
            case "LOW" -> 3;
            case "HIGH" -> 1;
            default -> 2;
        };

        if (riskScore <= 4) return "LOW";
        if (riskScore <= 6) return "MEDIUM";
        return "HIGH";
    }

    private Integer calculateConfidenceScore(Num rsi, String trendStrength,
                                             String volatility, String liquidity) {
        log.trace("AnalysisService | Calculating confidence score using RSI");
        int score = 50;

        double rsiValue = rsi.doubleValue();
        if (rsiValue < 30 || rsiValue > 70) {
            score += 20;
        } else if (rsiValue > 40 && rsiValue < 60) {
            score -= 10;
        }

        score += switch (trendStrength) {
            case "STRONG" -> 15;
            case "MODERATE" -> 5;
            case "WEAK" -> -5;
            default -> 0;
        };

        score += switch (volatility) {
            case "LOW" -> 10;
            case "HIGH" -> -10;
            default -> 0;
        };

        score += switch (liquidity) {
            case "HIGH" -> 10;
            case "NORMAL" -> 5;
            case "LOW" -> -10;
            default -> 0;
        };

        return Math.min(100, score);
    }

    private String determineAction(Num latestPrice, Num shortTermValue, Num rsiValue,
                                   Num thresholdUpper, Num thresholdLower) {
        log.trace("AnalysisService | Determining action using RSI, value: {} oversold: {}, overbought: {}",
                rsiValue, oversold, overbought);
        if (shortTermValue.isGreaterThan(thresholdUpper)
                && latestPrice.isGreaterThan(shortTermValue)
                && rsiValue.isLessThan(DecimalNum.valueOf(oversold))) {
            return "BUY";
        } else if (shortTermValue.isLessThan(thresholdLower)
                && latestPrice.isLessThan(shortTermValue)
                && rsiValue.isGreaterThan(DecimalNum.valueOf(overbought))) {
            return "SELL";
        }
        return "HOLD";
    }

    private Num[] calculateThresholds(BarSeries series, Num longTermValue) {
        ATRIndicator atrIndicator = new ATRIndicator(series, atrPeriod);
        Num atrValue = atrIndicator.getValue(series.getEndIndex());

        Num thresholdUpper = longTermValue.plus(atrValue.multipliedBy(DecimalNum.valueOf(atrMultiplier)));
        Num thresholdLower = longTermValue.minus(atrValue.multipliedBy(DecimalNum.valueOf(atrMultiplier)));

        log.trace("AnalysisService | Calculating threshold using ATR, value: {}, multiplier: {}, upper threshold: {}, lower threshold: {}",
                atrValue, atrMultiplier, thresholdUpper, thresholdLower);

        return new Num[]{thresholdUpper, thresholdLower};
    }

    private Analysis buildAnalysis(
            String symbol, String action, String marketState, String volatility,
            String trendStrength, String liquidity, String riskLevel,
            Integer confidenceScore, boolean shouldLog
    ) {
        if (shouldLog) {
            var timestamp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ssZ")
                    .format(Instant.now().atZone(ZoneOffset.UTC));
            log.info("AnalysisService | Symbol: {}, Action: {}, Market state: {}, Volatility: {}, Trend: {}, Liquidity: {}, Risk level: {}, Confidence score: {}%, Timestamp: {}",
                    symbol, action, marketState, volatility, trendStrength, liquidity, riskLevel, confidenceScore, timestamp);
        }

        return Analysis.builder()
                .symbol(symbol)
                .action(action)
                .marketState(marketState)
                .volatility(volatility)
                .trendStrength(trendStrength)
                .liquidity(liquidity)
                .riskLevel(riskLevel)
                .confidenceScore(confidenceScore)
                .build();
    }

    private BarSeries buildBarSeries(List<Candle> candles) {
        log.trace("AnalysisService | Building bar series, candles count: {}", candles.size());
        List<Bar> barList = new ArrayList<>();
        Instant lastBarEndTime = null;

        for (Candle candle : candles) {
            Instant endTime = candle.getCloseTime();
            if (lastBarEndTime != null && !endTime.isAfter(lastBarEndTime)) {
                log.warn("AnalysisService | Bar with end time {} skipped; not later than previous {}",
                        endTime, lastBarEndTime);
                continue;
            }
            barList.add(buildBar(candle));
            lastBarEndTime = endTime;
        }

        return new BaseBarSeriesBuilder()
                .withName("CryptoSeries")
                .withBars(barList)
                .withMaxBarCount(1000)
                .build();
    }

    private Bar buildBar(Candle candle) {
        log.trace("Building bar, symbol: {}, openTime: {}, closeTime: {}",
                candle.getSymbol(), candle.getOpenTime(), candle.getCloseTime());
        return new BaseBar(
                candle.getTimePeriod(),
                candle.getOpenTime(),
                candle.getCloseTime(),
                DecimalNum.valueOf(candle.getOpenPrice()),
                DecimalNum.valueOf(candle.getHighPrice()),
                DecimalNum.valueOf(candle.getLowPrice()),
                DecimalNum.valueOf(candle.getClosePrice()),
                DecimalNum.valueOf(candle.getVolume()),
                DecimalNum.valueOf(candle.getAmount()),
                candle.getTrades()
        );
    }
}