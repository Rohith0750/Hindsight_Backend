package com.hindsight.backend.controller;

import com.hindsight.backend.dto.HintDto;
import com.hindsight.backend.service.HintService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/a1/hint")
public class HintController {

    private final HintService hintService;

    public HintController(HintService hintService) {
        this.hintService = hintService;
    }

    @PostMapping("/request")
    public ResponseEntity<HintDto.HintGateResponse> requestHint(@RequestBody HintDto.HintRequest request) {
        HintDto.HintGateResponse response = hintService.requestHint(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/answer")
    public ResponseEntity<HintDto.AnswerResponse> answerHintGate(@RequestBody HintDto.AnswerRequest request) {
        HintDto.AnswerResponse response = hintService.answerHintGate(request);
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Legacy Session-based endpoints
    // ─────────────────────────────────────────────────────────────────────────────

    @PostMapping("/gate")
    public ResponseEntity<Map<String, Object>> legacyHintGate(@RequestBody Map<String, String> body) {
        String userId = body.get("user_id");
        String message = body.get("message");
        String questions = hintService.legacyHintGate(userId, message);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "hint_gate_questions");
        response.put("questions", questions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> legacyGenerateHint(@RequestBody Map<String, String> body) {
        String userId = body.get("user_id");
        String answers = body.get("answers");
        String hint = hintService.legacyGenerateHint(userId, answers);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "hint");
        response.put("hint", hint);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/earned")
    public ResponseEntity<Map<String, Object>> legacyMarkHintEarned(@RequestBody Map<String, String> body) {
        String userId = body.get("user_id");
        int hintsEarned = hintService.legacyMarkHintEarned(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "hint earned");
        response.put("hints_earned", hintsEarned);
        return ResponseEntity.ok(response);
    }
}
