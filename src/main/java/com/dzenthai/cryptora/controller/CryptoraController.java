package com.dzenthai.cryptora.controller;

import com.dzenthai.cryptora.service.AnalysisService;
import com.dzenthai.cryptora.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/cryptora")
public class CryptoraController {

    private final AnalysisService analysisService;

    private final ReportService reportService;

    public CryptoraController(
            AnalysisService analysisService,
            ReportService reportService
    ) {
        this.analysisService = analysisService;
        this.reportService = reportService;
    }

    @GetMapping("analysis/{ticker}")
    public ResponseEntity<?> analysis(@PathVariable String ticker) {
        return new ResponseEntity<>(analysisService.getAnalysis(ticker),
                HttpStatus.OK);
    }

    @GetMapping("/report/{ticker}")
    public ResponseEntity<?> report(@PathVariable String ticker) {
        return new ResponseEntity<>(reportService.getReport(ticker),
                HttpStatus.OK);
    }
}
