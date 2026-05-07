package com.dzenthai.cryptora.repository;

import com.dzenthai.cryptora.model.entity.Candle;

import java.util.List;


public interface CandleRepository {

    List<Candle> findAll();

    List<Candle> findBySymbolIgnoreCase(String symbol);

    void saveAll(List<Candle> candles);
}

