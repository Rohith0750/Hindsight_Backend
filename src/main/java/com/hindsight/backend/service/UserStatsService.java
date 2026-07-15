package com.hindsight.backend.service;

import com.hindsight.backend.dto.StatsDto;
import com.hindsight.backend.model.Problem;
import com.hindsight.backend.model.Submission;
import com.hindsight.backend.model.User;
import com.hindsight.backend.repository.HintUsageRepository;
import com.hindsight.backend.repository.ProblemRepository;
import com.hindsight.backend.repository.SubmissionRepository;
import com.hindsight.backend.repository.UserRepository;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserStatsService {

    private final SubmissionRepository submissionRepository;
    private final HintUsageRepository hintUsageRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public UserStatsService(SubmissionRepository submissionRepository,
                            HintUsageRepository hintUsageRepository,
                            ProblemRepository problemRepository,
                            UserRepository userRepository,
                            MongoTemplate mongoTemplate) {
        this.submissionRepository = submissionRepository;
        this.hintUsageRepository = hintUsageRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public StatsDto.UserStatsResponse getUserStats(String userId) {
        long totalAttempts = submissionRepository.countByUserId(userId);
        long acceptedAttempts = submissionRepository.countByUserIdAndStatus(userId, "accepted");

        List<Submission> acceptedSubmissions = submissionRepository.findByUserIdAndStatus(userId, "accepted");

        List<String> solvedProblems = acceptedSubmissions.stream()
                .map(Submission::getProblemId)
                .distinct()
                .collect(Collectors.toList());

        long hintsUsed = hintUsageRepository.findByUserId(userId).size();
        long hintsEarned = hintUsageRepository.findByUserId(userId).stream()
                .filter(h -> "unlocked".equalsIgnoreCase(h.getStatus()))
                .count();

        // Streak calculation in Java to avoid MongoDB DateToString aggregation error
        List<LocalDate> sortedDates = acceptedSubmissions.stream()
                .map(Submission::getCreatedAt)
                .filter(Objects::nonNull)
                .map(instant -> instant.atZone(ZoneId.systemDefault()).toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int streak = 0;
        if (!sortedDates.isEmpty()) {
            streak = 1;
            LocalDate prev = sortedDates.get(0);
            for (int i = 1; i < sortedDates.size(); i++) {
                LocalDate curr = sortedDates.get(i);
                long diff = ChronoUnit.DAYS.between(curr, prev);
                if (diff == 1) {
                    streak++;
                } else if (diff > 1) {
                    break;
                }
                prev = curr;
            }
        }

        int accuracy = totalAttempts > 0 ? (int) Math.round(((double) acceptedAttempts / totalAttempts) * 100) : 0;

        User user = userRepository.findById(userId).orElse(null);
        int xp = user != null ? user.getXp() : 0;

        return StatsDto.UserStatsResponse.builder()
                .problemsSolved(solvedProblems.size())
                .accuracy(accuracy)
                .streak(streak)
                .xp(xp)
                .hintsUsed(hintsUsed)
                .hintsEarned(hintsEarned)
                .build();
    }

    public StatsDto.InsightBannerResponse getInsightBanner(String userId) {
        ObjectId userObjectId = new ObjectId(userId);

        // 1. Group failed submissions by topic
        Aggregation failAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user_id").is(userObjectId).and("status").ne("accepted")),
                Aggregation.lookup("problems", "problem_id", "_id", "problem"),
                Aggregation.unwind("problem"),
                Aggregation.group("problem.topic").count().as("count")
        );
        AggregationResults<CountGroupResult> failResults = mongoTemplate.aggregate(failAgg, "submissions", CountGroupResult.class);

        // 2. Group hint usages by topic
        Aggregation hintAgg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user_id").is(userObjectId)),
                Aggregation.group("topic").count().as("count")
        );
        AggregationResults<CountGroupResult> hintResults = mongoTemplate.aggregate(hintAgg, "hintusages", CountGroupResult.class);

        // Combine scores
        Map<String, Integer> scores = new HashMap<>();
        for (CountGroupResult r : failResults.getMappedResults()) {
            scores.put(r.getId(), r.getCount());
        }
        for (CountGroupResult r : hintResults.getMappedResults()) {
            scores.put(r.getId(), scores.getOrDefault(r.getId(), 0) + r.getCount() * 2);
        }

        String bestStruggle = null;
        int maxScore = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestStruggle = entry.getKey();
            }
        }

        if (bestStruggle != null) {
            return StatsDto.InsightBannerResponse.builder()
                    .message("You're focusing hard on " + bestStruggle + ". Keep practicing to master it!")
                    .topic(bestStruggle)
                    .build();
        } else {
            return StatsDto.InsightBannerResponse.builder()
                    .message("You haven't struggled much yet. Ready for a harder challenge?")
                    .topic("general")
                    .build();
        }
    }

    public List<StatsDto.UserActivityResponse> getUserActivities(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("user_id").is(new ObjectId(userId)));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(10);

        List<Submission> submissions = mongoTemplate.find(query, Submission.class);

        return submissions.stream().map(s -> {
            Problem p = problemRepository.findById(s.getProblemId()).orElse(null);
            return StatsDto.UserActivityResponse.builder()
                    .problem(p != null ? p.getTitle() : "Unknown Problem")
                    .verdict(s.getStatus())
                    .language(s.getLanguage())
                    .date(s.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    public List<StatsDto.LanguageProficiencyResponse> getUserProficiency(String userId) {
        ObjectId userObjectId = new ObjectId(userId);
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("user_id").is(userObjectId).and("status").is("accepted")),
                Aggregation.group("language").count().as("count")
        );
        AggregationResults<CountGroupResult> results = mongoTemplate.aggregate(agg, "submissions", CountGroupResult.class);

        return results.getMappedResults().stream()
                .map(r -> {
                    String langName = r.getId();
                    // Capitalize first letter of language for visual presentation
                    if (langName != null && !langName.isEmpty()) {
                        langName = langName.substring(0, 1).toUpperCase() + langName.substring(1).toLowerCase();
                    }
                    int solvedCount = r.getCount();
                    int profPercent = Math.min(solvedCount * 10, 100);
                    return StatsDto.LanguageProficiencyResponse.builder()
                            .language(r.getId())
                            .solved(solvedCount)
                            .name(langName)
                            .proficiency(profPercent)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<StatsDto.StreakActivityResponse> getUserStreakActivity(String userId) {
        Instant since = Instant.now().minus(35, ChronoUnit.DAYS);
        List<Submission> submissions = submissionRepository.findByUserIdAndStatusAndCreatedAtGreaterThanEqual(userId, "accepted", since);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Integer> solvedCountMap = new HashMap<>();

        for (Submission s : submissions) {
            if (s.getCreatedAt() != null) {
                String dateStr = s.getCreatedAt().atZone(ZoneId.systemDefault()).format(formatter);
                solvedCountMap.put(dateStr, solvedCountMap.getOrDefault(dateStr, 0) + 1);
            }
        }

        List<StatsDto.StreakActivityResponse> history = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 35; i >= 0; i--) {
            LocalDate d = now.minusDays(i);
            String dateStr = d.format(formatter);
            history.add(StatsDto.StreakActivityResponse.builder()
                    .date(dateStr)
                    .solved(solvedCountMap.getOrDefault(dateStr, 0))
                    .frozen(false)
                    .build());
        }

        return history;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateGroupResult {
        private String id; // date string YYYY-MM-DD
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountGroupResult {
        private String id;
        private int count;
    }
}
