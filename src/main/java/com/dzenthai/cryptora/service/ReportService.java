package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.model.dto.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class ReportService {

    private final StatisticService statisticService;

    private final OllamaService ollamaService;

    public ReportService(
            StatisticService statisticService,
            OllamaService ollamaService
    ) {
        this.statisticService = statisticService;
        this.ollamaService = ollamaService;
    }

    public Report getReport(String ticker) {
        log.debug("ReportService | Receiving report for {}", ticker);
        return Report.builder()
                .statistic(statisticService.calculateStatisticReport(ticker))
                .recommendation(ollamaService.generateAIResponse(ticker))
                .build();
    }
}

