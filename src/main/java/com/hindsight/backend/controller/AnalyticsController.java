package com.hindsight.backend.controller;

import com.hindsight.backend.service.AnalyticsService;
import com.hindsight.backend.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hindsight.backend.exception.ApiException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public AnalyticsController(AnalyticsService analyticsService, ChatService chatService, ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/crime-trends")
    public ResponseEntity<Map<String, String>> getCrimeTrends() {
        Map<String, Object> data = chatService.getCachedPoliceData();

        try {
            String prompt = String.format("""
                You are a Police Station Analytics Assistant AI.
                
                Here is the available data:
                
                1. **Stations**:
                %s
                
                2. **Incidents**:
                %s
                
                3. **SOS Alerts**:
                %s
                
                Please analyze the data and return crime trend insights using the following guidelines:
                
                - Use **concise bullet points only** — no long paragraphs.
                - Each point should begin with a relevant **emoji or icon** (e.g., 🚨, 📍, ⚠️, ✅, ⏱).
                - Highlight key crime trends, patterns, correlations between SOS and incidents.
                - Provide **actionable** insights useful for law enforcement.
                - Avoid repeating raw data or giving general suggestions — focus on insights derived from the data.
                """,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.get("stations")),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.get("incidents")),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.get("sos"))
            );

            String reply = analyticsService.generateContent(prompt);
            return ResponseEntity.ok(Map.of("reply", reply));
        } catch (Exception e) {
            log.error("Failed to build crime trends prompt or call Gemini", e);
            throw new ApiException("Failed to generate crime trends analysis", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
