![Java Version](https://img.shields.io/badge/Java-v21-red)
![Repository Size](https://img.shields.io/github/repo-size/dzenthai/cryptora?color=red)

<div style="display: flex; flex-wrap: wrap; justify-content: center;">
    <img src="assets/binance-logo.png" style="width: 65px; height: 65px;" alt="">
</div>

## **Description**

**Cryptora** is a Spring Boot–based service for real-time technical analysis of cryptocurrency markets using n-minute
candlestick data from the Binance API. It combines SMA, RSI, and ATR indicators to assess current market conditions
and generate interpretable, context-aware trading signals focused on describing the present market state rather than
predicting future price movements.

---

## **Disclaimer**

**This service does not guarantee a 100% increase or decrease in cryptocurrency values.**
The service was developed solely by the author for educational purposes and should not be considered as financial
advice. The creator of this service is not responsible for any financial losses or damages incurred while using it.
Cryptocurrencies are highly volatile and carry inherent risks. Users should conduct their own research and consult with
a qualified financial advisor before making any investment decisions. By using this service, you acknowledge and accept
these risks.

---

## **Key Features**

- **Real-time Data Fetching**: Integrates with Binance API to retrieve live n-minute candlestick data for multiple
  cryptocurrencies with automatic deduplication.

- **Historical Data Storage**: TimescaleDB stores complete price history.

- **Advanced Technical Analysis**: Utilizes Ta4j library for well-established technical indicators provided by Ta4j:
    - **Moving Averages**: Configurable SMA periods for trend detection and crossover signals
    - **RSI (Relative Strength Index)**: Momentum oscillator with customizable overbought/oversold thresholds
    - **ATR (Average True Range)**: Volatility measurement with adaptive multipliers for dynamic threshold calculation

- **Multi-Dimensional Market Assessment**:
    - **Trading Signals**: STRONG_BUY, BUY, SELL, STRONG_SELL, HOLD recommendations based on technical indicator
      confluence
    - **Market State**: TRENDING, BREAKOUT_ATTEMPT, CONSOLIDATION, RANGE classification
    - **Volatility Analysis**: LOW, MEDIUM, HIGH volatility categorization
    - **Trend Strength**: WEAK, MODERATE, STRONG trend evaluation
    - **Liquidity Assessment**: Volume-based liquidity scoring
    - **Risk Level**: Composite risk calculation (LOW, MEDIUM, HIGH)
    - **Confidence Score**: 0-100% confidence rating for each signal. Confidence Score is a heuristic metric derived
      from indicator alignment, not a probabilistic forecast.

- **Statistical Reports**: Detailed interval-based reports with current, average, min, max, and total metrics for
  comprehensive market overview.

- **Automated Scheduling**: Configurable scheduler with retry mechanism ensures continuous data collection and analysis
  every n seconds.

---

## **Technologies**

- **Java**: The primary programming language.

- **Spring Boot**: Framework used for building the service.

- **TimescaleDB**: A SQL database (built on PostgreSQL) optimized for storing real-time cryptocurrency time-series data.

- **Docker**: Containerization platform that helps package the application with its dependencies, ensuring consistent
  environments and simplifying deployment.

---

## **Dependencies**

- **Spring Boot Starter Web**: RESTful web services framework with embedded Tomcat server for API endpoints

- **Ta4j 0.22.0**: Professional technical analysis library providing battle-tested indicators (SMA, RSI, ATR) and
  strategy framework

- **Binance Spot Connector 2.0.0**: Official Binance API client for accessing real-time market data and candlestick
  information

- **Gson**: Google's JSON serialization/deserialization library for parsing API responses

- **Lombok**: Annotation processor reducing boilerplate code with @Data, @Builder, @Slf4j annotations

---

## **How it Works**

### **Technical Analysis Engine**

The application continuously fetches 1-minute candlestick data from Binance and applies multiple technical indicators to
generate trading signals. The analysis considers:

1. **Short-term vs Long-term Moving Average Crossovers**: Identifies trend changes when faster MA crosses slower MA
2. **RSI Momentum**: Filters signals based on overbought and oversold conditions
3. **ATR-based Thresholds**: Dynamic support/resistance levels adapted to current market volatility
4. **Volume Confirmation**: Requires above-average volume to validate breakout signals

```shell
2026-01-19T03:28:07.918Z  INFO 1 --- [cryptora] [   scheduling-1] c.d.cryptora.service.AnalysisService     : AnalysisService | Receiving analysis via logs

2026-01-19T03:28:07.956Z  INFO 1 --- [cryptora] [   scheduling-1] c.d.cryptora.service.AnalysisService     : IndicatorMapper | Symbol: ETHUSDT, Price: 3204.91000000, SMA9: 3204.03888889, SMA21: 3206.85476190, SMA Diff%: -0.08780794, RSI: 47.45370010, ATR: 2.06935262, ATR%: 0.06456800, Upper Threshold: 3210.99346714, Lower Threshold: 3202.71605667, Vol: 21.70080000/236.73354500, Volume Ok: false
2026-01-19T03:28:07.957Z  INFO 1 --- [cryptora] [   scheduling-1] c.d.cryptora.service.AnalysisService     : AnalysisMapper  | Symbol: ETHUSDT, Action: HOLD, Market: RANGE, Volatility: LOW, Trend: WEAK, Liquidity: LOW, Risk: MEDIUM, Confidence: 30%

2026-01-19T03:28:07.959Z  INFO 1 --- [cryptora] [   scheduling-1] c.d.cryptora.service.AnalysisService     : IndicatorMapper | Symbol: BTCUSDT, Price: 92573.55000000, SMA9: 92607.57444444, SMA21: 92670.42714286, SMA Diff%: -0.06782390, RSI: 42.56293722, ATR: 31.50832251, ATR%: 0.03403600, Upper Threshold: 92733.44378789, Lower Threshold: 92607.41049783, Vol: 0.32690000/5.85294000, Volume Ok: false
2026-01-19T03:28:07.959Z  INFO 1 --- [cryptora] [   scheduling-1] c.d.cryptora.service.AnalysisService     : AnalysisMapper  | Symbol: BTCUSDT, Action: HOLD, Market: CONSOLIDATION, Volatility: LOW, Trend: WEAK, Liquidity: LOW, Risk: MEDIUM, Confidence: 30%

2026-01-19T03:28:07.961Z  INFO 1 --- [cryptora] [   scheduling-1] c.d.cryptora.service.AnalysisService     : IndicatorMapper | Symbol: TONUSDT, Price: 1.62300000, SMA9: 1.62344444, SMA21: 1.62423810, SMA Diff%: -0.04886296, RSI: 47.60042545, ATR: 0.00116970, ATR%: 0.07207000, Upper Threshold: 1.62657750, Lower Threshold: 1.62189869, Vol: 0E-8/18069.54450000, Volume Ok: false
2026-01-19T03:28:07.961Z  INFO 1 --- [cryptora] [   scheduling-1] c.d.cryptora.service.AnalysisService     : AnalysisMapper  | Symbol: TONUSDT, Action: HOLD, Market: RANGE, Volatility: LOW, Trend: WEAK, Liquidity: LOW, Risk: MEDIUM, Confidence: 30%
```

In the example above:

The analysis evaluates BTC, ETH, and TON in real-time, displaying:

- **Action**: Trading signal (BUY/STRONG_BUY/SELL/STRONG_SELL/HOLD)
- **Market State**: Current market phase
- **Volatility**: Price volatility level
- **Trend Strength**: Trend momentum assessment
- **Liquidity**: Current trading volume status
- **Risk Level**: Composite risk evaluation
- **Confidence Score**: Signal reliability percentage

### **Statistical Reports API**

Access comprehensive market reports via REST endpoint:

```
GET http://localhost:8088/api/v1/report/asset=btc&duration=6h
```

**Parameters**:

- `asset`: Cryptocurrency symbol (btc, eth, ton)
- `duration`: Lookback window (e.g., 1h, 6h, 1d, 7d)

```json
{
  "statistic": {
    "analysis": {
      "symbol": "BTCUSDT",
      "action": "HOLD",
      "market_state": "CONSOLIDATION",
      "volatility": "LOW",
      "trend_strength": "WEAK",
      "liquidity": "LOW",
      "risk_level": "MEDIUM",
      "confidence_score": 30,
      "indicators": {
        "price": 92599.89000000,
        "sma_short": 92732.23777778,
        "sma_long": 92690.92809524,
        "sma_diff": 0.04456713,
        "rsi": 38.83955912,
        "atr": 27.43356931,
        "atr_percent": 0.02962600,
        "upper_threshold": 92745.79523385,
        "lower_threshold": 92636.06095663,
        "current_volume": 0.62158000,
        "average_volume": 5.85294000,
        "volume_ok": false
      }
    },
    "current": {
      "open_price": 92616.37000000,
      "close_price": 92599.89000000,
      "high_price": 92616.37000000,
      "low_price": 92599.88000000,
      "volume": 0.62158000,
      "amount": 57564.78136740,
      "open_time": "2026-01-19T03:23:00Z",
      "close_time": "2026-01-19T03:23:59.999Z",
      "trades": 253
    },
    "average": {
      "open_price": 93720.17,
      "close_price": 93712.71,
      "high_price": 93751.07,
      "low_price": 93677.02,
      "trade_price": 93237.26,
      "price_range": 74.05
    },
    "max_values": {
      "open_price": 95521.73000000,
      "close_price": 95521.74000000,
      "high_price": 95531.12000000,
      "low_price": 95521.00000000,
      "price_range": 917.16000000,
      "volume": 437.07618000,
      "amount": 41129202.21135120
    },
    "min_values": {
      "open_price": 92209.91000000,
      "close_price": 92209.91000000,
      "high_price": 92328.15000000,
      "low_price": 91910.20000000,
      "price_range": 0.01000000,
      "volume": 0.01331000,
      "amount": 1231.59500140
    },
    "total": {
      "volume": 7903.66,
      "amount": 736842240.13
    },
    "additional_information": {
      "entries_count": 361,
      "beginTime": "2026-01-18T21:23:59.999Z",
      "endTime": "2026-01-19T03:23:59.999Z",
      "duration": "PT6H"
    }
  }
}
```

**Response includes**:

- **Analysis**: Current trading signal with market assessment
- **Current**: Latest OHLCV data and trade count
- **Average**: Average trade price (VWAP-like, derived from total amount / total volume)
- **Max/Min**: Extremum values within the interval
- **Total**: Cumulative volume and amount
- **Info**: Metadata including candle count, time range, and interval duration

### **Storing cryptocurrency data in TimescaleDB**

After successfully retrieving data from the Binance API, all cryptocurrency information is stored in TimescaleDB.

| symbol  | close\_time                       | open\_time                        | open\_price | close\_price | high\_price | low\_price | volume  | amount         | trades |
|:--------|:----------------------------------|:----------------------------------|:------------|:-------------|:------------|:-----------|:--------|:---------------|:-------|
| ETHUSDT | 2026-01-19 16:39:59.999000 +00:00 | 2026-01-19 16:39:00.000000 +00:00 | 3219.52     | 3218.98      | 3219.94     | 3218.62    | 94.5675 | 304446.538688  | 3240   |
| BTCUSDT | 2026-01-19 16:39:59.999000 +00:00 | 2026-01-19 16:39:00.000000 +00:00 | 93234.41    | 93229.08     | 93234.41    | 93222.35   | 2.20986 | 206020.9851719 | 890    |
| ETHUSDT | 2026-01-19 16:40:59.999000 +00:00 | 2026-01-19 16:40:00.000000 +00:00 | 3218.98     | 3217.48      | 3218.99     | 3217.47    | 65.0898 | 209477.046718  | 1918   |
| BTCUSDT | 2026-01-19 16:40:59.999000 +00:00 | 2026-01-19 16:40:00.000000 +00:00 | 93229.08    | 93191.96     | 93229.09    | 93191.96   | 2.90681 | 270936.8196032 | 1487   |
| ETHUSDT | 2026-01-19 16:41:59.999000 +00:00 | 2026-01-19 16:41:00.000000 +00:00 | 3217.47     | 3218.26      | 3218.36     | 3216.74    | 62.8779 | 202308.107113  | 3063   |
| BTCUSDT | 2026-01-19 16:41:59.999000 +00:00 | 2026-01-19 16:41:00.000000 +00:00 | 93191.96    | 93200.6      | 93200.6     | 93173.89   | 4.7     | 437983.9348559 | 2004   |

In the example above:

**Fields**:

- **symbol**: Trading pair symbol (e.g., BTCUSDT)
- **openTime/closeTime**: Bar start/end timestamps (ISO 8601)
- **openPrice/closePrice**: Opening/closing prices
- **highPrice/lowPrice**: High/low prices during the period
- **volume**: Base asset volume
- **amount**: Quote asset amount
- **trades**: Number of trades executed
- **timePeriod**: Duration of the candlestick (ISO 8601 duration)

---

## **Installation Guide**

### **Prerequisites**

- Java 21
- Gradle 9.2.1
- Docker 29.1.3

### **Installation and Startup Steps**

1. **Clone the Repository**
   ```bash
   git clone https://github.com/dzenthai/cryptora.git
   cd cryptora
   ```

2. **Add Environment Variables**
   Before setting environment variables, make sure your Binance account is verified,
   as this is required to obtain the API_KEY and API_SECRET.

   You can obtain the API key and secret by following this [link](https://www.binance.com/my/settings/api-management).

   Create an .env file and add the required environment variables such as the Binance API key and secret.

3. **Build the Project Using Gradle**
   ```bash
   ./gradlew build
   ```

4. **Run the Application Using Docker**
   ```bash
   docker-compose up --build
   ```

---

## **Configuration**

### **Technical Indicator Parameters**

Edit `application.yaml` to customize analysis behavior:

```yaml
cryptora:
  short:
    time:
      period: 9     # Fast MA; lower = quicker response to price, higher = smoother trend
  long:
    time:
      period: 21    # Slow MA; provides the trend baseline for crossover signals
  atr:
    period: 14      # Volatility window; captures the average price swing range
    multiplier:     # Dynamic filter; higher = more noise protection, lower = aggressive entries
      strong: 1.2
      weak: 2.0 
  rsi:
    period: 14      # Momentum window; standard period to identify market exhaustion
    overbought: 70  # Sell pressure zone; higher = waits for extreme greed before exiting
    oversold: 30    # Buy pressure zone; lower = hunts for deeper dips (fewer "fake" bottoms)
  volume:
    period: 20      # Liquidity baseline; filters out low-volume "wash trading" and spikes
```

**Recommended Settings for 1-minute Timeframe**:

- Standard periods (9/21) for a balanced trend baseline and smoother crossover signals.
- Standard RSI period of 14 for reliable momentum detection and market exhaustion analysis.
- ATR multiplier of 2.0 to provide high-level noise protection and filter out false breakouts.
- Classic Overbought/Oversold thresholds (70/30) to identify established market reversals and extreme greed/fear.

### **Adding New Cryptocurrencies**

Update the `Asset` enum in `model/enums/Asset.java`:

```java
public enum Asset {
    BTC,
    ETH,
    TON,
    // Add more supported pairs here
}
```

**Note**: All tickers are automatically suffixed with "USDT" to form trading pairs (symbols).

### **Adjusting Fetch Interval**

Modify scheduler frequency in `job/AppScheduler.java`:

```java

@Scheduled(fixedRate = 60000) // 60000ms = 1 minute
public void executeInSequence() {
    // Fetch and analyze data
}
```

### **Historical Data Limit**

Adjust initial data fetch size in `service/FetchService.java`:

```java
ApiResponse<KlinesResponse> klinesResponse = spotRestApi.klines(
        symbol,
        Interval.INTERVAL_1m,
        null,
        null,
        "+0",
        500  // Number of historical candles (max: 1000)
);
```