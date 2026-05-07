package com.dzenthai.cryptora.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;


@Data
@Builder
public class Candle {

    private String symbol;

    private double openPrice;

    private double closePrice;

    private double highPrice;

    private double lowPrice;

    private double volume;

    private double amount;

    private long trades;

    private Instant openTime;

    private Instant closeTime;
}
