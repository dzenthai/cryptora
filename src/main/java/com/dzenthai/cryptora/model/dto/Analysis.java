package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record Analysis(
        @JsonProperty("symbol")
        String symbol,
        @JsonProperty("action")
        String action,
        @JsonProperty("market_state")
        String marketState,
        @JsonProperty("volatility")
        String volatility,
        @JsonProperty("trend_strength")
        String trendStrength,
        @JsonProperty("liquidity")
        String liquidity,
        @JsonProperty("risk_level")
        String riskLevel,
        @JsonProperty("confidence_score")
        Integer confidenceScore,
        @JsonProperty("details")
        Details details
) {
}
