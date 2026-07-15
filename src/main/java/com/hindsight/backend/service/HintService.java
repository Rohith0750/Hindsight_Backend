package com.hindsight.backend.service;

import com.hindsight.backend.dto.HintDto;
import com.hindsight.backend.exception.ApiException;
import com.hindsight.backend.model.HintUsage;
import com.hindsight.backend.repository.HintUsageRepository;
import com.hindsight.backend.util.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HintService {

    private final LlmService llmService;
    private final HintUsageRepository hintUsageRepository;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    // In-memory hint gate store
    private final Map<String, ActiveHintGate> hintGates = new ConcurrentHashMap<>();

    public HintService(LlmService llmService,
                       HintUsageRepository hintUsageRepository,
                       @Lazy SessionService sessionService,
                       ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.hintUsageRepository = hintUsageRepository;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    public HintDto.HintGateResponse requestHint(HintDto.HintRequest request) {
        String userId = request.getUser_id();
        String problemTitle = request.getProblem_title();
        String topic = request.getTopic();
        String problemId = request.getProblem_id();

        if (userId == null || problemTitle == null || topic == null) {
            throw new ApiException("user_id, problem_title, and topic are required", HttpStatus.BAD_REQUEST);
        }

        SessionService.ActiveSession session = sessionService.getActiveSession(userId);
        if (problemTitle.isBlank() && session != null) {
            problemTitle = session.getProblemTitle();
        }
        if (topic.isBlank() && session != null) {
            topic = session.getTopic();
        }
        if ((problemId == null || problemId.isBlank()) && session != null) {
            problemId = session.getProblemId();
        }

        String prompt = Constants.getHintGatePrompt(problemTitle, topic);
        String raw = llmService.jsonCompletion(
                "You generate concept questions for coding problems. Always respond with valid JSON only.",
                prompt
        );

        List<String> questions = null;
        try {
            String clean = raw.trim().replaceAll("^```json|```$", "").trim();
            JsonNode root = objectMapper.readTree(clean);
            if (root.has("questions") && root.get("questions").isArray()) {
                questions = new ArrayList<>();
                for (JsonNode qNode : root.get("questions")) {
                    questions.add(qNode.asText());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse hint gate questions: {}", raw, e);
        }

        // Fallback questions
        if (questions == null || questions.size() < 2) {
            questions = List.of(
                    "What is the core concept behind solving a " + topic + " problem?",
                    "What would be your first step to approach this " + topic + " problem?"
            );
        } else {
            questions = questions.subList(0, 2);
        }

        ActiveHintGate gate = new ActiveHintGate();
        gate.setQuestions(questions);
        gate.setAnswersPassed(new ArrayList<>(List.of(false, false)));
        gate.setAttempts(new ArrayList<>(List.of(0, 0)));
        gate.setProblemTitle(problemTitle);
        gate.setTopic(topic);
        gate.setProblemId(problemId);

        hintGates.put(userId, gate);

        // Persist hint request
        if (problemId != null && !problemId.isBlank()) {
            HintUsage usage = HintUsage.builder()
                    .userId(userId)
                    .problemId(problemId)
                    .problemTitle(problemTitle)
                    .topic(topic)
                    .questions(questions)
                    .answersPassed(List.of(false, false))
                    .attempts(List.of(0, 0))
                    .status("gate_opened")
                    .build();
            hintUsageRepository.save(usage);
        }

        return HintDto.HintGateResponse.builder()
                .status("gate_opened")
                .questions(questions)
                .message("Answer both questions correctly to earn your hint.")
                .build();
    }

    public HintDto.AnswerResponse answerHintGate(HintDto.AnswerRequest request) {
        String userId = request.getUser_id();
        Integer questionIndex = request.getQuestion_index();
        String answer = request.getAnswer();

        if (userId == null) {
            throw new ApiException("user_id is required", HttpStatus.BAD_REQUEST);
        }

        ActiveHintGate gate = hintGates.get(userId);
        if (gate == null) {
            throw new ApiException("No active hint gate. Call /hint/request first.", HttpStatus.NOT_FOUND);
        }

        if (questionIndex == null || (questionIndex != 0 && questionIndex != 1)) {
            throw new ApiException("question_index must be 0 or 1", HttpStatus.BAD_REQUEST);
        }

        String question = gate.getQuestions().get(questionIndex);
        gate.getAttempts().set(questionIndex, gate.getAttempts().get(questionIndex) + 1);

        String gradePrompt = Constants.getGradeAnswerPrompt(question, answer, "concepts related to " + gate.getTopic());
        String raw = llmService.jsonCompletion(
                "You grade student answers to coding concept questions. Always respond with valid JSON only.",
                gradePrompt
        );

        boolean passed = false;
        String feedback = "Keep thinking about it!";

        try {
            String clean = raw.trim().replaceAll("^```json|```$", "").trim();
            JsonNode root = objectMapper.readTree(clean);
            if (root.has("passed")) {
                passed = root.get("passed").asBoolean();
            }
            if (root.has("feedback")) {
                feedback = root.get("feedback").asText();
            }
        } catch (Exception e) {
            log.error("Failed to parse grade answer: {}", raw, e);
        }

        gate.getAnswersPassed().set(questionIndex, passed);
        boolean bothPassed = gate.getAnswersPassed().stream().allMatch(Boolean::booleanValue);

        String hint = null;

        if (bothPassed) {
            SessionService.ActiveSession session = sessionService.getActiveSession(userId);
            if (session != null) {
                session.setHintsEarned(session.getHintsEarned() + 1);
            }

            // Generate unique hint
            List<String> previousHints = hintUsageRepository.findByUserIdAndProblemIdAndStatus(userId, gate.getProblemId(), "unlocked")
                    .stream()
                    .map(HintUsage::getHint)
                    .filter(h -> h != null && !h.isBlank())
                    .collect(Collectors.toList());

            String hintPrompt = Constants.getDirectHintPrompt(gate.getProblemTitle(), gate.getTopic(), previousHints);
            hint = llmService.jsonCompletion(
                    "You are a Socratic coding mentor giving a brief, helpful hint.",
                    hintPrompt
            ).trim();

            hintGates.remove(userId);
        }

        // Update persistent record
        if (gate.getProblemId() != null) {
            Optional<HintUsage> latestRecordOpt = hintUsageRepository.findFirstByUserIdAndProblemIdAndStatusOrderByCreatedAtDesc(
                    userId, gate.getProblemId(), "gate_opened"
            );
            if (latestRecordOpt.isPresent()) {
                HintUsage usage = latestRecordOpt.get();
                usage.setAnswersPassed(gate.getAnswersPassed());
                usage.setAttempts(gate.getAttempts());
                if (bothPassed) {
                    usage.setStatus("unlocked");
                    usage.setHint(hint);
                }
                hintUsageRepository.save(usage);
            }
        }

        if (bothPassed) {
            return HintDto.AnswerResponse.builder()
                    .status("hint_earned")
                    .passed(true)
                    .feedback(feedback)
                    .hint_unlocked(true)
                    .hint(hint)
                    .message("Well done! You have earned your hint. Here it is:")
                    .build();
        }

        return HintDto.AnswerResponse.builder()
                .status("answer_graded")
                .passed(passed)
                .feedback(feedback)
                .hint_unlocked(false)
                .questions_passed(gate.getAnswersPassed())
                .message("Keep going — answer both questions to unlock the hint.")
                .build();
    }

    public String legacyHintGate(String userId, String message) {
        SessionService.ActiveSession session = sessionService.getActiveSession(userId);
        if (session == null) {
            throw new ApiException("No active session", HttpStatus.BAD_REQUEST);
        }

        String systemPrompt = """
                You are a coding mentor.
                Before giving a hint, ask 2 short diagnostic questions to understand:
                - What the student has tried
                - Where they are stuck
                - What they think is the next step
                
                Rules:
                - Do NOT give hint yet
                - Keep it concise
                - Questions only
                """;

        return llmService.jsonCompletion(systemPrompt, message);
    }

    public String legacyGenerateHint(String userId, String answers) {
        SessionService.ActiveSession session = sessionService.getActiveSession(userId);
        if (session == null) {
            throw new ApiException("No active session", HttpStatus.BAD_REQUEST);
        }

        session.setHintsTaken(session.getHintsTaken() + 1);

        String systemPrompt = String.format("""
                You are a coding mentor.
                
                Context:
                - Problem: %s
                - Topic: %s
                - Past mistakes: %s
                
                Based on student's answers:
                %%s
                
                Rules:
                - Give ONLY a small hint (not full solution)
                - Guide thinking, not answer directly
                - Keep it short (2-4 lines)
                """, session.getProblemTitle(), session.getTopic(), session.getMemory() != null ? session.getMemory() : "none");

        return llmService.jsonCompletion(String.format(systemPrompt, answers), "Generate a helpful hint");
    }

    public int legacyMarkHintEarned(String userId) {
        SessionService.ActiveSession session = sessionService.getActiveSession(userId);
        if (session == null) {
            throw new ApiException("No active session", HttpStatus.BAD_REQUEST);
        }
        session.setHintsEarned(session.getHintsEarned() + 1);
        return session.getHintsEarned();
    }

    @Data
    public static class ActiveHintGate {
        private List<String> questions;
        private List<Boolean> answersPassed;
        private List<Integer> attempts;
        private String problemTitle;
        private String topic;
        private String problemId;
    }
}
