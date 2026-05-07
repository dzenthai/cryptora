package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Duration;
import java.time.Instant;


@Builder
public record Info(
        @JsonProperty("entries_count")
        int entriesCount,
        @JsonProperty("beginTime")
        Instant beginTime,
        @JsonProperty("endTime")
        Instant endTime,
        @JsonProperty("duration")
        Duration duration
) {
}
