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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LlmService {

    private final WebClient groqWebClient;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String modelName;

    public LlmService(WebClient groqWebClient) {
        this.groqWebClient = groqWebClient;
    }

    public String chatCompletion(String systemPrompt, List<Map<String, String>> messagesList) {
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new Message("system", systemPrompt));
            for (Map<String, String> msg : messagesList) {
                messages.add(new Message(msg.get("role"), msg.get("content")));
            }

            GroqRequest request = GroqRequest.builder()
                    .model(modelName)
                    .messages(messages)
                    .temperature(0.7)
                    .max_tokens(1024)
                    .build();

            GroqResponse response = groqWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .onErrorResume(e -> {
                        log.error("Groq chat API error: ", e);
                        return Mono.just(fallbackChatResponse());
                    })
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            log.error("Error executing Groq chat completion", e);
        }
        return "I'm having trouble thinking right now. Please try again in a moment.";
    }

    public String jsonCompletion(String systemPrompt, String userMessage) {
        try {
            List<Message> messages = List.of(
                    new Message("system", systemPrompt),
                    new Message("user", userMessage)
            );

            GroqRequest request = GroqRequest.builder()
                    .model(modelName)
                    .messages(messages)
                    .temperature(0.3)
                    .max_tokens(512)
                    .build();

            GroqResponse response = groqWebClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GroqResponse.class)
                    .onErrorResume(e -> {
                        log.error("Groq JSON API error: ", e);
                        return Mono.just(fallbackJsonResponse());
                    })
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            }
        } catch (Exception e) {
            log.error("Error executing Groq JSON completion", e);
        }
        return "{\"error\": \"LLM call failed\"}";
    }

    private GroqResponse fallbackChatResponse() {
        GroqResponse response = new GroqResponse();
        Choice choice = new Choice();
        choice.setMessage(new Message("assistant", "I'm having trouble thinking right now. Please try again in a moment."));
        response.setChoices(List.of(choice));
        return response;
    }

    private GroqResponse fallbackJsonResponse() {
        GroqResponse response = new GroqResponse();
        Choice choice = new Choice();
        choice.setMessage(new Message("assistant", "{\"error\": \"LLM call failed\"}"));
        response.setChoices(List.of(choice));
        return response;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroqRequest {
        private String model;
        private List<Message> messages;
        private Double temperature;
        private Integer max_tokens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroqResponse {
        private List<Choice> choices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Message message;
    }
}
