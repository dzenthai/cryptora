package com.dzenthai.cryptora.service;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.binance.api.client.domain.market.TickerPrice;
import com.binance.api.client.exception.BinanceApiException;
import com.dzenthai.cryptora.model.enums.Ticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class FetchService {

    private final QuoteService quoteService;

    private final BinanceApiRestClient binanceApiRestClient;

    public FetchService(
            QuoteService quoteService,
            BinanceApiRestClient binanceApiRestClient
    ) {
        this.quoteService = quoteService;
        this.binanceApiRestClient = binanceApiRestClient;
    }

    public void fetchNewQuotes() {
        log.info("FetchService | Fetching new quotes");
        List<String> symbols = Ticker.getAllSymbols();
        symbols.forEach(symbol -> {
            log.debug("FetchService | Processing symbol: {}", symbol);
            try {
                TickerPrice tickerPrice = fetchTickerPrice(symbol);
                List<Candlestick> candlesticks = fetchCandlestickBars(symbol);

                var quote = quoteService.addNewQuote(tickerPrice, candlesticks);
                log.debug("FetchService | Quote successfully saved, quote: {}", quote);

            } catch (BinanceApiException e) {
                log.error("FetchService | Binance API error while fetching, symbol: {}, exception: ", symbol, e);
            } catch (Exception e) {
                log.error("FetchService | Unexpected error while fetching, symbol: {}, exception: ", symbol, e);
            }
        });
    }

    private TickerPrice fetchTickerPrice(String symbol) {
        TickerPrice tickerPrice = binanceApiRestClient.getPrice(symbol);
        if (tickerPrice == null) {
            log.warn("FetchService | Ticker Price is null for symbol: {}", symbol);
        }
        return tickerPrice;
    }

    private List<Candlestick> fetchCandlestickBars(String symbol) {
        List<Candlestick> candlesticks = binanceApiRestClient.getCandlestickBars(symbol, CandlestickInterval.ONE_MINUTE);
        if (candlesticks == null || candlesticks.isEmpty()) {
            log.warn("FetchService | Candlesticks are empty for symbol: {}", symbol);
        }
        return candlesticks;
    }
}
