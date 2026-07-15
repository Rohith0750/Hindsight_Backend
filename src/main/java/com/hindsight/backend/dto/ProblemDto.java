package com.hindsight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ProblemDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemResponse {
        private String id;
        private String title;
        private String slug;
        private String difficulty;
        private List<String> tags;
        private Integer acceptance;
        @Builder.Default
        private String aiPriority = "Medium";
        @Builder.Default
        private String status = "unsolved";
        private String aiReason;
    }
}
