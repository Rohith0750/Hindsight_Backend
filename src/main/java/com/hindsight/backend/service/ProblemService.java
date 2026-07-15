package com.hindsight.backend.service;

import com.hindsight.backend.dto.ProblemDto;
import com.hindsight.backend.exception.ApiException;
import com.hindsight.backend.model.HintUsage;
import com.hindsight.backend.model.Problem;
import com.hindsight.backend.model.Submission;
import com.hindsight.backend.repository.ProblemRepository;
import com.hindsight.backend.repository.SubmissionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;
    private final MongoTemplate mongoTemplate;

    public ProblemService(ProblemRepository problemRepository,
                          SubmissionRepository submissionRepository,
                          MongoTemplate mongoTemplate) {
        this.problemRepository = problemRepository;
        this.submissionRepository = submissionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    private String slugify(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public List<Problem> createProblems(List<Problem> problems) {
        for (Problem p : problems) {
            p.setSlug(slugify(p.getTitle()));
        }
        return problemRepository.saveAll(problems);
    }

    public Problem createProblem(Problem problem) {
        problem.setSlug(slugify(problem.getTitle()));
        return problemRepository.save(problem);
    }

    public List<ProblemDto.ProblemResponse> getProblems(String topic, String difficulty, String search) {
        Query query = new Query();

        if (topic != null && !topic.isBlank()) {
            query.addCriteria(Criteria.where("topic").is(topic));
        }
        if (difficulty != null && !difficulty.isBlank()) {
            query.addCriteria(Criteria.where("difficulty").is(difficulty.toLowerCase()));
        }
        if (search != null && !search.isBlank()) {
            query.addCriteria(Criteria.where("title").regex(search, "i"));
        }

        query.with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(50);
        List<Problem> problems = mongoTemplate.find(query, Problem.class);

        return problems.stream().map(p -> ProblemDto.ProblemResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .difficulty(capitalize(p.getDifficulty()))
                .tags(p.getTags())
                .acceptance(0) // Default Node.js value
                .aiPriority("Medium")
                .status("unsolved")
                .build()
        ).collect(Collectors.toList());
    }

    public Problem getProblemBySlug(String slug) {
        return problemRepository.findBySlug(slug)
                .orElseThrow(() -> new ApiException("Problem not found", HttpStatus.NOT_FOUND));
    }

    public Problem updateProblem(String id, Problem request) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ApiException("Problem not found", HttpStatus.NOT_FOUND));

        problem.setTitle(request.getTitle());
        problem.setSlug(slugify(request.getTitle()));
        problem.setDescription(request.getDescription());
        problem.setStarterCode(request.getStarterCode());
        problem.setTopic(request.getTopic());
        problem.setDifficulty(request.getDifficulty());
        problem.setTags(request.getTags());
        problem.setConstraints(request.getConstraints());
        problem.setExamples(request.getExamples());
        problem.setTestCases(request.getTestCases());
        problem.setFunctionName(request.getFunctionName());

        return problemRepository.save(problem);
    }

    public void deleteProblem(String id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new ApiException("Problem not found", HttpStatus.NOT_FOUND));
        problemRepository.delete(problem);
    }

    public List<ProblemDto.ProblemResponse> getRecommendedProblems(String userId) {
        try {
            ObjectId userObjectId = new ObjectId(userId);

            // 1. Aggregate submission failure counts per topic
            Aggregation subAgg = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("user_id").is(userObjectId)),
                    Aggregation.lookup("problems", "problem_id", "_id", "problem"),
                    Aggregation.unwind("problem"),
                    Aggregation.group("problem.topic")
                            .sum(ConditionalOperators.when(Criteria.where("status").ne("accepted"))
                                    .then(1)
                                    .otherwise(0)).as("wrongCount")
                            .count().as("totalCount")
            );

            AggregationResults<SubmissionAggResult> subResults = mongoTemplate.aggregate(
                    subAgg, "submissions", SubmissionAggResult.class);

            // 2. Aggregate hint usage counts per topic
            Aggregation hintAgg = Aggregation.newAggregation(
                    Aggregation.match(Criteria.where("user_id").is(userObjectId)),
                    Aggregation.group("topic").count().as("hintsCount")
            );

            AggregationResults<HintAggResult> hintResults = mongoTemplate.aggregate(
                    hintAgg, "hintusages", HintAggResult.class);

            // 3. Calculate weakness scores
            Map<String, Double> topicWeaknessScores = new HashMap<>();

            for (SubmissionAggResult r : subResults.getMappedResults()) {
                double score = ((double) r.getWrongCount() / Math.max(r.getTotalCount(), 1)) * 5.0;
                topicWeaknessScores.put(r.getId(), score);
            }

            for (HintAggResult r : hintResults.getMappedResults()) {
                double score = topicWeaknessScores.getOrDefault(r.getId(), 0.0) + (r.getHintsCount() * 1.5);
                topicWeaknessScores.put(r.getId(), score);
            }

            // Find best topic
            String bestTopic = "arrays";
            double maxScore = 0.0;
            for (Map.Entry<String, Double> entry : topicWeaknessScores.entrySet()) {
                if (entry.getValue() > maxScore) {
                    maxScore = entry.getValue();
                    bestTopic = entry.getKey();
                }
            }

            String aiReason;
            if (maxScore > 5.0) {
                aiReason = "Based on your recent struggles in " + bestTopic;
            } else if (maxScore > 0.0) {
                aiReason = "Strengthen your foundations in " + bestTopic;
            } else {
                aiReason = "Ready to explore " + bestTopic + "?";
            }

            // 4. Retrieve solved problem IDs (status accepted)
            List<String> solvedProblemIds = submissionRepository.findByUserIdAndStatus(userId, "accepted")
                    .stream()
                    .map(Submission::getProblemId)
                    .distinct()
                    .collect(Collectors.toList());

            List<ObjectId> solvedObjectIds = solvedProblemIds.stream()
                    .map(ObjectId::new)
                    .collect(Collectors.toList());

            // 5. Query problems not in solved problems lists under bestTopic and difficulty medium
            Query query = new Query();
            query.addCriteria(Criteria.where("topic").is(bestTopic));
            query.addCriteria(Criteria.where("difficulty").is("medium"));
            if (!solvedObjectIds.isEmpty()) {
                query.addCriteria(Criteria.where("_id").nin(solvedObjectIds));
            }
            query.limit(3);

            List<Problem> recommendedProblems = mongoTemplate.find(query, Problem.class);

            return recommendedProblems.stream().map(p -> ProblemDto.ProblemResponse.builder()
                    .id(p.getId())
                    .title(p.getTitle())
                    .slug(p.getSlug())
                    .difficulty(capitalize(p.getDifficulty()))
                    .tags(p.getTags())
                    .aiReason(aiReason)
                    .build()
            ).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to generate problem recommendations", e);
            throw new ApiException("Failed to fetch recommended problems", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionAggResult {
        private String id; // topic
        private int wrongCount;
        private int totalCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HintAggResult {
        private String id; // topic
        private int hintsCount;
    }
}
