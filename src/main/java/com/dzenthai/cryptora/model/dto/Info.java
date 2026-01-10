package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Duration;
import java.time.Instant;


@Builder
public record Info(
        @JsonProperty("quote_entries_count")
        Integer quoteEntriesCount,
        @JsonProperty("begin_time")
        Instant beginTime,
        @JsonProperty("end_time")
        Instant endTime,
        @JsonProperty("interval")
        Duration interval
) {
}
