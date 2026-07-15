package com.hindsight.backend.service;

import com.hindsight.backend.dto.SubmissionDto;
import com.hindsight.backend.exception.ApiException;
import com.hindsight.backend.model.Problem;
import com.hindsight.backend.model.Submission;
import com.hindsight.backend.model.User;
import com.hindsight.backend.repository.ProblemRepository;
import com.hindsight.backend.repository.SubmissionRepository;
import com.hindsight.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final JudgeService judgeService;

    public SubmissionService(SubmissionRepository submissionRepository,
                             ProblemRepository problemRepository,
                             UserRepository userRepository,
                             JudgeService judgeService) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.judgeService = judgeService;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", "")
                .replaceAll("\\[", "")
                .replaceAll("\\]", "")
                .trim();
    }

    public Submission createSubmission(Submission submission) {
        // Compute attempt number
        Optional<Submission> lastOpt = submissionRepository.findFirstByUserIdAndProblemIdOrderByAttemptNumberDesc(
                submission.getUserId(), submission.getProblemId()
        );
        int attempt = lastOpt.map(value -> value.getAttemptNumber() + 1).orElse(1);
        submission.setAttemptNumber(attempt);

        return submissionRepository.save(submission);
    }

    public List<Submission> getUserSubmissions(String userId) {
        return submissionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Submission> getProblemSubmissions(String userId, String problemId) {
        return submissionRepository.findByUserIdAndProblemIdOrderByAttemptNumberAsc(userId, problemId);
    }

    public Map<String, Object> getUserProgress(String userId) {
        List<String> solved = submissionRepository.findByUserIdAndStatus(userId, "accepted")
                .stream()
                .map(Submission::getProblemId)
                .distinct()
                .collect(Collectors.toList());

        long totalAttempts = submissionRepository.countByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("solved_count", solved.size());
        result.put("total_attempts", totalAttempts);
        return result;
    }

    public SubmissionDto.JudgeResponse judgeSubmission(SubmissionDto.JudgeRequest request) {
        String userId = request.getUser_id();
        String problemId = request.getProblem_id();
        String code = request.getCode();
        String language = request.getLanguage();
        boolean isDryRun = request.is_dry_run();

        if (problemId == null || code == null || language == null) {
            throw new ApiException("problem_id, code, and language are required", HttpStatus.BAD_REQUEST);
        }

        String langKey = language.toLowerCase();
        Integer langId = judgeService.getLanguageId(langKey);
        if (langId == null) {
            throw new ApiException("Unsupported language: " + language, HttpStatus.BAD_REQUEST);
        }

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ApiException("Problem not found", HttpStatus.NOT_FOUND));

        List<Problem.TestCase> testCases = problem.getTestCases();
        if (testCases == null || testCases.isEmpty()) {
            throw new ApiException("Problem has no test cases configured", HttpStatus.BAD_REQUEST);
        }

        List<SubmissionDto.TestCaseResult> results = new ArrayList<>();
        String errorMessage = null;
        double maxTime = 0.0;
        int maxMemory = 0;

        for (int i = 0; i < testCases.size(); i++) {
            Problem.TestCase tc = testCases.get(i);
            String wrappedCode = code;

            // Apply wrapper if function name exists
            if (problem.getFunctionName() != null && !problem.getFunctionName().isBlank()) {
                if ("python".equals(langKey)) {
                    wrappedCode = String.format("%s\n\nimport json, sys\ntry:\n    args = json.loads('''%s''')\n    print(json.dumps(%s(*args)))\nexcept Exception as e:\n    print(e, file=sys.stderr)\n    sys.exit(1)",
                            code, tc.getInput(), problem.getFunctionName());
                } else if ("javascript".equals(langKey)) {
                    wrappedCode = String.format("%s\n\ntry {\n    const args = JSON.parse('%s');\n    console.log(JSON.stringify(%s(...args)));\n} catch (err) {\n    console.error(err);\n    process.exit(1);\n}",
                            code, tc.getInput(), problem.getFunctionName());
                }
            }

            JudgeService.ExecutionResult execResult = judgeService.runTestCase(wrappedCode, langKey, tc.getInput());

            boolean passed = normalize(execResult.getStdout()).equals(normalize(tc.getOutput()));

            results.add(SubmissionDto.TestCaseResult.builder()
                    .testCase(i + 1)
                    .input(tc.getInput())
                    .expected(tc.getOutput())
                    .actual(execResult.getStdout())
                    .passed(passed)
                    .status(execResult.getStatusDesc())
                    .stderr(execResult.getStderr())
                    .build());

            if (execResult.getTime() != null) {
                maxTime = Math.max(maxTime, execResult.getTime() * 1000);
            }
            if (execResult.getMemory() != null) {
                maxMemory = Math.max(maxMemory, execResult.getMemory());
            }

            if ("Compilation Error".equalsIgnoreCase(execResult.getStatusDesc())) {
                errorMessage = execResult.getStderr();
                break;
            }
        }

        long passedCount = results.stream().filter(SubmissionDto.TestCaseResult::isPassed).count();
        long totalCount = testCases.size();
        boolean allPassed = passedCount == totalCount && errorMessage == null;

        String verdict = "wrong_answer";
        if (errorMessage != null) {
            verdict = "compile_error";
        } else if (allPassed) {
            verdict = "accepted";
        } else {
            boolean hasTle = results.stream().anyMatch(r -> "Time Limit Exceeded".equalsIgnoreCase(r.getStatus()));
            boolean hasRe = results.stream().anyMatch(r -> r.getStatus() != null && r.getStatus().toLowerCase().startsWith("runtime error"));
            if (hasTle) {
                verdict = "time_limit_exceeded";
            } else if (hasRe) {
                verdict = "runtime_error";
            }
        }

        String submissionId = null;
        if (!isDryRun) {
            Optional<Submission> lastOpt = submissionRepository.findFirstByUserIdAndProblemIdOrderByAttemptNumberDesc(userId, problemId);
            int attempt = lastOpt.map(submission -> submission.getAttemptNumber() + 1).orElse(1);

            Submission submission = Submission.builder()
                    .userId(userId)
                    .problemId(problemId)
                    .code(code)
                    .language(langKey)
                    .status(verdict)
                    .stdout(results.stream().map(SubmissionDto.TestCaseResult::getActual).collect(Collectors.joining("\n")))
                    .stderr(errorMessage != null ? errorMessage : "")
                    .executionTime((int) Math.round(maxTime))
                    .memory(maxMemory)
                    .attemptNumber(attempt)
                    .isSolved(allPassed)
                    .build();

            Submission saved = submissionRepository.save(submission);
            submissionId = saved.getId();

            // Update user stats
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    boolean alreadySolved = user.getSolvedProblems().contains(problemId);
                    int xpGain = allPassed ? (alreadySolved ? 10 : 60) : 0;

                    user.setTotalAttempts(user.getTotalAttempts() + 1);
                    if (xpGain > 0) {
                        user.setXp(user.getXp() + xpGain);

                        int newXp = user.getXp();
                        String newLevel = "Beginner";
                        if (newXp >= 4000) newLevel = "Expert";
                        else if (newXp >= 1500) newLevel = "Advanced";
                        else if (newXp >= 500) newLevel = "Intermediate";
                        user.setLevel(newLevel);

                        if (!alreadySolved) {
                            user.setSolvedCount(user.getSolvedCount() + 1);
                            user.getSolvedProblems().add(problemId);

                            // Update language proficiency
                            boolean langFound = false;
                            for (User.LanguageProficiency lp : user.getLanguages()) {
                                if (lp.getLanguage().equalsIgnoreCase(langKey)) {
                                    lp.setSolved(lp.getSolved() + 1);
                                    langFound = true;
                                    break;
                                }
                            }
                            if (!langFound) {
                                user.getLanguages().add(new User.LanguageProficiency(langKey, 1));
                            }
                        }
                    }
                    userRepository.save(user);
                }
            }
        }

        // Prepare public test results (hiding details of hidden test cases)
        List<SubmissionDto.TestCaseResult> publicResults = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            SubmissionDto.TestCaseResult r = results.get(i);
            Problem.TestCase originalTc = testCases.get(i);
            if (originalTc.isHidden()) {
                publicResults.add(SubmissionDto.TestCaseResult.builder()
                        .testCase(r.getTestCase())
                        .passed(r.isPassed())
                        .status(r.getStatus())
                        .build());
            } else {
                publicResults.add(r);
            }
        }

        return SubmissionDto.JudgeResponse.builder()
                .submissionId(submissionId)
                .verdict(verdict)
                .passed(passedCount)
                .total(totalCount)
                .runtime(maxTime > 0 ? Math.round(maxTime) + "ms" : "-")
                .memory(maxMemory > 0 ? String.format(Locale.US, "%.1fMB", (double) maxMemory / 1024) : "-")
                .testResults(publicResults)
                .errorMessage(errorMessage)
                .build();
    }
}
