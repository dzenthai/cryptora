package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.model.dto.Analysis;
import com.dzenthai.cryptora.model.entity.Quote;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
public class AnalysisService {

    private final Integer shortTimePeriod;

    private final Integer longTimePeriod;

    private final Integer atrPeriod;

    private final Double atrMultiplier;

    private final QuoteService quoteService;

    public AnalysisService(
            @Value("${cryptora.short.time.period}") Integer shortTimePeriod,
            @Value("${cryptora.long.time.period}") Integer longTimePeriod,
            @Value("${cryptora.atr.period}") Integer atrPeriod,
            @Value("${cryptora.atr.multiplier}") Double atrMultiplier,
            QuoteService quoteService
    ) {
        this.shortTimePeriod = shortTimePeriod;
        this.longTimePeriod = longTimePeriod;
        this.atrPeriod = atrPeriod;
        this.atrMultiplier = atrMultiplier;
        this.quoteService = quoteService;
    }

    public Analysis getAnalysis(String ticker) {
        return analyzeTickerQuotes(ticker, quoteService.getQuotesByTicker(ticker), false);
    }

    public void getAnalysis() {
        quoteService.getAllQuotes().stream()
                .collect(Collectors.groupingBy(Quote::getTicker))
                .forEach((ticker, quotes) ->
                        analyzeTickerQuotes(ticker, quotes, true));
    }

    private Analysis analyzeTickerQuotes(String ticker, List<Quote> quotes, boolean shouldLog) {
        var shortCut = ticker.replaceAll("USDT", "");
        quotes.sort(Comparator.comparing(Quote::getDatetime));

        var series = buildBarSeries(quotes);
        if (series.getBarCount() < shortTimePeriod) {
            var warn = "Insufficient data for SMA calculation";
            if (shouldLog) {
                log.info("AnalysisService | Quote: {}, Action: {}",
                        ticker, warn);
            }
            return Analysis.builder()
                    .ticker(ticker)
                    .action(warn)
                    .build();
        }
        return evaluateSignals(series, shortCut, shouldLog);
    }

    private Analysis evaluateSignals(BarSeries series, String ticker, boolean shouldLog) {
        SMAIndicator shortTermSMA = new SMAIndicator(new ClosePriceIndicator(series), shortTimePeriod);
        SMAIndicator longTermSMA = new SMAIndicator(new ClosePriceIndicator(series), longTimePeriod);

        Num latestPrice = series.getLastBar().getClosePrice();
        Num shortTermValue = shortTermSMA.getValue(series.getEndIndex());
        Num longTermValue = longTermSMA.getValue(series.getEndIndex());

        var thresholds = calculateThresholds(series, longTermValue);
        return sendSignalMessage(latestPrice, shortTermValue, thresholds[0], thresholds[1], ticker, shouldLog);
    }

    private Num[] calculateThresholds(BarSeries series, Num longTermValue) {
        ATRIndicator atrIndicator = new ATRIndicator(series, atrPeriod);
        Num atrValue = atrIndicator.getValue(series.getEndIndex());

        Num thresholdUpper = longTermValue.plus(atrValue.multipliedBy(DecimalNum.valueOf(atrMultiplier)));
        Num thresholdLower = longTermValue.minus(atrValue.multipliedBy(DecimalNum.valueOf(atrMultiplier)));

        return new Num[]{thresholdUpper, thresholdLower};
    }

    private Analysis sendSignalMessage(
            Num latestPrice,
            Num shortTermValue,
            Num thresholdUpper,
            Num thresholdLower,
            String ticker,
            boolean shouldLog
    ) {
        if (shortTermValue.isGreaterThan(thresholdUpper) && latestPrice.isGreaterThan(shortTermValue)) {
            return sendSignals("BUY", ticker, shouldLog);
        } else if (shortTermValue.isLessThan(thresholdLower) && latestPrice.isLessThan(shortTermValue)) {
            return sendSignals("SELL", ticker, shouldLog);
        } else {
            return sendSignals("HOLD", ticker, shouldLog);
        }
    }

    private Analysis sendSignals(String action, String ticker, boolean shouldLog) {
        if (shouldLog) {
            var now = LocalDateTime.now();
            log.info("AnalysisService | Quote: {}, Action: {}, Datetime: {}",
                    ticker, action, now);
        }
        return Analysis.builder()
                .ticker(ticker)
                .action(action)
                .build();
    }

    private BarSeries buildBarSeries(List<Quote> quotes) {
        var series = new BaseBarSeries();
        series.setMaximumBarCount(1000);

        ZonedDateTime lastBarEndTime = null;
        for (Quote quote : quotes) {
            ZonedDateTime endTime = quote.getDatetime().atZone(ZoneOffset.UTC);
            if (lastBarEndTime != null && !endTime.isAfter(lastBarEndTime)) {
                log.warn("AnalysisService | Bar with end time {} skipped; not later than previous {}",
                        endTime, lastBarEndTime);
                continue;
            }
            series.addBar(buildBar(endTime, quote));
            lastBarEndTime = endTime;
        }
        return series;
    }

    private Bar buildBar(ZonedDateTime endTime, Quote quote) {
        return new BaseBar(
                Duration.ofHours(1),
                endTime,
                DecimalNum.valueOf(quote.getOpenPrice()),
                DecimalNum.valueOf(quote.getHighPrice()),
                DecimalNum.valueOf(quote.getLowPrice()),
                DecimalNum.valueOf(quote.getClosePrice()),
                DecimalNum.valueOf(quote.getVolume()),
                DecimalNum.valueOf(quote.getAmount())
        );
    }
}
