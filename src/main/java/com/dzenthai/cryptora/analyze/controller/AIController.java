package com.dzenthai.cryptora.analyze.controller;

import com.dzenthai.cryptora.analyze.service.AIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/analyze/{ticker}")
    public String analyze(@PathVariable String ticker) {
        return aiService.getAnalysis(ticker);
    }
}