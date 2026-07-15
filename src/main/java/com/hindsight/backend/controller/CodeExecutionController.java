package com.hindsight.backend.controller;

import com.hindsight.backend.dto.SubmissionDto;
import com.hindsight.backend.service.JudgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/code")
public class CodeExecutionController {

    private final JudgeService judgeService;

    public CodeExecutionController(JudgeService judgeService) {
        this.judgeService = judgeService;
    }

    @PostMapping("/run")
    public ResponseEntity<SubmissionDto.RunCodeResponse> runCode(@RequestBody SubmissionDto.RunCodeRequest request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(SubmissionDto.RunCodeResponse.builder()
                    .status("error")
                    .stderr("Code cannot be empty")
                    .build());
        }

        try {
            JudgeService.ExecutionResult result = judgeService.runTestCase(
                    request.getCode(),
                    request.getLanguage(),
                    request.getStdin()
            );

            SubmissionDto.RunCodeResponse response = SubmissionDto.RunCodeResponse.builder()
                    .status("success")
                    .result(result.getStatusDesc())
                    .stdout(result.getStdout())
                    .stderr(result.getStderr())
                    .time(result.getTime())
                    .memory(result.getMemory())
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(SubmissionDto.RunCodeResponse.builder()
                    .status("error")
                    .stderr(e.getMessage())
                    .build());
        }
    }
}
