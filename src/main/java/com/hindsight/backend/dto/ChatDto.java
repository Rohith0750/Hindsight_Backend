package com.hindsight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class ChatDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageRequest {
        private String user_id;
        private String message;
        private List<Map<String, String>> conversation_history;
        private String problem_title;
        private String problem_description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private String type;
        private String message;
        private boolean redirect_to_hint_gate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatbotRequest {
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatbotResponse {
        private String reply;
    }
}
