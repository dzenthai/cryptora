package com.dzenthai.cryptora.service;

import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.spot.rest.api.SpotRestApi;
import com.binance.connector.client.spot.rest.model.Interval;
import com.binance.connector.client.spot.rest.model.KlinesResponse;
import com.dzenthai.cryptora.model.enums.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class FetchService {

    private final QuoteService quoteService;

    private final SpotRestApi spotRestApi;

    public FetchService(
            QuoteService quoteService,
            SpotRestApi spotRestApi
            ) {
        this.quoteService = quoteService;
        this.spotRestApi = spotRestApi;
    }

    public void fetchNewQuotes() {
        log.info("FetchService | Fetching new quotes");
        List<String> symbols = Ticker.getAllSymbols();

        symbols.forEach(symbol -> {
            log.debug("FetchService | Processing symbol: {}", symbol);
            try {

                ApiResponse<KlinesResponse> klinesResponse = spotRestApi.klines(
                        symbol,
                        Interval.INTERVAL_1m,
                        null,
                        null,
                        "+0",
                        500
                );

                KlinesResponse klines = klinesResponse.getData();
                quoteService.addAllQuotes(symbol, klines);
                log.debug("FetchService | Quote successfully saved");

            } catch (Exception e) {
                log.error("FetchService | Error while fetching symbol: {}", symbol, e);
            }
        });
    }
}
