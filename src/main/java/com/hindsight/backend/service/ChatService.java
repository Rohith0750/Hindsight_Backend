package com.hindsight.backend.service;

import com.hindsight.backend.dto.ChatDto;
import com.hindsight.backend.exception.ApiException;
import com.hindsight.backend.model.Incident;
import com.hindsight.backend.model.Sos;
import com.hindsight.backend.repository.IncidentRepository;
import com.hindsight.backend.repository.SosRepository;
import com.hindsight.backend.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatService {

    private final LlmService llmService;
    private final AnalyticsService analyticsService;
    private final SessionService sessionService;
    private final IncidentRepository incidentRepository;
    private final SosRepository sosRepository;
    private final ObjectMapper objectMapper;

    // Cache for police chatbot analytics
    private Map<String, Object> cachedPoliceData = new ConcurrentHashMap<>();

    public ChatService(LlmService llmService,
                       AnalyticsService analyticsService,
                       SessionService sessionService,
                       IncidentRepository incidentRepository,
                       SosRepository sosRepository,
                       ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.analyticsService = analyticsService;
        this.sessionService = sessionService;
        this.incidentRepository = incidentRepository;
        this.sosRepository = sosRepository;
        this.objectMapper = objectMapper;
    }

    private boolean isHintRequest(String message) {
        if (message == null) return false;
        String lowered = message.toLowerCase();
        return Constants.HINT_TRIGGERS.stream().anyMatch(lowered::contains);
    }

    public ChatDto.MessageResponse handleSocraticMessage(ChatDto.MessageRequest request) {
        String userId = request.getUser_id();
        String message = request.getMessage();

        if (userId == null || message == null) {
            throw new ApiException("user_id and message are required", HttpStatus.BAD_REQUEST);
        }

        // 1. Hint request detection
        if (isHintRequest(message)) {
            return ChatDto.MessageResponse.builder()
                    .type("hint_request_detected")
                    .message("It looks like you want a hint. Let me ask you two quick questions first to make sure the hint will actually help you. Head to the hint section!")
                    .redirect_to_hint_gate(true)
                    .build();
        }

        // 2. Load session memory context
        SessionService.ActiveSession session = sessionService.getActiveSession(userId);
        String memoryContext;
        if (session != null && session.getMemory() != null && !session.getMemory().isBlank()) {
            memoryContext = "STUDENT MEMORY CONTEXT (use this to personalize your responses):\n" +
                    session.getMemory() +
                    "\n\nBased on this history, you already know this student's patterns. Reference their past mistakes naturally when relevant.";
        } else {
            memoryContext = "STUDENT MEMORY CONTEXT: This is a new student with no history yet.";
        }

        String systemPrompt = Constants.SOCRATIC_SYSTEM_PROMPT.replace("{memory_context}", memoryContext);

        // 3. Prepare messages list
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getConversation_history() != null && !request.getConversation_history().isEmpty()) {
            messages.addAll(request.getConversation_history());
            messages.add(Map.of("role", "user", "content", message));
        } else {
            messages.add(Map.of("role", "user", "content",
                    String.format("[Context: I am working on the problem '%s'. Description: %s]",
                            request.getProblem_title(), request.getProblem_description())));
            messages.add(Map.of("role", "user", "content", message));
        }

        String response = llmService.chatCompletion(systemPrompt, messages);

        return ChatDto.MessageResponse.builder()
                .type("message")
                .message(response)
                .redirect_to_hint_gate(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Police Chatbot Analytics Logic
    // ─────────────────────────────────────────────────────────────────────────────

    @Scheduled(fixedRate = 5000)
    public void fetchPoliceDataScheduled() {
        try {
            List<Incident> incidents = incidentRepository.findAll().stream().limit(10).collect(Collectors.toList());
            List<Sos> sosAlerts = sosRepository.findAll().stream().limit(10).collect(Collectors.toList());

            Set<String> stationIds = new HashSet<>();
            incidents.forEach(i -> { if (i.getStationId() != null) stationIds.add(i.getStationId()); });
            sosAlerts.forEach(s -> { if (s.getStationId() != null) stationIds.add(s.getStationId()); });

            List<Map<String, Object>> stations = new ArrayList<>();
            int idx = 1;
            for (String id : stationIds) {
                stations.add(Map.of(
                        "id", id,
                        "name", "Station " + id,
                        "area", "Area " + idx++
                ));
            }

            cachedPoliceData.put("incidents", incidents);
            cachedPoliceData.put("sos", sosAlerts);
            cachedPoliceData.put("stations", stations);
        } catch (Exception e) {
            log.error("Failed to fetch police analytical data", e);
        }
    }

    public Map<String, Object> getCachedPoliceData() {
        if (cachedPoliceData.isEmpty()) {
            fetchPoliceDataScheduled();
        }
        return cachedPoliceData;
    }

    public ChatDto.ChatbotResponse policeChat(String message) {
        Map<String, Object> data = getCachedPoliceData();

        try {
            String prompt = String.format("""
                You are a Police Station Analytics Assistant AI.
                User asked: "%s".
                
                Here is the available data:
                
                Stations:
                %s
                
                Incidents:
                %s
                
                SOS alerts:
                %s
                
                Use only the available data to answer questions.
                If information is missing, politely say "Data not available."
                """,
                    message,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.get("stations")),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.get("incidents")),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.get("sos"))
            );

            String reply = analyticsService.generateContent(prompt);
            return new ChatDto.ChatbotResponse(reply);
        } catch (Exception e) {
            log.error("Failed to format police data or complete chat", e);
            throw new ApiException("Failed to query police chatbot analyst", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
