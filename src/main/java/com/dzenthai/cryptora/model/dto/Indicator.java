package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record Indicator(
        @JsonProperty("price")
        double price,
        @JsonProperty("sma_short")
        double smaShort,
        @JsonProperty("sma_long")
        double smaLong,
        @JsonProperty("sma_diff")
        double smaDiff,
        @JsonProperty("rsi")
        double rsi,
        @JsonProperty("atr")
        double atr,
        @JsonProperty("atr_percent")
        double atrPercent,
        @JsonProperty("upper_threshold")
        double upperThreshold,
        @JsonProperty("lower_threshold")
        double lowerThreshold,
        @JsonProperty("current_volume")
        double currentVolume,
        @JsonProperty("average_volume")
        double averageVolume,
        @JsonProperty("volume_ok")
        boolean volumeOk
) {
}
