package com.hindsight.backend.service;

import com.hindsight.backend.dto.SessionDto;
import com.hindsight.backend.dto.StatsDto;
import com.hindsight.backend.exception.ApiException;
import com.hindsight.backend.model.Submission;
import com.hindsight.backend.repository.SubmissionRepository;
import com.hindsight.backend.util.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HindsightService {

    private final SubmissionRepository submissionRepository;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    // In-memory mistake store
    private final Map<String, List<String>> mistakeMemory = new ConcurrentHashMap<>();

    public HindsightService(SubmissionRepository submissionRepository,
                            LlmService llmService,
                            ObjectMapper objectMapper) {
        this.submissionRepository = submissionRepository;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public boolean storeMistake(String userId, Object mistakeObj) {
        if (userId == null || mistakeObj == null) {
            throw new ApiException("userId and mistake are required", HttpStatus.BAD_REQUEST);
        }

        String mistakeStr;
        if (mistakeObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) mistakeObj;
            mistakeStr = map.containsKey("mistakeType") ? String.valueOf(map.get("mistakeType")) : map.toString();
        } else {
            mistakeStr = String.valueOf(mistakeObj);
        }

        mistakeMemory.computeIfAbsent(userId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(mistakeStr);

        log.info("[Hindsight] Stored mistake for {}: {}", userId, mistakeStr);
        return true;
    }

    public List<MistakePattern> getMistakePatterns(String userId) {
        // 1. Fetch mistakes from database submissions (limit 50)
        List<Submission> submissions = submissionRepository.findByUserId(userId);
        List<String> dbMistakes = submissions.stream()
                .filter(s -> s.getMistakes() != null && !s.getMistakes().isEmpty())
                .flatMap(s -> s.getMistakes().stream())
                .limit(50)
                .collect(Collectors.toList());

        // 2. Fetch in-memory mistakes
        List<String> memMistakes = mistakeMemory.getOrDefault(userId, Collections.emptyList());

        // 3. Combine mistakes
        List<String> allMistakes = new ArrayList<>();
        allMistakes.addAll(dbMistakes);
        allMistakes.addAll(memMistakes);

        if (allMistakes.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Try LLM patterns extraction if mistakes >= 2
        if (allMistakes.size() >= 2) {
            String prompt = Constants.getMistakePatternPrompt(allMistakes);
            String rawJson = llmService.jsonCompletion(
                    "You are a coding coach analysing student mistake patterns. Always respond with valid JSON only.",
                    prompt
            );

            try {
                String clean = rawJson.trim().replaceAll("^```json|```$", "").trim();
                JsonNode root = objectMapper.readTree(clean);
                if (root.has("patterns") && root.get("patterns").isArray()) {
                    List<MistakePattern> patterns = new ArrayList<>();
                    for (JsonNode node : root.get("patterns")) {
                        patterns.add(new MistakePattern(
                                node.has("name") ? node.get("name").asText() : "",
                                node.has("severity") ? node.get("severity").asText() : "low",
                                node.has("description") ? node.get("description").asText() : ""
                        ));
                    }
                    return patterns;
                }
            } catch (Exception e) {
                log.error("Failed to parse mistake patterns LLM response", e);
            }
        }

        // Fallback: group by frequency in-memory
        Map<String, Integer> freq = new HashMap<>();
        for (String m : allMistakes) {
            freq.put(m, freq.getOrDefault(m, 0) + 1);
        }

        return freq.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(entry -> new MistakePattern(
                        entry.getKey(),
                        entry.getValue() >= 4 ? "high" : entry.getValue() >= 2 ? "medium" : "low",
                        "Occurred " + entry.getValue() + " times during sessions."
                ))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getInsights(String userId) {
        long total = submissionRepository.countByUserId(userId);
        long accepted = submissionRepository.countByUserIdAndStatus(userId, "accepted");

        int accuracy = total > 0 ? (int) Math.round(((double) accepted / total) * 100) : 0;

        String message;
        String type = "memory_update";

        if (total == 0) {
            message = "Start solving problems to build your Hindsight Memory! Your AI mentor is watching.";
        } else if (accuracy >= 80) {
            message = "🔥 You have a " + accuracy + "% accuracy rate. Outstanding work — your consistency is building mastery!";
            type = "positive";
        } else if (accuracy >= 50) {
            message = "📈 " + accuracy + "% accuracy — you're improving. Focus on the topics you miss most.";
            type = "encouragement";
        } else {
            message = "💡 " + accuracy + "% accuracy. Don't give up — every wrong answer is a lesson. Use hints wisely and review mistakes.";
            type = "guidance";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("type", type);
        response.put("accuracy", accuracy);
        response.put("total", total);
        response.put("accepted", accepted);
        return response;
    }

    public List<String> getInMemoryMistakes(String userId) {
        return mistakeMemory.getOrDefault(userId, Collections.emptyList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistakePattern {
        private String name;
        private String severity; // high, medium, low
        private String description;
    }
}
