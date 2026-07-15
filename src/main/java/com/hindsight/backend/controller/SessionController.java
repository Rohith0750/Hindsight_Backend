package com.hindsight.backend.controller;

import com.hindsight.backend.dto.SessionDto;
import com.hindsight.backend.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping({"/api/v1/session/start", "/api/v1/hindsight/start"})
    public ResponseEntity<SessionDto.SessionResponse> startSession(@RequestBody SessionDto.StartSessionRequest request) {
        SessionDto.SessionResponse response = sessionService.startSession(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping({"/api/v1/session/end", "/api/v1/hindsight/end"})
    public ResponseEntity<SessionDto.SessionEndResponse> endSession(@RequestBody SessionDto.EndSessionRequest request) {
        SessionDto.SessionEndResponse response = sessionService.endSession(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/api/v1/session/status/{userId}", "/api/v1/hindsight/status/{userId}"})
    public ResponseEntity<SessionDto.SessionStatusResponse> getSessionStatus(@PathVariable String userId) {
        SessionDto.SessionStatusResponse response = sessionService.getSessionStatus(userId);
        return ResponseEntity.ok(response);
    }
}
