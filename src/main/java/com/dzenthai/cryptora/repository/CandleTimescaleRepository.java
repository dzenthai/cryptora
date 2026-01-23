package com.dzenthai.cryptora.repository;

import com.dzenthai.cryptora.model.entity.Candle;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;


@Repository
public class CandleTimescaleRepository implements CandleRepository {

    private final JdbcTemplate jdbc;

    public CandleTimescaleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Candle> rowMapper = (rs, rowNum) ->
            Candle.builder()
                    .symbol(rs.getString("symbol"))
                    .openTime(rs.getTimestamp("open_time") != null
                            ? rs.getTimestamp("open_time").toInstant()
                            : null)
                    .closeTime(rs.getTimestamp("close_time") != null
                            ? rs.getTimestamp("close_time").toInstant()
                            : null)
                    .openPrice(rs.getDouble("open_price"))
                    .closePrice(rs.getDouble("close_price"))
                    .highPrice(rs.getDouble("high_price"))
                    .lowPrice(rs.getDouble("low_price"))
                    .volume(rs.getDouble("volume"))
                    .amount(rs.getDouble("amount"))
                    .trades(rs.getLong("trades"))
                    .build();

    @Override
    public List<Candle> findAll() {
        var sql = """
                SELECT * FROM public.candles
                ORDER BY close_time
                """;
        return jdbc.query(sql, rowMapper);
    }

    @Override
    public List<Candle> findBySymbolIgnoreCase(String symbol) {
        var sql = """
                SELECT * FROM public.candles
                WHERE lower(symbol) = lower(?)
                ORDER BY close_time
                """;
        return jdbc.query(sql, rowMapper, symbol);
    }

    @Override
    public void saveAll(List<Candle> candles) {
        var sql = """
                INSERT INTO public.candles
                (symbol, open_price, close_price, high_price, low_price, volume, amount, trades, open_time, close_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (symbol, close_time) DO NOTHING
                """;
        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(@NotNull PreparedStatement ps, int i) throws SQLException {
                Candle c = candles.get(i);
                ps.setString(1, c.getSymbol());
                ps.setDouble(2, c.getOpenPrice());
                ps.setDouble(3, c.getClosePrice());
                ps.setDouble(4, c.getHighPrice());
                ps.setDouble(5, c.getLowPrice());
                ps.setDouble(6, c.getVolume());
                ps.setDouble(7, c.getAmount());
                ps.setLong(8, c.getTrades());
                ps.setTimestamp(9, Timestamp.from(c.getOpenTime()));
                ps.setTimestamp(10, Timestamp.from(c.getCloseTime()));
            }

            @Override
            public int getBatchSize() {
                return candles.size();
            }
        });
    }
}
