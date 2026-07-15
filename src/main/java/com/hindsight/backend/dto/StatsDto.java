package com.hindsight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

public class StatsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatsResponse {
        private int problemsSolved;
        private int accuracy;
        private int streak;
        private int xp;
        private long hintsUsed;
        private long hintsEarned;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsightBannerResponse {
        private String message;
        private String topic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityResponse {
        private String problem;
        private String verdict;
        private String language;
        private Instant date;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageProficiencyResponse {
        private String language;
        private int solved;
        private String name;
        private int proficiency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakActivityResponse {
        private String date;
        private int solved;
        private boolean frozen;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressOverviewResponse {
        private long hintsUsed;
        private long hintsEarned;
        private int accuracy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistakePatternResponse {
        private String pattern;
        private long count;
        private String label;
    }
}
