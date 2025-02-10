package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;


@Builder
public record Average(
        @JsonProperty("open_price")
        BigDecimal openPrice,
        @JsonProperty("close_price")
        BigDecimal closePrice,
        @JsonProperty("high_price")
        BigDecimal highPrice,
        @JsonProperty("low_price")
        BigDecimal lowPrice,
        @JsonProperty("trade_price")
        BigDecimal tradePrice,
        @JsonProperty("price_range")
        BigDecimal priceRange
)
{}
