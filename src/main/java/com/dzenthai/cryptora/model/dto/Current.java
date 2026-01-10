package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;


@Builder
public record Current(
        @JsonProperty("open_price")
        BigDecimal openPrice,
        @JsonProperty("close_price")
        BigDecimal closePrice,
        @JsonProperty("high_price")
        BigDecimal highPrice,
        @JsonProperty("low_price")
        BigDecimal lowPrice,
        @JsonProperty("volume")
        BigDecimal volume,
        @JsonProperty("amount")
        BigDecimal amount,
        @JsonProperty("open_time")
        Instant openTime,
        @JsonProperty("close_time")
        Instant closeTime,
        @JsonProperty("trades")
        Long trades
) {
}
