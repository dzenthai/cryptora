package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;


@Builder
public record Analysis(
        @JsonProperty("ticker")
        String ticker,
        @JsonProperty("action")
        String action
)
{}
