package com.dzenthai.cryptora.mapper;

import com.dzenthai.cryptora.model.dto.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.ta4j.core.num.Num;

import java.math.BigDecimal;
import java.math.RoundingMode;


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
            double doubleSmaDiff,
            Num numRsiVal,
            Num numAtrVal,
            Num numThrUp,
            Num numThrLo,
            double doubleCurrVol,
            double doubleRecentAvgVol,
            boolean volumeOk,
            boolean shouldLog
    ) {

        log.trace("IndicatorMapper | Building indicators for {}", symbol);

        var price = numPrice.bigDecimalValue().setScale(8, RoundingMode.HALF_UP);
        var smaShort = numShortSMA.bigDecimalValue().setScale(8, RoundingMode.HALF_UP);
        var smaLong = numLongSMA.bigDecimalValue().setScale(8, RoundingMode.HALF_UP);
        var smaDiff = BigDecimal.valueOf(doubleSmaDiff).setScale(8, RoundingMode.HALF_UP);
        var rsi = numRsiVal.bigDecimalValue().setScale(8, RoundingMode.HALF_UP);
        var atr = numAtrVal.bigDecimalValue().setScale(8, RoundingMode.HALF_UP);
        var atrPercent = atr.divide(price, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        var upperThreshold = numThrUp.bigDecimalValue().setScale(8, RoundingMode.HALF_UP);
        var lowerThreshold = numThrLo.bigDecimalValue().setScale(8, RoundingMode.HALF_UP);
        var currentVolume = BigDecimal.valueOf(doubleCurrVol).setScale(8, RoundingMode.HALF_UP);
        var averageVolume = BigDecimal.valueOf(doubleRecentAvgVol).setScale(8, RoundingMode.HALF_UP);

        if (shouldLog) {
            log.info("IndicatorMapper | Symbol: {}, Price: {}, SMA{}: {}, SMA{}: {}, SMA Diff%: {}, RSI: {}, ATR: {}, ATR%: {}, Upper Threshold: {}, Lower Threshold: {}, Vol: {}/{}, Volume Ok: {}",
                    symbol, price, shortTimePeriod, smaShort, longTimePeriod, smaLong, smaDiff, rsi, atr, atrPercent, upperThreshold, lowerThreshold, currentVolume, averageVolume, volumeOk);
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
