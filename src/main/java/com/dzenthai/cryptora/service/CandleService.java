package com.dzenthai.cryptora.service;

import com.binance.connector.client.spot.rest.model.KlinesItem;
import com.binance.connector.client.spot.rest.model.KlinesResponse;
import com.dzenthai.cryptora.model.entity.Candle;
import com.dzenthai.cryptora.repository.CandleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;


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
        log.debug("CandleService | Saving candle for symbol: {}, kline response: {}", symbol, klinesResponse);
        if (klinesResponse == null || klinesResponse.isEmpty()) return;

        String saveSymbol = symbol.endsWith("USDT") ? symbol : symbol + "USDT";
        int count = 0;

        for (KlinesItem item : klinesResponse) {
            long closeTime = Long.parseLong(item.get(6));
            Instant closeInstant = Instant.ofEpochMilli(closeTime);

            if (!candleRepository.existsBySymbolAndCloseTime(saveSymbol, closeInstant)) {
                Candle candle = buildCandle(saveSymbol, item);
                candleRepository.save(candle);
                count++;
            }
        }
        if (count > 0) {
            log.debug("CandleService | Saved {} new bars for {}", count, saveSymbol);
        }
    }

    public List<Candle> getCandleBySymbol(String symbol) {
        String searchSymbol = symbol.endsWith("USDT") ? symbol : symbol + "USDT";
        log.debug("CandleService | Receiving candle for: {}", searchSymbol);
        return candleRepository.findBySymbolIgnoreCase(searchSymbol);
    }

    private Candle buildCandle(String symbol, KlinesItem kline) {
        log.debug("CandleService | Parsing and converting kline (candle) json for {} into object", symbol);
        long openTime = Long.parseLong(kline.get(0));
        long closeTime = Long.parseLong(kline.get(6));

        Instant beginTime = Instant.ofEpochMilli(openTime);
        Instant endTime = Instant.ofEpochMilli(closeTime);

        return Candle.builder()
                .symbol(symbol)
                .openTime(beginTime)
                .closeTime(endTime)
                .openPrice(new BigDecimal(kline.get(1)))
                .highPrice(new BigDecimal(kline.get(2)))
                .lowPrice(new BigDecimal(kline.get(3)))
                .closePrice(new BigDecimal(kline.get(4)))
                .volume(new BigDecimal(kline.get(5)))
                .amount(new BigDecimal(kline.get(7)))
                .trades(Long.parseLong(kline.get(8)))
                .timePeriod(Duration.between(beginTime, endTime))
                .build();
    }
}
