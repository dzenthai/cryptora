package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.configuration.CryptoraProperties;
import com.dzenthai.cryptora.mapper.AnalysisMapper;
import com.dzenthai.cryptora.mapper.IndicatorMapper;
import com.dzenthai.cryptora.model.dto.Analysis;
import com.dzenthai.cryptora.model.entity.Candle;
import lombok.extern.slf4j.Slf4j;
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

    private final CryptoraProperties cryptoraProperties;

    private final CandleService candleService;

    private final AnalysisMapper analysisMapper;

    private final IndicatorMapper indicatorMapper;

    public AnalysisService(
            CryptoraProperties cryptoraProperties,
            CandleService candleService,
            AnalysisMapper analysisMapper,
            IndicatorMapper indicatorMapper
    ) {
        this.cryptoraProperties = cryptoraProperties;
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

        int requiredBars = Math.max(
                cryptoraProperties.longTime().period(),
                Math.max(
                        cryptoraProperties.rsi().period(),
                        Math.max(
                                cryptoraProperties.atr().period(),
                                cryptoraProperties.tuning().volume().windowLookback()
                        )
                )
        );

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

        SMAIndicator smaShort = new SMAIndicator(close, cryptoraProperties.shortTime().period());
        SMAIndicator smaLong = new SMAIndicator(close, cryptoraProperties.longTime().period());
        RSIIndicator rsiRaw = new RSIIndicator(close, cryptoraProperties.rsi().period());
        ATRIndicator atrRaw = new ATRIndicator(series, cryptoraProperties.atr().period());

        int end = series.getEndIndex();
        int prev = Math.max(0, end - 1);

        Num price = series.getLastBar().getClosePrice();
        Num shortSMA = smaShort.getValue(end);
        Num longSMA = smaLong.getValue(end);
        Num rsiVal = rsiRaw.getValue(end);
        Num atrVal = atrRaw.getValue(end);

        double recentAvgVol = calculateRecentAvgVolume(series);
        double currVol = series.getLastBar().getVolume().doubleValue();

        String volatility = calculateVolatility(atrVal, price);
        String trendStrength = calculateTrendStrength(shortSMA, longSMA, rsiVal);
        String liquidity = calculateLiquidity(series);

        Num[] thresholds;
        if ("STRONG".equals(trendStrength)) {
            thresholds = calculateThresholds(longSMA, atrVal, cryptoraProperties.atr().multiplier().strong());
        } else {
            thresholds = calculateThresholds(longSMA, atrVal, cryptoraProperties.atr().multiplier().weak());
        }
        Num thrUp = thresholds[0];
        Num thrLo = thresholds[1];

        String marketState = calculateMarketState(price, shortSMA, longSMA, thrUp, thrLo, volatility, liquidity, series);
        String riskLevel = calculateRiskLevel(volatility, trendStrength, liquidity);

        var confidenceScore = calculateConfidenceScore(rsiVal, shortSMA, longSMA, trendStrength, volatility, liquidity, marketState);

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

        var action = determineAction(scoreNow, scorePrev, currVol, recentAvgVol, marketState, trendStrength, liquidity);

        double smaDiff = calculateSMADiffPercent(shortSMA, longSMA);
        boolean volumeOk = currVol >= Math.max(recentAvgVol, cryptoraProperties.tuning().thresholds().minSafeValue())
                * cryptoraProperties.tuning().volume().minRelativeToAvg();

        var indicator = indicatorMapper.mapToIndicator(
                symbol,
                price,
                cryptoraProperties.shortTime().period(),
                shortSMA,
                cryptoraProperties.longTime().period(),
                longSMA,
                smaDiff,
                rsiVal,
                atrVal,
                thrUp,
                thrLo,
                currVol,
                recentAvgVol,
                volumeOk,
                confidenceScore,
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
                confidenceScore,
                indicator,
                shouldLog
        );
    }

    private String determineAction(
            int scoreNow, int scorePrev, double currVol, double avgVol,
            String marketState, String trendStrength, String liquidity) {

        log.trace("AnalysisService | Determining action");
        double safeAvgVol = Math.max(avgVol, cryptoraProperties.tuning().thresholds().minSafeValue());

        boolean volumeOk = currVol >= safeAvgVol * cryptoraProperties.tuning().volume().minRelativeToAvg();
        boolean liquidityHighOrNormal = "HIGH".equals(liquidity) || "NORMAL".equals(liquidity);
        boolean liquidityLow = "LOW".equals(liquidity);

        int breakoutThreshold = cryptoraProperties.tuning().scoreThresholds().breakout();
        int strong = cryptoraProperties.tuning().scoreThresholds().strong();
        int strongPrev = cryptoraProperties.tuning().scoreThresholds().strongPrev();
        int buy = cryptoraProperties.tuning().scoreThresholds().buy();
        int buyPrev = cryptoraProperties.tuning().scoreThresholds().buyPrev();

        if (("CONSOLIDATION".equals(marketState) || "RANGE".equals(marketState))
                && "WEAK".equals(trendStrength)) {
            if (Math.abs(scoreNow) < 3 || liquidityLow) {
                return "HOLD";
            }
        }

        if ("BREAKOUT_ATTEMPT".equals(marketState)) {
            if (scoreNow >= breakoutThreshold && volumeOk && liquidityHighOrNormal) return "BUY";
            if (scoreNow <= -breakoutThreshold && volumeOk && liquidityHighOrNormal) return "SELL";
            return "HOLD";
        }

        if (scoreNow >= strong && scorePrev >= strongPrev && volumeOk && liquidityHighOrNormal) return "STRONG_BUY";
        if (scoreNow <= -strong && scorePrev <= -strongPrev && volumeOk && liquidityHighOrNormal) return "STRONG_SELL";

        if (scoreNow >= buy) {
            if (scorePrev >= buyPrev) {
                if (liquidityLow && !volumeOk) return "HOLD";
                return "BUY";
            }
            if (volumeOk && liquidityHighOrNormal) return "BUY";
        }
        if (scoreNow <= -buy) {
            if (scorePrev <= -buyPrev) {
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
        double signalPct = cryptoraProperties.tuning().sma().diff().signalPct();

        if (smaDiffPercent > signalPct) score += 2;
        else if (smaDiffPercent < -signalPct) score -= 2;

        if (rsi.isLessThan(DecimalNum.valueOf(cryptoraProperties.rsi().oversold()))) score += 2;

        if (rsi.isGreaterThan(DecimalNum.valueOf(cryptoraProperties.rsi().overbought()))) score -= 2;
        else {
            double span = cryptoraProperties.rsi().overbought() - cryptoraProperties.rsi().oversold();
            double midLow = cryptoraProperties.rsi().oversold() + span * 0.333;
            double midHigh = cryptoraProperties.rsi().overbought() - span * 0.333;
            if (rsi.doubleValue() > midLow && rsi.doubleValue() < midHigh) {
                score -= 1;
            }
        }

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
        int bars = Math.min(cryptoraProperties.tuning().volume().windowLookback(), series.getBarCount());
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
        int lookback = Math.min(cryptoraProperties.tuning().marketState().lookbackBars(), series.getBarCount());
        Num high = series.getBar(series.getBarCount() - lookback).getHighPrice();
        Num low = series.getBar(series.getBarCount() - lookback).getLowPrice();

        for (int i = series.getBarCount() - lookback + 1; i < series.getBarCount(); i++) {
            high = high.max(series.getBar(i).getHighPrice());
            low = low.min(series.getBar(i).getLowPrice());
        }

        Num range = high.minus(low);
        Num compression = price.multipliedBy(DecimalNum.valueOf(cryptoraProperties.tuning().marketState().compressionPct()));

        boolean compressed = range.isLessThan(compression);

        Num eps = price.multipliedBy(DecimalNum.valueOf(cryptoraProperties.tuning().marketState().breakoutEpsPct()));
        boolean nearBreakout =
                price.isGreaterThan(thrUp.minus(eps)) ||
                        price.isLessThan(thrLo.plus(eps));

        if (nearBreakout && "HIGH".equals(liquidity)) return "BREAKOUT_ATTEMPT";
        if (compressed && "LOW".equals(volatility)) return "CONSOLIDATION";

        double smaDiffPercent = calculateSMADiffPercent(shortSMA, longSMA);
        if (Math.abs(smaDiffPercent) > cryptoraProperties.tuning().sma().diff().trendingPct()) return "TRENDING";

        return "RANGE";
    }

    private String calculateVolatility(Num atr, Num price) {
        log.trace("AnalysisService | Calculating volatility");
        double p = atr.dividedBy(price).multipliedBy(DecimalNum.valueOf(100)).doubleValue();
        if (p < cryptoraProperties.tuning().volatility().pct().low()) return "LOW";
        if (p < cryptoraProperties.tuning().volatility().pct().medium()) return "MEDIUM";
        return "HIGH";
    }

    private String calculateTrendStrength(Num shortSMA, Num longSMA, Num rsi) {
        log.trace("AnalysisService | Calculating trend strength");
        if (longSMA == null || longSMA.isZero()) return "WEAK";

        double diff = Math.abs(calculateSMADiffPercent(shortSMA, longSMA));
        boolean strongRsi = rsi.doubleValue() < cryptoraProperties.rsi().oversold() || rsi.doubleValue() > cryptoraProperties.rsi().overbought();

        if (diff > cryptoraProperties.tuning().sma().diff().strongPct() && strongRsi) return "STRONG";
        if (diff > cryptoraProperties.tuning().sma().diff().moderatePct()) return "MODERATE";
        return "WEAK";
    }

    private String calculateLiquidity(BarSeries series) {
        log.trace("AnalysisService | Calculating liquidity");
        int barCount = series.getBarCount();
        if (barCount < 2) return "LOW";

        int windowLookback = Math.min(cryptoraProperties.tuning().volume().windowLookback(), barCount);

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

        double localRatio = curr / Math.max(windowAvg, cryptoraProperties.tuning().thresholds().minSafeValue());
        double globalRatio = curr / Math.max(globalAvg, cryptoraProperties.tuning().thresholds().minSafeValue());

        boolean localLow = localRatio < cryptoraProperties.tuning().liquidity().localLow();
        boolean globalLow = globalRatio < cryptoraProperties.tuning().liquidity().globalLow();
        if (localLow && globalLow)
            return "LOW";
        if (localRatio > cryptoraProperties.tuning().liquidity().localHigh() && globalRatio > cryptoraProperties.tuning().liquidity().globalHigh())
            return "HIGH";
        if (localLow || globalLow)
            return "LOW";

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
        int score = cryptoraProperties.tuning().confidence().base();

        double rv = rsi.doubleValue();
        double smaDiff = calculateSMADiffPercent(shortSMA, longSMA);

        if (rv < cryptoraProperties.rsi().oversold() || rv > cryptoraProperties.rsi().overbought()) {
            score += cryptoraProperties.tuning().confidence().rsiExtreme();
            if ("LOW".equals(liquidity)) score += cryptoraProperties.tuning().confidence().liquidityLowPenalty();
        } else {
            double span = cryptoraProperties.rsi().overbought() - cryptoraProperties.rsi().oversold();
            double midLow = cryptoraProperties.rsi().oversold() + span * 0.333;
            double midHigh = cryptoraProperties.rsi().overbought() - span * 0.333;
            if (rv > midLow && rv < midHigh) {
                score += cryptoraProperties.tuning().confidence().rsiMiddlePenalty();
            }
        }

        if ("STRONG".equals(trendStrength)) score += cryptoraProperties.tuning().confidence().trendStrong();
        else if ("WEAK".equals(trendStrength)) score += cryptoraProperties.tuning().confidence().trendWeak();

        if ("LOW".equals(volatility)) score += cryptoraProperties.tuning().confidence().volatilityLowBonus();
        if ("HIGH".equals(liquidity)) score += cryptoraProperties.tuning().confidence().liquidityHighBonus();
        else if ("LOW".equals(liquidity)) score += cryptoraProperties.tuning().confidence().liquidityLowPenalty();

        if (smaDiff > 0 && rv > 50) score += cryptoraProperties.tuning().confidence().smaRsiAgreementBonus();
        if (smaDiff < 0 && rv < 50) score += cryptoraProperties.tuning().confidence().smaRsiAgreementBonus();

        double signalPct = cryptoraProperties.tuning().sma().diff().signalPct();
        int smaRsiConflictPenalty = cryptoraProperties.tuning().confidence().smaRsiConflictPenalty();
        if (smaDiff > signalPct &&
                rv < cryptoraProperties.rsi().oversold()) score += smaRsiConflictPenalty;
        if (smaDiff < -signalPct &&
                rv > cryptoraProperties.rsi().overbought()) score += smaRsiConflictPenalty;

        if ("CONSOLIDATION".equals(marketState) || "RANGE".equals(marketState)) {
            score -= 10;
        }

        return Math.min(100, score);
    }

    private Num[] calculateThresholds(Num base, Num atr, double atrMultiplier) {
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
                .withMaxBarCount(cryptoraProperties.tuning().series().maxBars())
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
