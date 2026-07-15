package com.hindsight.backend.repository;

import com.hindsight.backend.model.CrimeLocation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrimeLocationRepository extends MongoRepository<CrimeLocation, String> {
}
