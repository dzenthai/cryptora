package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.model.dto.Analysis;
import com.dzenthai.cryptora.model.entity.Quote;
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

    private final QuoteService quoteService;


    public AnalysisService(
            @Value("${cryptora.short.time.period}") Integer shortTimePeriod,
            @Value("${cryptora.long.time.period}") Integer longTimePeriod,
            @Value("${cryptora.rsi.period}") Integer period,
            @Value("${cryptora.rsi.overbought}") Integer overbought,
            @Value("${cryptora.rsi.oversold}") Integer oversold,
            @Value("${cryptora.atr.period}") Integer atrPeriod,
            @Value("${cryptora.atr.multiplier}") Double atrMultiplier,
            QuoteService quoteService
    ) {
        this.shortTimePeriod = shortTimePeriod;
        this.longTimePeriod = longTimePeriod;
        this.period = period;
        this.overbought = overbought;
        this.oversold = oversold;
        this.atrPeriod = atrPeriod;
        this.atrMultiplier = atrMultiplier;
        this.quoteService = quoteService;
    }

    public Analysis getAnalysis(String ticker) {
        return analyzeTickerQuotes(ticker, quoteService.getQuotesByTicker(ticker), false);
    }

    public void getAnalysis() {
        quoteService.getAllQuotes().stream()
                .collect(Collectors.groupingBy(Quote::getTicker))
                .forEach((ticker, quotes) ->
                        analyzeTickerQuotes(ticker, quotes, true));
    }

    private Analysis analyzeTickerQuotes(String ticker, List<Quote> quotes, boolean shouldLog) {
        var shortCut = ticker.replaceAll("USDT", "").toUpperCase();
        var sortedQuotes = quotes.stream()
                .sorted(Comparator.comparing(Quote::getOpenTime))
                .toList();

        var series = buildBarSeries(sortedQuotes);
        int requiredBars = Math.max(longTimePeriod, Math.max(period, atrPeriod));

        if (series.getBarCount() < requiredBars) {
            var warn = String.format("Insufficient data: %d bars available, %d required",
                    series.getBarCount(), requiredBars);
            if (shouldLog) {
                log.warn("AnalysisService | Quote: {}/USDT, Action: {}", shortCut, warn);
            }
            return Analysis.builder()
                    .ticker(shortCut)
                    .action(warn)
                    .build();
        }
        return evaluateSignals(series, shortCut, shouldLog);
    }

    private Analysis evaluateSignals(BarSeries series, String ticker, boolean shouldLog) {
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

        return buildAnalysis(ticker, action, marketState, volatility, trendStrength,
                liquidity, riskLevel, confidenceScore, shouldLog);
    }

    private String calculateVolatility(Num atr, Num price) {
        double atrPercent = atr.dividedBy(price).multipliedBy(DecimalNum.valueOf(100)).doubleValue();

        if (atrPercent < 1.0) return "LOW";
        if (atrPercent < 3.0) return "MEDIUM";
        return "HIGH";
    }

    private String calculateTrendStrength(Num shortSMA, Num longSMA, Num rsi) {
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
        if (averageVolume.isZero()) return "NORMAL";

        double volumeRatio = currentVolume.dividedBy(averageVolume).doubleValue();

        if (volumeRatio < 0.5) return "LOW";
        if (volumeRatio > 1.5) return "HIGH";
        return "NORMAL";
    }

    private String calculateRiskLevel(String volatility, String trendStrength, String liquidity) {
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

        return new Num[]{thresholdUpper, thresholdLower};
    }

    private Analysis buildAnalysis(
            String ticker, String action, String marketState, String volatility,
            String trendStrength, String liquidity, String riskLevel,
            Integer confidenceScore, boolean shouldLog
    ) {
        if (shouldLog) {
            var timestamp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ssZ")
                    .format(Instant.now().atZone(ZoneOffset.UTC));
            log.info("AnalysisService | Ticker: {}/USDT, Action: {}, Market state: {}, Volatility: {}, Trend: {}, Liquidity: {}, Risk level: {}, Confidence score: {}%, Timestamp: {}",
                    ticker, action, marketState, volatility, trendStrength, liquidity, riskLevel, confidenceScore, timestamp);
        }

        return Analysis.builder()
                .ticker(ticker)
                .action(action)
                .marketState(marketState)
                .volatility(volatility)
                .trendStrength(trendStrength)
                .liquidity(liquidity)
                .riskLevel(riskLevel)
                .confidenceScore(confidenceScore)
                .build();
    }

    private BarSeries buildBarSeries(List<Quote> quotes) {
        List<Bar> barList = new ArrayList<>();
        Instant lastBarEndTime = null;

        for (Quote quote : quotes) {
            Instant endTime = quote.getCloseTime();
            if (lastBarEndTime != null && !endTime.isAfter(lastBarEndTime)) {
                log.warn("AnalysisService | Bar with end time {} skipped; not later than previous {}",
                        endTime, lastBarEndTime);
                continue;
            }
            barList.add(buildBar(quote));
            lastBarEndTime = endTime;
        }

        return new BaseBarSeriesBuilder()
                .withName("CryptoSeries")
                .withBars(barList)
                .withMaxBarCount(1000)
                .build();
    }

    private Bar buildBar(Quote quote) {
        return new BaseBar(
                quote.getTimePeriod(),
                quote.getOpenTime(),
                quote.getCloseTime(),
                DecimalNum.valueOf(quote.getOpenPrice()),
                DecimalNum.valueOf(quote.getHighPrice()),
                DecimalNum.valueOf(quote.getLowPrice()),
                DecimalNum.valueOf(quote.getClosePrice()),
                DecimalNum.valueOf(quote.getVolume()),
                DecimalNum.valueOf(quote.getAmount()),
                quote.getTrades()
        );
    }
}