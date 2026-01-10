package com.dzenthai.cryptora.controller;

import com.dzenthai.cryptora.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(
            ReportService reportService
    ) {
        this.reportService = reportService;
    }

    @GetMapping("/ticker={ticker}&interval={interval}")
    public ResponseEntity<?> report(
            @PathVariable String ticker,
            @PathVariable String interval
    ) {
        return new ResponseEntity<>(reportService.getReport(ticker, interval),
                HttpStatus.OK);
    }
}
