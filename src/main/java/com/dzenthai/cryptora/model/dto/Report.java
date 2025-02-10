package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record Report(
        @JsonProperty("statistic")
        Statistic statistic,
        @JsonProperty("recommendation")
        String recommendation
)
{}
