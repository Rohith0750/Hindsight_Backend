package com.hindsight.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class SubmissionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunCodeRequest {
        private String code;
        private String language;
        private String stdin;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunCodeResponse {
        private String status;
        private String result;
        private String stdout;
        private String stderr;
        private Double time;
        private Integer memory;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeRequest {
        private String user_id;
        private String problem_id;
        private String code;
        private String language;
        private boolean is_dry_run = false;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeResponse {
        private String submissionId;
        private String verdict;
        private long passed;
        private long total;
        private String runtime;
        private String memory;
        private List<TestCaseResult> testResults;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private int testCase;
        private boolean passed;
        private String status;
        private String input;
        private String expected;
        private String actual;
        private String stderr;
    }
}
