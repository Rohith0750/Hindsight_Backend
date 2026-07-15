package com.hindsight.backend.repository;

import com.hindsight.backend.model.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {
    List<Submission> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Submission> findByUserIdAndProblemIdOrderByAttemptNumberAsc(String userId, String problemId);
    Optional<Submission> findFirstByUserIdAndProblemIdOrderByAttemptNumberDesc(String userId, String problemId);
    long countByUserId(String userId);
    long countByUserIdAndStatus(String userId, String status);
    List<Submission> findByUserIdAndStatus(String userId, String status);
    List<Submission> findByUserIdAndStatusAndCreatedAtGreaterThanEqual(String userId, String status, Instant since);
    List<Submission> findByUserId(String userId);
}
