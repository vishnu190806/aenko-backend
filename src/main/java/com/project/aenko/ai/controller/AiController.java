package com.project.aenko.ai.controller;

import com.project.aenko.ai.dto.InsightResponse;
import com.project.aenko.ai.service.AiService;
import com.project.aenko.common.config.AppConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/insight")
    public ResponseEntity<InsightResponse> insight() {
        return ResponseEntity.ok(aiService.insight(AppConfig.currentUserId()));
    }

    @PostMapping("/insight")
    public ResponseEntity<InsightResponse> refreshInsight() {
        return ResponseEntity.ok(aiService.insight(AppConfig.currentUserId()));
    }
}
