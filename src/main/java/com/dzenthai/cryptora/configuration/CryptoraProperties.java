package com.dzenthai.cryptora.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "cryptora")
public record CryptoraProperties(
        ShortTime shortTime,
        LongTime longTime,
        Rsi rsi,
        Atr atr,
        Volume volume,
        Tuning tuning
) {

    public record ShortTime(int period) {}
    public record LongTime(int period) {}

    public record Rsi(
            int period,
            int overbought,
            int oversold
    ) {}

    public record Atr(
            int period,
            Multiplier multiplier
    ) {
        public record Multiplier(
                double strong,
                double weak
        ) {}
    }

    public record Volume(int period) {}

    public record Tuning(
            Thresholds thresholds,
            Series series,
            MarketState marketState,
            Sma sma,
            ScoreThresholds scoreThresholds,
            Liquidity liquidity,
            VolumeTuning volume,
            Volatility volatility,
            Confidence confidence
    ) {
        public record Thresholds(double minSafeValue) {}
        public record Series(int maxBars) {}
        public record MarketState(
                int lookbackBars,
                double compressionPct,
                double breakoutEpsPct
        ) {}
        public record Sma(
                Diff diff
        ) {
            public record Diff(
                    double trendingPct,
                    double moderatePct,
                    double strongPct,
                    double signalPct
            ) {}
        }
        public record ScoreThresholds(
                int strong,
                int strongPrev,
                int buy,
                int buyPrev,
                int breakout
        ) {}
        public record Liquidity(
                double localHigh,
                double globalHigh,
                double localLow,
                double globalLow
        ) {}
        public record VolumeTuning(
                double minRelativeToAvg,
                int windowLookback
        ) {}
        public record Volatility(
                Pct pct
        ) {
            public record Pct(
                    double low,
                    double medium
            ) {}
        }
        public record Confidence(
                int base,
                int rsiExtreme,
                int rsiMiddlePenalty,
                int trendStrong,
                int trendWeak,
                int volatilityLowBonus,
                int liquidityHighBonus,
                int liquidityLowPenalty,
                int smaRsiAgreementBonus,
                int smaRsiConflictPenalty
        ) {}
    }
}
