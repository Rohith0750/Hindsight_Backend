package com.hindsight.backend.controller;

import com.hindsight.backend.dto.ProblemDto;
import com.hindsight.backend.model.Problem;
import com.hindsight.backend.service.ProblemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/problem")
public class ProblemController {

    private final ProblemService problemService;

    private final ObjectMapper objectMapper;

    public ProblemController(ProblemService problemService, ObjectMapper objectMapper) {
        this.problemService = problemService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createProblem(@RequestBody Object data) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (data instanceof List) {
                List<Problem> problems = objectMapper.convertValue(
                        data,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Problem.class)
                );
                List<Problem> saved = problemService.createProblems(problems);
                response.put("success", true);
                response.put("data", saved);
            } else {
                Problem problem = objectMapper.convertValue(data, Problem.class);
                Problem saved = problemService.createProblem(problem);
                response.put("success", true);
                response.put("data", saved);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ProblemDto.ProblemResponse>> getProblems(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search) {
        List<ProblemDto.ProblemResponse> problems = problemService.getProblems(topic, difficulty, search);
        return ResponseEntity.ok(problems);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Problem> getProblemBySlug(@PathVariable String slug) {
        Problem problem = problemService.getProblemBySlug(slug);
        return ResponseEntity.ok(problem);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Problem> updateProblem(@PathVariable String id, @RequestBody Problem request) {
        Problem updated = problemService.updateProblem(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProblem(@PathVariable String id) {
        problemService.deleteProblem(id);
        return ResponseEntity.ok(Map.of("message", "Problem deleted successfully"));
    }
}
