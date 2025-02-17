package com.dzenthai.cryptora.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;


@Builder
public record Info(
        @JsonProperty("quote_entries_count")
        Integer quoteEntriesCount,
        @JsonProperty("current_date_time")
        Instant currentDateTime,
        @JsonProperty("initial_date_time")
        Instant initDateTime
)
{}
