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
@Document(collection = "hintusages")
public class HintUsage {

    @Id
    private String id;

    @Field("user_id")
    private String userId;

    @Field("problem_id")
    private String problemId;

    @Field("problem_title")
    private String problemTitle;

    private String topic;

    @Builder.Default
    private List<String> questions = new ArrayList<>();

    @Field("answers_passed")
    @Builder.Default
    private List<Boolean> answersPassed = new ArrayList<>();

    @Builder.Default
    private List<Integer> attempts = new ArrayList<>();

    @Builder.Default
    private String status = "gate_opened"; // gate_opened, unlocked

    private String hint;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
