package com.dzenthai.cryptora.service;

import com.binance.connector.client.spot.rest.model.KlinesItem;
import com.binance.connector.client.spot.rest.model.KlinesResponse;
import com.dzenthai.cryptora.model.entity.Candle;
import com.dzenthai.cryptora.repository.CandleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Slf4j
@Service
public class CandleService {

    private final CandleRepository candleRepository;

    public CandleService(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    public List<Candle> getAllCandles() {
        log.debug("CandleService | Receiving all candles");
        return candleRepository.findAll();
    }

    public void saveAllCandles(String symbol, KlinesResponse klinesResponse) {
        if (klinesResponse == null || klinesResponse.isEmpty()) return;

        String saveSymbol = (symbol.endsWith("USDT") ? symbol : symbol + "USDT").toUpperCase(Locale.ROOT);

        List<Candle> toInsert = new ArrayList<>(klinesResponse.size());
        for (KlinesItem item : klinesResponse) {
            Candle candle = buildCandle(saveSymbol, item);
            toInsert.add(candle);
        }
        candleRepository.saveAll(toInsert);

        log.debug("CandleService | Attempted to insert {} bars for {}", toInsert.size(), saveSymbol);
    }

    public List<Candle> getCandleBySymbol(String symbol) {
        String searchSymbol = (symbol.endsWith("USDT") ? symbol : symbol + "USDT")
                .toUpperCase(Locale.ROOT);
        log.debug("CandleService | Receiving candle for: {}", searchSymbol);
        return candleRepository.findBySymbolIgnoreCase(searchSymbol);
    }

    private Candle buildCandle(String symbol, KlinesItem kline) {
        var savingSymbol = symbol.toUpperCase(Locale.ROOT);
        log.debug("CandleService | Parsing and converting kline (candle) json for {} into object", savingSymbol);
        long openTime = Long.parseLong(kline.get(0));
        long closeTime = Long.parseLong(kline.get(6));

        Instant beginTime = Instant.ofEpochMilli(openTime);
        Instant endTime = Instant.ofEpochMilli(closeTime);

        return Candle.builder()
                .symbol(savingSymbol)
                .openTime(beginTime)
                .closeTime(endTime)
                .openPrice(Double.parseDouble(kline.get(1)))
                .closePrice(Double.parseDouble(kline.get(4)))
                .highPrice(Double.parseDouble(kline.get(2)))
                .lowPrice(Double.parseDouble(kline.get(3)))
                .volume(Double.parseDouble(kline.get(5)))
                .amount(Double.parseDouble(kline.get(7)))
                .trades(Long.parseLong(kline.get(8)))
                .build();
    }
}
