package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;


@Builder
public record Indicator(
        @JsonProperty("price")
        BigDecimal price,
        @JsonProperty("sma_short")
        BigDecimal smaShort,
        @JsonProperty("sma_long")
        BigDecimal smaLong,
        @JsonProperty("sma_diff")
        BigDecimal smaDiff,
        @JsonProperty("rsi")
        BigDecimal rsi,
        @JsonProperty("atr")
        BigDecimal atr,
        @JsonProperty("atr_percent")
        BigDecimal atrPercent,
        @JsonProperty("upper_threshold")
        BigDecimal upperThreshold,
        @JsonProperty("lower_threshold")
        BigDecimal lowerThreshold,
        @JsonProperty("current_volume")
        BigDecimal currentVolume,
        @JsonProperty("average_volume")
        BigDecimal averageVolume,
        @JsonProperty("volume_ok")
        boolean volumeOk
) {
}
