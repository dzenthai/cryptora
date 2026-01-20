package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record Min(
        @JsonProperty("open_price")
        double openPrice,
        @JsonProperty("close_price")
        double closePrice,
        @JsonProperty("high_price")
        double highPrice,
        @JsonProperty("low_price")
        double lowPrice,
        @JsonProperty("price_range")
        double priceRange,
        @JsonProperty("volume")
        double volume,
        @JsonProperty("amount")
        double amount
) {
}
