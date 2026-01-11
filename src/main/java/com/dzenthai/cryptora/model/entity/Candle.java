package com.dzenthai.cryptora.model.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;


@Data
@Builder
@Document(collection = "candles")
public class Candle {

    @Id
    private String id;

    @Indexed
    @Field(name = "symbol")
    private String symbol;

    @Field(name = "open_price")
    private BigDecimal openPrice;

    @Field(name = "high_price")
    private BigDecimal highPrice;

    @Field(name = "low_price")
    private BigDecimal lowPrice;

    @Field(name = "close_price")
    private BigDecimal closePrice;

    @Field(name = "volume")
    private BigDecimal volume;

    @Field(name = "amount")
    private BigDecimal amount;

    @Field(name = "trades")
    private Long trades;

    @Field(name = "openTime")
    private Instant openTime;

    @Field(name = "closeTime")
    private Instant closeTime;

    @Field(name = "timePeriod")
    private Duration timePeriod;
}
