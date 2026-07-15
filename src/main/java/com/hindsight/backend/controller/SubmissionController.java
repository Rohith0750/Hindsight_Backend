package com.hindsight.backend.controller;

import com.hindsight.backend.dto.SubmissionDto;
import com.hindsight.backend.model.Submission;
import com.hindsight.backend.service.SubmissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submission")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping("/judge")
    public ResponseEntity<SubmissionDto.JudgeResponse> judgeSubmission(@RequestBody SubmissionDto.JudgeRequest request) {
        SubmissionDto.JudgeResponse response = submissionService.judgeSubmission(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Submission> createSubmission(@RequestBody Submission request) {
        Submission submission = submissionService.createSubmission(request);
        return new ResponseEntity<>(submission, HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Submission>> getUserSubmissions(@PathVariable String userId) {
        List<Submission> submissions = submissionService.getUserSubmissions(userId);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/problem")
    public ResponseEntity<List<Submission>> getProblemSubmissions(
            @RequestParam("user_id") String userId,
            @RequestParam("problem_id") String problemId) {
        List<Submission> submissions = submissionService.getProblemSubmissions(userId, problemId);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/progress/{userId}")
    public ResponseEntity<Map<String, Object>> getUserProgress(@PathVariable String userId) {
        Map<String, Object> progress = submissionService.getUserProgress(userId);
        return ResponseEntity.ok(progress);
    }
}
