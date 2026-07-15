package com.hindsight.backend.controller;

import com.hindsight.backend.dto.StatsDto;
import com.hindsight.backend.security.UserPrincipal;
import com.hindsight.backend.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/user/progress/overview")
    public ResponseEntity<StatsDto.ProgressOverviewResponse> getProgressOverview(@AuthenticationPrincipal UserPrincipal principal) {
        StatsDto.ProgressOverviewResponse response = progressService.getProgressOverview(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/insights/mistake-patterns")
    public ResponseEntity<List<StatsDto.MistakePatternResponse>> getMistakePatterns(@AuthenticationPrincipal UserPrincipal principal) {
        List<StatsDto.MistakePatternResponse> response = progressService.getMistakePatterns(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/proficiency/chart")
    public ResponseEntity<List<StatsDto.LanguageProficiencyResponse>> getProficiencyChart(@AuthenticationPrincipal UserPrincipal principal) {
        List<StatsDto.LanguageProficiencyResponse> response = progressService.getProficiencyChart(principal.getId());
        return ResponseEntity.ok(response);
    }
}
