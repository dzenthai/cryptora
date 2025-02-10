package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record Statistic(
        @JsonProperty("ticker")
        String ticker,
        @JsonProperty("average")
        Average average,
        @JsonProperty("total")
        Total total,
        @JsonProperty("additional_information")
        Info info
)
{}
