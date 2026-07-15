package com.hindsight.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "submissions")
public class Submission {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("problem_id")
    private String problemId;

    private String code;

    private String language; // cpp, python, javascript, java, c

    @Builder.Default
    private String status = "pending"; // pending, accepted, wrong_answer, runtime_error, compile_error, time_limit_exceeded

    private String stdout;

    private String stderr;

    @Field("execution_time")
    private Integer executionTime;

    private Integer memory;

    @Builder.Default
    private List<String> mistakes = new ArrayList<>();

    @Field("attempt_number")
    @Builder.Default
    private Integer attemptNumber = 1;

    @Field("is_first_solved")
    @Builder.Default
    private boolean isFirstSolved = false;

    @Field("is_solved")
    @Builder.Default
    private boolean isSolved = false;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
