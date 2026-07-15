package com.hindsight.backend.controller;

import com.hindsight.backend.dto.SessionDto;
import com.hindsight.backend.service.HindsightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/hindsight")
public class HindsightController {

    private final HindsightService hindsightService;

    public HindsightController(HindsightService hindsightService) {
        this.hindsightService = hindsightService;
    }

    @PostMapping("/mistake")
    public ResponseEntity<Map<String, Boolean>> storeMistake(@RequestBody SessionDto.MistakeRequest request) {
        boolean success = hindsightService.storeMistake(request.getUserId(), request.getMistake());
        return ResponseEntity.ok(Map.of("success", success));
    }

    @GetMapping("/patterns/{userId}")
    public ResponseEntity<List<HindsightService.MistakePattern>> getMistakePatterns(@PathVariable String userId) {
        List<HindsightService.MistakePattern> patterns = hindsightService.getMistakePatterns(userId);
        return ResponseEntity.ok(patterns);
    }

    @GetMapping("/insights/{userId}")
    public ResponseEntity<Map<String, Object>> getInsights(@PathVariable String userId) {
        Map<String, Object> insights = hindsightService.getInsights(userId);
        return ResponseEntity.ok(insights);
    }
}
