package com.hindsight.backend.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class JudgeService {

    private final WebClient judgeWebClient;

    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
            "python", 71,
            "javascript", 63,
            "java", 62,
            "cpp", 54,
            "c", 50
    );

    public JudgeService(WebClient judgeWebClient) {
        this.judgeWebClient = judgeWebClient;
    }

    public Integer getLanguageId(String language) {
        if (language == null) return null;
        return LANGUAGE_IDS.get(language.toLowerCase());
    }

    public ExecutionResult runTestCase(String code, String language, String stdin) {
        Integer langId = getLanguageId(language);
        if (langId == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }

        String encodedCode = Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8));
        String encodedStdin = Base64.getEncoder().encodeToString((stdin != null ? stdin : "").getBytes(StandardCharsets.UTF_8));

        JudgeRequest request = JudgeRequest.builder()
                .source_code(encodedCode)
                .language_id(langId)
                .stdin(encodedStdin)
                .base64_encoded(true)
                .wait(true)
                .build();

        try {
            JudgeResponse response = judgeWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/submissions")
                            .queryParam("base64_encoded", "true")
                            .queryParam("wait", "true")
                            .build())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JudgeResponse.class)
                    .onErrorResume(e -> {
                        log.error("Judge0 API execution error: ", e);
                        return Mono.empty();
                    })
                    .block();

            if (response != null) {
                String stdout = decodeBase64(response.getStdout());
                String stderr = decodeBase64(response.getStderr());
                String compileOutput = decodeBase64(response.getCompile_output());

                String finalStderr = stderr != null && !stderr.isBlank() ? stderr : compileOutput;

                String statusDesc = response.getStatus() != null ? response.getStatus().getDescription() : "Unknown";

                return ExecutionResult.builder()
                        .stdout(stdout)
                        .stderr(finalStderr)
                        .statusDesc(statusDesc)
                        .time(response.getTime())
                        .memory(response.getMemory())
                        .build();
            }
        } catch (Exception e) {
            log.error("Error executing code via JudgeService", e);
        }

        return ExecutionResult.builder()
                .stdout("")
                .stderr("Judge0 execution failed to respond.")
                .statusDesc("Error")
                .time(0.0)
                .memory(0)
                .build();
    }

    private String decodeBase64(String base64Str) {
        if (base64Str == null || base64Str.isBlank()) {
            return "";
        }
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64Str.trim());
            return new String(decodedBytes, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            log.warn("Failed to decode base64 string: {}", base64Str, e);
            return base64Str;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeRequest {
        private String source_code;
        private Integer language_id;
        private String stdin;
        private boolean base64_encoded;
        @Builder.Default
        private boolean wait = true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeResponse {
        private String stdout;
        private String stderr;
        private String compile_output;
        private Double time;
        private Integer memory;
        private Status status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Status {
        private Integer id;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionResult {
        private String stdout;
        private String stderr;
        private String statusDesc;
        private Double time;
        private Integer memory;
    }
}
