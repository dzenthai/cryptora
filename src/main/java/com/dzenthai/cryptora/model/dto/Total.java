package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record Total(
        @JsonProperty("volume")
        BigDecimal volume,
        @JsonProperty("amount")
        BigDecimal amount
)
{}
