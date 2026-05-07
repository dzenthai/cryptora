package com.dzenthai.cryptora.service;

import com.dzenthai.cryptora.model.dto.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class ReportService {

    private final StatisticService statisticService;

    public ReportService(
            StatisticService statisticService
    ) {
        this.statisticService = statisticService;
    }

    public Report getReport(String asset, String duration) {
        log.debug("ReportService | Receiving report for {}", asset);
        return Report.builder()
                .statistic(statisticService.calculateStatisticReport(asset, duration))
                .build();
    }
}

