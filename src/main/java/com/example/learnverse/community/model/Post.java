package com.example.learnverse.community.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    @Id
    private String id;

    private String authorId;
    private String authorName;
    private String authorType;

    private String content;
    private String mediaUrl;
    private String mediaType;

    private Set<String> likedBy = new HashSet<>();
    private List<String> shares = new ArrayList<>();

    private List<Comment> comments = new ArrayList<>();
    private int commentsCount = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Comment {
        private String id;
        private String authorId;
        private String authorName;
        private String authorType;
        private String content;
        private Set<String> likedBy = new HashSet<>();
        private LocalDateTime createdAt;


        public Comment(String authorId, String authorName, String authorType, String content) {
            this.id = UUID.randomUUID().toString();
            this.authorId = authorId;
            this.authorName = authorName;
            this.authorType = authorType;
            this.content = content;
            this.createdAt = LocalDateTime.now();
        }
    }
}
