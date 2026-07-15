package com.hindsight.backend.repository;

import com.hindsight.backend.model.Sos;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SosRepository extends MongoRepository<Sos, String> {
}
