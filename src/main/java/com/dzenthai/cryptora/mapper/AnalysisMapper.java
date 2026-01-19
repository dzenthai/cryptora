package com.dzenthai.cryptora.mapper;

import com.dzenthai.cryptora.model.dto.Analysis;
import com.dzenthai.cryptora.model.dto.Indicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class AnalysisMapper {

    public Analysis mapToAnalysis(
            String symbol,
            String action,
            String marketState,
            String volatility,
            String trendStrength,
            String liquidity,
            String riskLevel,
            Integer confidenceScore,
            Indicator details,
            boolean shouldLog
    ) {
        log.trace("AnalysisMapper | Building analysis for {}", symbol);

        if (shouldLog) {
            log.info("AnalysisMapper  | Symbol: {}, Action: {}, Market: {}, Volatility: {}, Trend: {}, Liquidity: {}, Risk: {}, Confidence: {}%",
                    symbol, action, marketState, volatility, trendStrength, liquidity, riskLevel, confidenceScore);
        }

        return Analysis.builder()
                .symbol(symbol)
                .action(action)
                .marketState(marketState)
                .volatility(volatility)
                .trendStrength(trendStrength)
                .liquidity(liquidity)
                .riskLevel(riskLevel)
                .confidenceScore(confidenceScore)
                .details(details)
                .build();
    }
}
