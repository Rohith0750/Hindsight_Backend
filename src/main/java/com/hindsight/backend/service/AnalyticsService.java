package com.hindsight.backend.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
public class AnalyticsService {

    private final WebClient geminiWebClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-pro}")
    private String modelName;

    public AnalyticsService(WebClient geminiWebClient) {
        this.geminiWebClient = geminiWebClient;
    }

    public String generateContent(String prompt) {
        try {
            GeminiRequest request = GeminiRequest.builder()
                    .contents(List.of(
                            GeminiRequest.Content.builder()
                                    .parts(List.of(new GeminiRequest.Part(prompt)))
                                    .build()
                    ))
                    .build();

            String path = "/v1beta/models/" + modelName + ":generateContent";

            GeminiResponse response = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .onErrorResume(e -> {
                        log.error("Gemini API error: ", e);
                        return Mono.empty();
                    })
                    .block();

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                GeminiResponse.Candidate candidate = response.getCandidates().get(0);
                if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                    return candidate.getContent().getParts().get(0).getText();
                }
            }
        } catch (Exception e) {
            log.error("Error executing Gemini API content generation", e);
        }
        return "Gemini analyst is currently unavailable. Please try again later.";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        private List<Content> contents;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Content {
            private List<Part> parts;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Part {
            private String text;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiResponse {
        private List<Candidate> candidates;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Candidate {
            private Content content;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Content {
            private List<Part> parts;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Part {
            private String text;
        }
    }
}
