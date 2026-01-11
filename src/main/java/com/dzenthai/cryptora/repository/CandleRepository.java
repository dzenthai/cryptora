package com.dzenthai.cryptora.repository;

import com.dzenthai.cryptora.model.entity.Candle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;


@Repository
public interface CandleRepository extends MongoRepository<Candle, String> {

    List<Candle> findBySymbolIgnoreCase(String symbol);

    boolean existsBySymbolAndCloseTime(String symbol, Instant closeTime);
}
