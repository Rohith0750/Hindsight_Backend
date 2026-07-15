package com.hindsight.backend.service;

import com.hindsight.backend.dto.StatsDto;
import com.hindsight.backend.repository.HintUsageRepository;
import com.hindsight.backend.repository.SubmissionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProgressService {

    private final SubmissionRepository submissionRepository;
    private final HintUsageRepository hintUsageRepository;
    private final MongoTemplate mongoTemplate;

    public ProgressService(SubmissionRepository submissionRepository,
                           HintUsageRepository hintUsageRepository,
                           MongoTemplate mongoTemplate) {
        this.submissionRepository = submissionRepository;
        this.hintUsageRepository = hintUsageRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public StatsDto.ProgressOverviewResponse getProgressOverview(String userId) {
        long total = submissionRepository.countByUserId(userId);
        long accepted = submissionRepository.countByUserIdAndStatus(userId, "accepted");

        long hintsUsed = hintUsageRepository.findByUserId(userId).size();
        long hintsEarned = hintUsageRepository.findByUserId(userId).stream()
                .filter(h -> "unlocked".equalsIgnoreCase(h.getStatus()))
                .count();

        int accuracy = total > 0 ? (int) Math.round(((double) accepted / total) * 100) : 0;

        return StatsDto.ProgressOverviewResponse.builder()
                .hintsUsed(hintsUsed)
                .hintsEarned(hintsEarned)
                .accuracy(accuracy)
                .build();
    }

    public List<StatsDto.MistakePatternResponse> getMistakePatterns(String userId) {
        ObjectId userObjectId = new ObjectId(userId);

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user_id").is(userObjectId)),
                Aggregation.unwind("mistakes"),
                Aggregation.group("mistakes").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count"),
                Aggregation.limit(5)
        );

        AggregationResults<MistakeAggResult> results = mongoTemplate.aggregate(
                agg, "submissions", MistakeAggResult.class
        );

        return results.getMappedResults().stream()
                .map(r -> StatsDto.MistakePatternResponse.builder()
                        .pattern(r.getId())
                        .count(r.getCount())
                        .label(r.getId() + " - " + r.getCount() + "x")
                        .build())
                .collect(Collectors.toList());
    }

    public List<StatsDto.LanguageProficiencyResponse> getProficiencyChart(String userId) {
        ObjectId userObjectId = new ObjectId(userId);

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user_id").is(userObjectId).and("status").is("accepted")),
                Aggregation.group("language").count().as("solvedCount")
        );

        AggregationResults<ProficiencyAggResult> results = mongoTemplate.aggregate(
                agg, "submissions", ProficiencyAggResult.class
        );

        return results.getMappedResults().stream()
                .map(r -> StatsDto.LanguageProficiencyResponse.builder()
                        .language(r.getId())
                        .solved(r.getSolvedCount())
                        .build())
                .collect(Collectors.toList());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MistakeAggResult {
        private String id; // mistake text
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProficiencyAggResult {
        private String id; // language
        private int solvedCount;
    }
}
