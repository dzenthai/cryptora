package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record Average(
        @JsonProperty("open_price")
        double openPrice,
        @JsonProperty("close_price")
        double closePrice,
        @JsonProperty("high_price")
        double highPrice,
        @JsonProperty("low_price")
        double lowPrice,
        @JsonProperty("trade_price")
        double tradePrice,
        @JsonProperty("price_range")
        double priceRange
)
{}
