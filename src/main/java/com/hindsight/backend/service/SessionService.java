package com.hindsight.backend.service;

import com.hindsight.backend.dto.SessionDto;
import com.hindsight.backend.exception.ApiException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionService {

    private final MemoryService memoryService;
    private final Map<String, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    public SessionService(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public ActiveSession getActiveSession(String userId) {
        return activeSessions.get(userId);
    }

    public SessionDto.SessionResponse startSession(SessionDto.StartSessionRequest request) {
        String userId = request.getUser_id();
        String problemTitle = request.getProblem_title();
        String topic = request.getTopic();
        String problemId = request.getProblem_id();

        if (userId == null || problemTitle == null || topic == null) {
            throw new ApiException("user_id, problem_title and topic are required", HttpStatus.BAD_REQUEST);
        }

        String memory = memoryService.readMemory(userId);

        ActiveSession session = ActiveSession.builder()
                .memory(memory)
                .problemId(problemId)
                .problemTitle(problemTitle)
                .topic(topic)
                .hintsTaken(0)
                .hintsEarned(0)
                .build();

        activeSessions.put(userId, session);

        String preview = (memory != null && !memory.isBlank())
                ? (memory.length() > 200 ? memory.substring(0, 200) : memory)
                : "No previous sessions found.";

        return SessionDto.SessionResponse.builder()
                .status("session started")
                .user_id(userId)
                .has_memory(memory != null && !memory.isBlank())
                .memory_preview(preview)
                .build();
    }

    public SessionDto.SessionEndResponse endSession(SessionDto.EndSessionRequest request) {
        String userId = request.getUser_id();
        if (userId == null) {
            throw new ApiException("user_id is required", HttpStatus.BAD_REQUEST);
        }

        String summary = String.format("""
                Session summary:
                - Problem attempted: %s
                - Topic: %s
                - Language used: %s
                - Hints taken without earning: %d
                - Hints properly earned: %d
                - Mistakes observed: %s
                """,
                request.getProblem_title(),
                request.getTopic(),
                request.getLanguage(),
                request.getHints_taken() != null ? request.getHints_taken() : 0,
                request.getHints_earned() != null ? request.getHints_earned() : 0,
                request.getMistakes() != null ? String.join(", ", request.getMistakes()) : "none recorded"
        ).trim();

        boolean success = memoryService.writeMemory(userId, summary);
        activeSessions.remove(userId);

        return SessionDto.SessionEndResponse.builder()
                .status("session ended")
                .memory_saved(success)
                .build();
    }

    public SessionDto.SessionStatusResponse getSessionStatus(String userId) {
        ActiveSession session = activeSessions.get(userId);
        if (session == null) {
            throw new ApiException("No active session for this user", HttpStatus.NOT_FOUND);
        }

        return SessionDto.SessionStatusResponse.builder()
                .user_id(userId)
                .problem_title(session.getProblemTitle())
                .topic(session.getTopic())
                .hints_taken(session.getHintsTaken())
                .hints_earned(session.getHintsEarned())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveSession {
        private String memory;
        private String problemId;
        private String problemTitle;
        private String topic;
        private int hintsTaken;
        private int hintsEarned;
    }
}
