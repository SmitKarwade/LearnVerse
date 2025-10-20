package com.example.learnverse.community.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "follows")
@CompoundIndex(name = "follower_following", def = "{'followerId': 1, 'followingId': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Follow {
    @Id
    private String id;

    private String followerId;      // User who is following
    private String followingId;     // Tutor being followed

    @CreatedDate
    private LocalDateTime followedAt;
}
