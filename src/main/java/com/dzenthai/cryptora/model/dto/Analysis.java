package com.dzenthai.cryptora.model.dto;

import com.dzenthai.cryptora.model.enums.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record Analysis(
        @JsonProperty("symbol")
        String symbol,
        @JsonProperty("action")
        Action action,
        @JsonProperty("market_state")
        MarketState marketState,
        @JsonProperty("volatility")
        Volatility volatility,
        @JsonProperty("trend_strength")
        TrendStrength trendStrength,
        @JsonProperty("liquidity")
        Liquidity liquidity,
        @JsonProperty("risk_level")
        RiskLevel riskLevel,
        @JsonProperty("confidence_score")
        int confidenceScore,
        @JsonProperty("indicators")
        Indicator details
) {
}
