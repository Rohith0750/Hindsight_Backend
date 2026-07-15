package com.hindsight.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "problems")
public class Problem {

    @Id
    private String id;

    @Indexed(unique = true)
    private String title;

    @Indexed(unique = true)
    private String slug;

    private String description;

    @Field("starter_code")
    @Builder.Default
    private Map<String, String> starterCode = new HashMap<>();

    private String topic;

    @Builder.Default
    private String difficulty = "easy"; // easy, medium, hard

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private String constraints;

    @Builder.Default
    private List<Example> examples = new ArrayList<>();

    @Field("test_cases")
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    @Field("createdBy")
    private String createdBy;

    @Field("function_name")
    private String functionName;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Example {
        private String input;
        private String output;
        private String explanation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCase {
        private String input;
        private String output;
        @Field("is_hidden")
        private boolean isHidden = true;
    }
}
