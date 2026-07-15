package com.hindsight.backend.repository;

import com.hindsight.backend.model.HintUsage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HintUsageRepository extends MongoRepository<HintUsage, String> {
    Optional<HintUsage> findFirstByUserIdAndProblemIdAndStatusOrderByCreatedAtDesc(String userId, String problemId, String status);
    List<HintUsage> findByUserIdAndProblemIdAndStatus(String userId, String problemId, String status);
    List<HintUsage> findByUserId(String userId);
}
