package com.hindsight.backend.controller;

import com.hindsight.backend.dto.ProblemDto;
import com.hindsight.backend.dto.StatsDto;
import com.hindsight.backend.security.UserPrincipal;
import com.hindsight.backend.service.ProblemService;
import com.hindsight.backend.service.UserStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
public class UserStatsController {

    private final UserStatsService userStatsService;
    private final ProblemService problemService;

    public UserStatsController(UserStatsService userStatsService, ProblemService problemService) {
        this.userStatsService = userStatsService;
        this.problemService = problemService;
    }

    @GetMapping("/user/stats")
    public ResponseEntity<StatsDto.UserStatsResponse> getUserStats(@AuthenticationPrincipal UserPrincipal principal) {
        StatsDto.UserStatsResponse response = userStatsService.getUserStats(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/insights/banner")
    public ResponseEntity<StatsDto.InsightBannerResponse> getInsightBanner(@AuthenticationPrincipal UserPrincipal principal) {
        StatsDto.InsightBannerResponse response = userStatsService.getInsightBanner(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/activities")
    public ResponseEntity<List<StatsDto.UserActivityResponse>> getUserActivities(@AuthenticationPrincipal UserPrincipal principal) {
        List<StatsDto.UserActivityResponse> response = userStatsService.getUserActivities(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/proficiency")
    public ResponseEntity<List<StatsDto.LanguageProficiencyResponse>> getUserProficiency(@AuthenticationPrincipal UserPrincipal principal) {
        List<StatsDto.LanguageProficiencyResponse> response = userStatsService.getUserProficiency(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/streak-activity")
    public ResponseEntity<List<StatsDto.StreakActivityResponse>> getUserStreakActivity(@AuthenticationPrincipal UserPrincipal principal) {
        List<StatsDto.StreakActivityResponse> response = userStatsService.getUserStreakActivity(principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/problems/recommended")
    public ResponseEntity<List<ProblemDto.ProblemResponse>> getRecommendedProblems(@AuthenticationPrincipal UserPrincipal principal) {
        List<ProblemDto.ProblemResponse> response = problemService.getRecommendedProblems(principal.getId());
        return ResponseEntity.ok(response);
    }
}
