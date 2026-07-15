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
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemoryService {

    private final WebClient hindsightWebClient;

    @Value("${hindsight.api.key}")
    private String apiKey;

    @Value("${hindsight.endpoint}")
    private String endpoint;

    public MemoryService(WebClient hindsightWebClient) {
        this.hindsightWebClient = hindsightWebClient;
    }

    private String getBankId(String userId) {
        return "student-" + userId;
    }

    public String readMemory(String userId) {
        try {
            String bankId = getBankId(userId);
            String recallUrl = endpoint + "/v1/default/banks/" + bankId + "/memories/recall";

            RecallRequest body = RecallRequest.builder()
                    .query("coding weaknesses, mistakes, hint history, topics struggled, languages used")
                    .budget("mid")
                    .build();

            RecallResponse response = hindsightWebClient.post()
                    .uri(recallUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(RecallResponse.class)
                    .onErrorResume(e -> {
                        log.error("Hindsight recall error: ", e);
                        return Mono.empty();
                    })
                    .block();

            if (response != null && response.getResults() != null) {
                return response.getResults().stream()
                        .map(RecallResult::getText)
                        .filter(text -> text != null && !text.isBlank())
                        .collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.error("Error reading memory from Hindsight", e);
        }
        return "";
    }

    public boolean writeMemory(String userId, String content) {
        try {
            String bankId = getBankId(userId);
            String retainUrl = endpoint + "/v1/default/banks/" + bankId + "/memories";

            RetainItem item = RetainItem.builder()
                    .content(content)
                    .build();

            RetainRequest body = RetainRequest.builder()
                    .items(List.of(item))
                    .async(false)
                    .build();

            hindsightWebClient.post()
                    .uri(retainUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .onErrorResume(e -> {
                        log.error("Hindsight retain error: ", e);
                        return Mono.empty();
                    })
                    .block();

            return true;
        } catch (Exception e) {
            log.error("Error writing memory to Hindsight", e);
            return false;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecallRequest {
        private String query;
        @Builder.Default
        private String budget = "mid";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecallResponse {
        private List<RecallResult> results;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecallResult {
        private String text;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetainRequest {
        private List<RetainItem> items;
        private boolean async;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetainItem {
        private String content;
    }
}
