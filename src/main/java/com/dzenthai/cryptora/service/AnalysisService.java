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

    private final Integer volumePeriod;

    private final CandleService candleService;

    public AnalysisService(
            @Value("${cryptora.short.time.period}") Integer shortTimePeriod,
            @Value("${cryptora.long.time.period}") Integer longTimePeriod,
            @Value("${cryptora.rsi.period}") Integer period,
            @Value("${cryptora.rsi.overbought}") Integer overbought,
            @Value("${cryptora.rsi.oversold}") Integer oversold,
            @Value("${cryptora.atr.period}") Integer atrPeriod,
            @Value("${cryptora.atr.multiplier}") Double atrMultiplier,
            @Value("${cryptora.volume.period:20}") Integer volumePeriod,
            CandleService candleService
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
    }

    public Analysis getAnalysis(String baseAsset) {
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
                : baseAsset.toUpperCase() + "USDT";

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

        ClosePriceIndicator close = new ClosePriceIndicator(series);

        SMAIndicator smaShort = new SMAIndicator(close, shortTimePeriod);
        SMAIndicator smaLong = new SMAIndicator(close, longTimePeriod);

        RSIIndicator rsiRaw = new RSIIndicator(close, period);
        SMAIndicator rsi = new SMAIndicator(rsiRaw, 3);

        ATRIndicator atrRaw = new ATRIndicator(series, atrPeriod);
        SMAIndicator atr = new SMAIndicator(atrRaw, 3);

        int end = series.getEndIndex();
        int prev = Math.max(0, end - 1);

        Num price = series.getLastBar().getClosePrice();
        Num shortSMA = smaShort.getValue(end);
        Num longSMA = smaLong.getValue(end);
        Num rsiVal = rsi.getValue(end);
        Num atrVal = atr.getValue(end);

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
        Integer confidence = calculateConfidenceScore(rsiVal, shortSMA, longSMA, trendStrength, volatility, liquidity);

        int scoreNow = calculateScore(price, shortSMA, longSMA, rsiVal, thrUp, thrLo, liquidity);
        int scorePrev = calculateScore(
                series.getBar(prev).getClosePrice(),
                smaShort.getValue(prev),
                smaLong.getValue(prev),
                rsi.getValue(prev),
                thrUp,
                thrLo,
                liquidity
        );

        String action = determineAction(scoreNow, scorePrev, currVol, recentAvgVol);

        log.info("AnalysisService | Symbol: {}, price: {}, SMA{}: {}, SMA{}: {}, RSI: {}, ATR: {}, ATR%: {}, thrUp: {}, thrLo: {}, vol: {}/{}, score: {}",
                symbol, price, shortTimePeriod, shortSMA, longTimePeriod, longSMA,
                rsiVal, atrVal, atrVal.dividedBy(price).multipliedBy(DecimalNum.valueOf(100)),
                thrUp, thrLo, currVol, recentAvgVol, scoreNow);

        return buildAnalysis(symbol, action, marketState, volatility, trendStrength,
                liquidity, riskLevel, confidence, shouldLog);
    }

    private int calculateScore(
            Num price, Num shortSMA, Num longSMA,
            Num rsi, Num thrUp, Num thrLo, String liquidity) {

        int score = 0;

        if (shortSMA.isGreaterThan(longSMA)) score += 2;
        else score -= 2;

        if (rsi.isLessThan(DecimalNum.valueOf(oversold))) score += 2;
        else if (rsi.isLessThan(DecimalNum.valueOf(oversold + 10))) score += 1;

        if (rsi.isGreaterThan(DecimalNum.valueOf(overbought))) score -= 2;
        else if (rsi.isGreaterThan(DecimalNum.valueOf(overbought - 10))) score -= 1;

        if (price.isLessThan(thrLo)) score += 1;
        if (price.isGreaterThan(thrUp)) score -= 1;

        if ("LOW".equals(liquidity)) score -= 1;

        return score;
    }

    private String determineAction(int scoreNow, int scorePrev, double currVol, double avgVol) {

        boolean volumeOk = currVol >= avgVol * 0.3;

        if (scoreNow >= 4 && scorePrev >= 2 && volumeOk) return "STRONG_BUY";
        if (scoreNow >= 2) return "BUY";
        if (scoreNow <= -4 && scorePrev <= -2 && volumeOk) return "STRONG_SELL";
        if (scoreNow <= -2) return "SELL";

        return "HOLD";
    }

    private double calculateRecentAvgVolume(BarSeries series) {
        int bars = Math.min(volumePeriod, series.getBarCount());
        double sum = 0;
        for (int i = series.getBarCount() - bars; i < series.getBarCount(); i++) {
            sum += series.getBar(i).getVolume().doubleValue();
        }
        return sum / bars;
    }

    private String calculateMarketState(
            Num price, Num shortSMA, Num longSMA,
            Num thrUp, Num thrLo,
            String volatility, String liquidity, BarSeries series) {

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

        Num diff = shortSMA.minus(longSMA).abs()
                .dividedBy(longSMA)
                .multipliedBy(DecimalNum.valueOf(100));

        if (diff.isGreaterThan(DecimalNum.valueOf(1.0))) return "TRENDING";

        return "RANGE";
    }

    private String calculateVolatility(Num atr, Num price) {
        double p = atr.dividedBy(price).multipliedBy(DecimalNum.valueOf(100)).doubleValue();
        if (p < 1.0) return "LOW";
        if (p < 3.0) return "MEDIUM";
        return "HIGH";
    }

    private String calculateTrendStrength(Num shortSMA, Num longSMA, Num rsi) {
        double diff = shortSMA.minus(longSMA).abs()
                .dividedBy(longSMA)
                .multipliedBy(DecimalNum.valueOf(100))
                .doubleValue();

        boolean strongRsi = rsi.doubleValue() < 30 || rsi.doubleValue() > 70;

        if (diff > 1.2 && strongRsi) return "STRONG";
        if (diff > 0.25) return "MODERATE";
        return "WEAK";
    }

    private String calculateLiquidity(BarSeries series) {
        int bars = Math.min(20, series.getBarCount());
        double sum = 0;
        for (int i = series.getBarCount() - bars; i < series.getBarCount(); i++) {
            sum += series.getBar(i).getVolume().doubleValue();
        }
        double avg = sum / bars;
        double curr = series.getLastBar().getVolume().doubleValue();

        if (curr < avg * 0.5) return "LOW";
        if (curr > avg * 1.5) return "HIGH";
        return "NORMAL";
    }

    private String calculateRiskLevel(String volatility, String trendStrength, String liquidity) {
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
            String trendStrength, String volatility, String liquidity) {

        int score = 50;

        double rv = rsi.doubleValue();
        if (rv < 30 || rv > 70) score += 20;
        else if (rv > 40 && rv < 60) score -= 10;

        if ("STRONG".equals(trendStrength)) score += 15;
        if ("LOW".equals(volatility)) score += 10;
        if ("HIGH".equals(liquidity)) score += 10;

        if (shortSMA.isGreaterThan(longSMA) && rv > 50) score += 10;
        if (shortSMA.isLessThan(longSMA) && rv < 50) score += 10;

        return Math.min(100, score);
    }

    private Num[] calculateThresholds(Num base, Num atr) {
        Num up = base.plus(atr.multipliedBy(DecimalNum.valueOf(atrMultiplier)));
        Num lo = base.minus(atr.multipliedBy(DecimalNum.valueOf(atrMultiplier)));
        return new Num[]{up, lo};
    }

    private Analysis buildAnalysis(
            String symbol, String action, String marketState, String volatility,
            String trendStrength, String liquidity, String riskLevel,
            Integer confidenceScore, boolean shouldLog) {

        if (shouldLog) {
            String timestamp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ssZ")
                    .format(Instant.now().atZone(ZoneOffset.UTC));
            log.info("AnalysisService | Symbol: {}, Action: {}, Market: {}, Volatility: {}, Trend: {}, Liquidity: {}, Risk: {}, Confidence: {}%, Time: {}",
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

    private Bar buildBar(Candle c) {
        return new BaseBar(
                c.getTimePeriod(),
                c.getOpenTime(),
                c.getCloseTime(),
                DecimalNum.valueOf(c.getOpenPrice()),
                DecimalNum.valueOf(c.getHighPrice()),
                DecimalNum.valueOf(c.getLowPrice()),
                DecimalNum.valueOf(c.getClosePrice()),
                DecimalNum.valueOf(c.getVolume()),
                DecimalNum.valueOf(c.getAmount()),
                c.getTrades()
        );
    }
}
