package com.example.learnverse.auth.refresh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private String id;

    private String token;
    private String userId;
    private Instant expiryDate;
    private Instant createdAt;
    private String deviceInfo;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}