package com.hindsight.backend.repository;

import com.hindsight.backend.model.Problem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemRepository extends MongoRepository<Problem, String> {
    Optional<Problem> findBySlug(String slug);
    List<Problem> findByTopic(String topic);
    List<Problem> findByDifficulty(String difficulty);
    List<Problem> findByTopicAndDifficulty(String topic, String difficulty);
    List<Problem> findByTitleRegexIgnoreCase(String titleRegex);
    List<Problem> findByTopicAndTitleRegexIgnoreCase(String topic, String titleRegex);
    List<Problem> findByDifficultyAndTitleRegexIgnoreCase(String difficulty, String titleRegex);
    List<Problem> findByTopicAndDifficultyAndTitleRegexIgnoreCase(String topic, String difficulty, String titleRegex);
}
