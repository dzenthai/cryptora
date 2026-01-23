CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE public.candles
(
    symbol      TEXT        NOT NULL,
    close_time  TIMESTAMPTZ NOT NULL,
    open_time   TIMESTAMPTZ NOT NULL,
    open_price  DOUBLE PRECISION,
    close_price DOUBLE PRECISION,
    high_price  DOUBLE PRECISION,
    low_price   DOUBLE PRECISION,
    volume      DOUBLE PRECISION,
    amount      DOUBLE PRECISION,
    trades      BIGINT,
    PRIMARY KEY (symbol, close_time)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_candles_symbol_close_time
    ON public.candles (symbol, close_time);

SELECT create_hypertable(
               'public.candles',
               'close_time',
               chunk_time_interval => INTERVAL '1 day'
       );

CREATE INDEX ON public.candles (symbol, close_time DESC);

ALTER TABLE public.candles SET (timescaledb.compress = true);

SELECT add_compression_policy('public.candles', INTERVAL '30 days');
SELECT add_retention_policy('public.candles', INTERVAL '90 days');