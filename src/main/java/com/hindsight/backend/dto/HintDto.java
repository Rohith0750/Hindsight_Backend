package com.hindsight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class HintDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HintRequest {
        private String user_id;
        private String problem_title;
        private String topic;
        private String problem_id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HintGateResponse {
        private String status;
        private List<String> questions;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerRequest {
        private String user_id;
        private Integer question_index;
        private String answer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResponse {
        private String status;
        private boolean passed;
        private String feedback;
        private boolean hint_unlocked;
        private String hint;
        private List<Boolean> questions_passed;
        private String message;
    }
}
