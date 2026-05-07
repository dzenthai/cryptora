package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record Statistic(
        @JsonProperty("analysis")
        Analysis analysis,
        @JsonProperty("current")
        Current current,
        @JsonProperty("average")
        Average average,
        @JsonProperty("max_values")
        Max max,
        @JsonProperty("min_values")
        Min min,
        @JsonProperty("total")
        Total total,
        @JsonProperty("additional_information")
        Info info
)
{}
