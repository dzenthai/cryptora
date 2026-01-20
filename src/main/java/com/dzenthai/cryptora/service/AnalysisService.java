package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.mapper.AnalysisMapper;
import com.dzenthai.cryptora.mapper.IndicatorMapper;
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
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AnalysisService {

    private final int shortTimePeriod;

    private final int longTimePeriod;

    private final int period;

    private final int overbought;

    private final int oversold;

    private final int atrPeriod;

    private final double atrMultiplier;

    private final int volumePeriod;

    private final CandleService candleService;

    private final AnalysisMapper analysisMapper;

    private final IndicatorMapper indicatorMapper;

    public AnalysisService(
            @Value("${cryptora.short.time.period}") Integer shortTimePeriod,
            @Value("${cryptora.long.time.period}") Integer longTimePeriod,
            @Value("${cryptora.rsi.period}") Integer period,
            @Value("${cryptora.rsi.overbought}") Integer overbought,
            @Value("${cryptora.rsi.oversold}") Integer oversold,
            @Value("${cryptora.atr.period}") Integer atrPeriod,
            @Value("${cryptora.atr.multiplier}") Double atrMultiplier,
            @Value("${cryptora.volume.period:20}") Integer volumePeriod,
            CandleService candleService,
            AnalysisMapper analysisMapper,
            IndicatorMapper indicatorMapper
    ) {
        this.shortTimePeriod = shortTimePeriod;
        this.longTimePeriod = longTimePeriod;
        this.period = period;
        this.overbought = overbought;
        this.oversold = oversold;
        this.atrPeriod = atrPeriod;
        this.atrMultiplier = atrMultiplier;
        this.volumePeriod = volumePeriod;
        this.candleService = candleService;
        this.analysisMapper = analysisMapper;
        this.indicatorMapper = indicatorMapper;
    }

    public Analysis getAnalysis(String baseAsset) {
        return analyzeSymbolCandles(baseAsset, candleService.getCandleBySymbol(baseAsset), false);
    }

    public void getAnalysis() {
        log.info("AnalysisService | Receiving analysis via logs");
        candleService.getAllCandles().stream()
                .collect(Collectors.groupingBy(Candle::getSymbol))
                .forEach((baseAsset, candles) ->
                        analyzeSymbolCandles(baseAsset, candles, true)
                );
    }

    private Analysis analyzeSymbolCandles(String baseAsset, List<Candle> candles, boolean shouldLog) {
        log.debug("AnalysisService | Analyzing symbol candles, base asset: {}", baseAsset);
        String symbol = (baseAsset.endsWith("USDT")
                ? baseAsset
                : baseAsset + "USDT")
                .toUpperCase(Locale.ROOT);

        var sortedCandles = candles.stream()
                .sorted(Comparator.comparing(Candle::getOpenTime))
                .toList();

        var series = buildBarSeries(sortedCandles);
        int requiredBars = Math.max(longTimePeriod, Math.max(period, Math.max(atrPeriod, volumePeriod)));

        if (series.getBarCount() < requiredBars) {
            return Analysis.builder()
                    .symbol(symbol)
                    .action("INSUFFICIENT_DATA")
                    .build();
        }

        return evaluateSignals(series, symbol, shouldLog);
    }

    private Analysis evaluateSignals(BarSeries series, String symbol, boolean shouldLog) {
        log.debug("AnalysisService | Evaluating signals, symbol: {}, bar count: {}", symbol, series.getBarCount());
        ClosePriceIndicator close = new ClosePriceIndicator(series);

        SMAIndicator smaShort = new SMAIndicator(close, shortTimePeriod);
        SMAIndicator smaLong = new SMAIndicator(close, longTimePeriod);
        RSIIndicator rsiRaw = new RSIIndicator(close, period);
        ATRIndicator atrRaw = new ATRIndicator(series, atrPeriod);

        int end = series.getEndIndex();
        int prev = Math.max(0, end - 1);

        Num price = series.getLastBar().getClosePrice();
        Num shortSMA = smaShort.getValue(end);
        Num longSMA = smaLong.getValue(end);
        Num rsiVal = rsiRaw.getValue(end);
        Num atrVal = atrRaw.getValue(end);

        Num[] thresholds = calculateThresholds(longSMA, atrVal);
        Num thrUp = thresholds[0];
        Num thrLo = thresholds[1];

        double recentAvgVol = calculateRecentAvgVolume(series);
        double currVol = series.getLastBar().getVolume().doubleValue();

        String volatility = calculateVolatility(atrVal, price);
        String trendStrength = calculateTrendStrength(shortSMA, longSMA, rsiVal);
        String liquidity = calculateLiquidity(series);
        String marketState = calculateMarketState(price, shortSMA, longSMA, thrUp, thrLo, volatility, liquidity, series);
        String riskLevel = calculateRiskLevel(volatility, trendStrength, liquidity);

        Integer confidence = calculateConfidenceScore(rsiVal, shortSMA, longSMA, trendStrength, volatility, liquidity, marketState);

        int scoreNow = calculateScore(price, shortSMA, longSMA, rsiVal, thrUp, thrLo, liquidity);
        int scorePrev = calculateScore(
                series.getBar(prev).getClosePrice(),
                smaShort.getValue(prev),
                smaLong.getValue(prev),
                rsiRaw.getValue(prev),
                thrUp,
                thrLo,
                liquidity
        );

        String action = determineAction(scoreNow, scorePrev, currVol, recentAvgVol, marketState, trendStrength, liquidity);

        double smaDiff = calculateSMADiffPercent(shortSMA, longSMA);
        boolean volumeOk = currVol >= Math.max(recentAvgVol, 1e-6) * 0.5;

        var indicator = indicatorMapper.mapToIndicator(
                symbol,
                price,
                shortTimePeriod,
                shortSMA,
                longTimePeriod,
                longSMA,
                smaDiff,
                rsiVal,
                atrVal,
                thrUp,
                thrLo,
                currVol,
                recentAvgVol,
                volumeOk,
                shouldLog
        );

        return analysisMapper.mapToAnalysis(
                symbol,
                action,
                marketState,
                volatility,
                trendStrength,
                liquidity,
                riskLevel,
                confidence,
                indicator,
                shouldLog
        );
    }

    private String determineAction(
            int scoreNow, int scorePrev, double currVol, double avgVol,
            String marketState, String trendStrength, String liquidity) {

        log.trace("AnalysisService | Determining action");
        double safeAvgVol = Math.max(avgVol, 1e-6);

        boolean volumeOk = currVol >= safeAvgVol * 0.5;
        boolean liquidityHighOrNormal = "HIGH".equals(liquidity) || "NORMAL".equals(liquidity);
        boolean liquidityLow = "LOW".equals(liquidity);

        if (("CONSOLIDATION".equals(marketState) || "RANGE".equals(marketState))
                && "WEAK".equals(trendStrength)) {
            if (Math.abs(scoreNow) < 3 || liquidityLow) {
                return "HOLD";
            }
        }

        if ("BREAKOUT_ATTEMPT".equals(marketState)) {
            if (scoreNow >= 3 && volumeOk && liquidityHighOrNormal) return "BUY";
            if (scoreNow <= -3 && volumeOk && liquidityHighOrNormal) return "SELL";
            return "HOLD";
        }

        if (scoreNow >= 4 && scorePrev >= 2 && volumeOk && liquidityHighOrNormal) return "STRONG_BUY";
        if (scoreNow <= -4 && scorePrev <= -2 && volumeOk && liquidityHighOrNormal) return "STRONG_SELL";

        if (scoreNow >= 2) {
            if (scorePrev >= 1) {
                if (liquidityLow && !volumeOk) return "HOLD";
                return "BUY";
            }
            if (volumeOk && liquidityHighOrNormal) return "BUY";
        }
        if (scoreNow <= -2) {
            if (scorePrev <= -1) {
                if (liquidityLow && !volumeOk) return "HOLD";
                return "SELL";
            }
            if (volumeOk && liquidityHighOrNormal) return "SELL";
        }
        return "HOLD";
    }

    private int calculateScore(
            Num price, Num shortSMA, Num longSMA,
            Num rsi, Num thrUp, Num thrLo, String liquidity) {
        log.trace("AnalysisService | Calculating score");
        int score = 0;

        double smaDiffPercent = calculateSMADiffPercent(shortSMA, longSMA);

        if (smaDiffPercent > 0.5) score += 2;
        else if (smaDiffPercent < -0.5) score -= 2;

        if (rsi.isLessThan(DecimalNum.valueOf(oversold))) score += 2;

        if (rsi.isGreaterThan(DecimalNum.valueOf(overbought))) score -= 2;
        else if (rsi.isGreaterThan(DecimalNum.valueOf(overbought - 10))) score -= 1;

        boolean highVolume = "HIGH".equals(liquidity) || "NORMAL".equals(liquidity);

        if (price.isGreaterThan(thrUp)) {
            if (highVolume && smaDiffPercent > 0) {
                score += 2;
            } else {
                score -= 1;
            }
        }

        if (price.isLessThan(thrLo)) {
            if (highVolume && smaDiffPercent < 0) {
                score -= 2;
            } else {
                score += 1;
            }
        }

        if ("LOW".equals(liquidity)) score -= 2;

        return score;
    }

    private double calculateRecentAvgVolume(BarSeries series) {
        log.trace("AnalysisService | Calculating recent average volume");
        int bars = Math.min(volumePeriod, series.getBarCount());
        double sum = 0;
        for (int i = series.getBarCount() - bars; i < series.getBarCount(); i++) {
            sum += series.getBar(i).getVolume().doubleValue();
        }
        return sum / bars;
    }

    private String calculateMarketState(
            Num price,
            Num shortSMA,
            Num longSMA,
            Num thrUp,
            Num thrLo,
            String volatility,
            String liquidity,
            BarSeries series
    ) {
        log.trace("AnalysisService | Calculating market state");
        int lookback = Math.min(50, series.getBarCount());
        Num high = series.getBar(series.getBarCount() - lookback).getHighPrice();
        Num low = series.getBar(series.getBarCount() - lookback).getLowPrice();

        for (int i = series.getBarCount() - lookback + 1; i < series.getBarCount(); i++) {
            high = high.max(series.getBar(i).getHighPrice());
            low = low.min(series.getBar(i).getLowPrice());
        }

        Num range = high.minus(low);
        Num compression = price.multipliedBy(DecimalNum.valueOf(0.004));

        boolean compressed = range.isLessThan(compression);

        Num eps = price.multipliedBy(DecimalNum.valueOf(0.002));
        boolean nearBreakout =
                price.isGreaterThan(thrUp.minus(eps)) ||
                        price.isLessThan(thrLo.plus(eps));

        if (nearBreakout && "HIGH".equals(liquidity)) return "BREAKOUT_ATTEMPT";
        if (compressed && "LOW".equals(volatility)) return "CONSOLIDATION";

        double smaDiffPercent = calculateSMADiffPercent(shortSMA, longSMA);
        if (Math.abs(smaDiffPercent) > 1.0) return "TRENDING";

        return "RANGE";
    }

    private String calculateVolatility(Num atr, Num price) {
        log.trace("AnalysisService | Calculating volatility");
        double p = atr.dividedBy(price).multipliedBy(DecimalNum.valueOf(100)).doubleValue();
        if (p < 1.0) return "LOW";
        if (p < 3.0) return "MEDIUM";
        return "HIGH";
    }

    private String calculateTrendStrength(Num shortSMA, Num longSMA, Num rsi) {
        log.trace("AnalysisService | Calculating trend strength");
        if (longSMA == null || longSMA.isZero()) return "WEAK";

        double diff = Math.abs(calculateSMADiffPercent(shortSMA, longSMA));
        boolean strongRsi = rsi.doubleValue() < 30 || rsi.doubleValue() > 70;

        if (diff > 1.2 && strongRsi) return "STRONG";
        if (diff > 0.25) return "MODERATE";
        return "WEAK";
    }

    private String calculateLiquidity(BarSeries series) {
        log.trace("AnalysisService | Calculating liquidity");
        int barCount = series.getBarCount();
        if (barCount < 2) return "LOW";

        int windowLookback = Math.min(20, barCount);

        double globalVolume = 0;
        for (int i = 0; i < barCount; i++) {
            globalVolume += series.getBar(i).getVolume().doubleValue();
        }
        double globalAvg = globalVolume / barCount;

        double windowVolume = 0;
        for (int i = barCount - windowLookback; i < barCount; i++) {
            windowVolume += series.getBar(i).getVolume().doubleValue();
        }
        double windowAvg = windowVolume / windowLookback;

        double curr = series.getLastBar().getVolume().doubleValue();

        double localRatio = curr / Math.max(windowAvg, 1e-6);
        double globalRatio = curr / Math.max(globalAvg, 1e-6);

        if (localRatio < 0.5 && globalRatio < 0.5) return "LOW";
        if (localRatio > 1.5 && globalRatio > 1.0) return "HIGH";
        if (localRatio < 0.7 || globalRatio < 0.5) return "LOW";

        return "NORMAL";
    }

    private String calculateRiskLevel(String volatility, String trendStrength, String liquidity) {
        log.trace("AnalysisService | Calculating risk level");
        int risk = 0;
        if ("HIGH".equals(volatility)) risk += 3;
        if ("WEAK".equals(trendStrength)) risk += 3;
        if ("LOW".equals(liquidity)) risk += 3;
        if (risk <= 3) return "LOW";
        if (risk <= 6) return "MEDIUM";
        return "HIGH";
    }

    private Integer calculateConfidenceScore(
            Num rsi, Num shortSMA, Num longSMA,
            String trendStrength, String volatility,
            String liquidity, String marketState
    ) {
        log.trace("AnalysisService | Calculating confidence score");
        int score = 50;

        double rv = rsi.doubleValue();
        double smaDiff = calculateSMADiffPercent(shortSMA, longSMA);

        if (rv < 30 || rv > 70) {
            score += 15;
            if ("LOW".equals(liquidity)) score -= 10;
        } else if (rv > 40 && rv < 60) {
            score -= 10;
        }

        if ("STRONG".equals(trendStrength)) score += 15;
        else if ("WEAK".equals(trendStrength)) score -= 5;

        if ("LOW".equals(volatility)) score += 10;
        if ("HIGH".equals(liquidity)) score += 10;
        else if ("LOW".equals(liquidity)) score -= 15;

        if (smaDiff > 0 && rv > 50) score += 10;
        if (smaDiff < 0 && rv < 50) score += 10;

        if (smaDiff > 0.5 && rv < 30) score -= 15;
        if (smaDiff < -0.5 && rv > 70) score -= 15;

        if ("CONSOLIDATION".equals(marketState) || "RANGE".equals(marketState)) {
            score -= 10;
        }

        return Math.min(100, score);
    }

    private Num[] calculateThresholds(Num base, Num atr) {
        log.trace("AnalysisService | Calculating thresholds");
        Num up = base.plus(atr.multipliedBy(DecimalNum.valueOf(atrMultiplier)));
        Num lo = base.minus(atr.multipliedBy(DecimalNum.valueOf(atrMultiplier)));
        return new Num[]{up, lo};
    }

    private double calculateSMADiffPercent(Num shortSMA, Num longSMA) {
        if (longSMA == null || longSMA.isZero()) return 0.0;

        return shortSMA.minus(longSMA)
                .dividedBy(longSMA)
                .multipliedBy(DecimalNum.valueOf(100))
                .doubleValue();
    }

    private BarSeries buildBarSeries(List<Candle> candles) {
        log.trace("AnalysisService | Building bar series");
        List<Bar> bars = new ArrayList<>();
        Instant last = null;

        for (Candle c : candles) {
            Instant end = c.getCloseTime();
            if (last != null && !end.isAfter(last)) continue;

            bars.add(buildBar(c));
            last = end;
        }

        return new BaseBarSeriesBuilder()
                .withName("CryptoSeries")
                .withBars(bars)
                .withMaxBarCount(500)
                .build();
    }

    private Bar buildBar(Candle candle) {
        log.trace("AnalysisService | Building bar, symbol: {}", candle.getSymbol());
        return new BaseBar(
                Duration.between(
                        candle.getOpenTime(),
                        candle.getCloseTime()
                ),
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