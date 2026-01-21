package com.dzenthai.cryptora.mapper;

import com.dzenthai.cryptora.model.dto.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.ta4j.core.num.Num;


@Slf4j
@Component
public class IndicatorMapper {

    public Indicator mapToIndicator(
            String symbol,
            Num numPrice,
            int shortTimePeriod,
            Num numShortSMA,
            int longTimePeriod,
            Num numLongSMA,
            double smaDiff,
            Num numRsiVal,
            Num numAtrVal,
            Num numThrUp,
            Num numThrLo,
            double currentVolume,
            double averageVolume,
            boolean volumeOk,
            int confidenceScore,
            boolean shouldLog
    ) {

        log.trace("IndicatorMapper | Building indicators for {}", symbol);

        var price = numPrice.doubleValue();
        var smaShort = numShortSMA.doubleValue();
        var smaLong = numLongSMA.doubleValue();
        var rsi = numRsiVal.doubleValue();
        var atr = numAtrVal.doubleValue();
        var atrPercent = atr / price * 100;
        var upperThreshold = numThrUp.doubleValue();
        var lowerThreshold = numThrLo.doubleValue();

        if (shouldLog) {
            log.info("IndicatorMapper | Symbol: {}, Price: {}, SMA{}: {}, SMA{}: {}, SMA Diff%: {}, RSI: {}, ATR: {}, ATR%: {}, Upper Threshold: {}, Lower Threshold: {}, Vol: {}/{}, Volume Ok: {}, Confidence Score: {}",
                    symbol, price, shortTimePeriod, smaShort, longTimePeriod, smaLong, smaDiff, rsi, atr, atrPercent, upperThreshold, lowerThreshold, currentVolume, averageVolume, volumeOk, confidenceScore);
        }

        return Indicator.builder()
                .price(price)
                .smaShort(smaShort)
                .smaLong(smaLong)
                .smaDiff(smaDiff)
                .rsi(rsi)
                .atr(atr)
                .atrPercent(atrPercent)
                .upperThreshold(upperThreshold)
                .lowerThreshold(lowerThreshold)
                .currentVolume(currentVolume)
                .averageVolume(averageVolume)
                .volumeOk(volumeOk)
                .build();
    }
}
