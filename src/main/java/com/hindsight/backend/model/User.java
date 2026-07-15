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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;
    
    private String name;
    
    @Builder.Default
    private String role = "user";

    @Builder.Default
    private Integer xp = 0;

    @Builder.Default
    private String level = "Beginner"; // Beginner | Intermediate | Advanced | Expert

    @Builder.Default
    private Integer streak = 0;

    @Builder.Default
    private Integer solvedCount = 0;

    @Builder.Default
    private Integer totalAttempts = 0;

    @Builder.Default
    private List<String> solvedProblems = new ArrayList<>();

    @Builder.Default
    private List<LanguageProficiency> languages = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LanguageProficiency {
        private String language;
        private Integer solved = 0;
    }
}
