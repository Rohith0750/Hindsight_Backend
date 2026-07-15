package com.hindsight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class SessionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistakeRequest {
        private String userId;
        private Object mistake; // Can be string or map representing mistakeType
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartSessionRequest {
        private String user_id;
        private String problem_id;
        private String problem_title;
        private String topic;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndSessionRequest {
        private String user_id;
        private String problem_title;
        private String topic;
        private String language;
        private Integer hints_taken;
        private Integer hints_earned;
        private List<String> mistakes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionResponse {
        private String status;
        private String user_id;
        private boolean has_memory;
        private String memory_preview;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionEndResponse {
        private String status;
        private boolean memory_saved;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStatusResponse {
        private String user_id;
        private String problem_title;
        private String topic;
        private Integer hints_taken;
        private Integer hints_earned;
    }
}
