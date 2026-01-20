package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;


@Builder
public record Current(
        @JsonProperty("open_price")
        double openPrice,
        @JsonProperty("close_price")
        double closePrice,
        @JsonProperty("high_price")
        double highPrice,
        @JsonProperty("low_price")
        double lowPrice,
        @JsonProperty("volume")
        double volume,
        @JsonProperty("amount")
        double amount,
        @JsonProperty("open_time")
        Instant openTime,
        @JsonProperty("close_time")
        Instant closeTime,
        @JsonProperty("trades")
        long trades
) {
}
