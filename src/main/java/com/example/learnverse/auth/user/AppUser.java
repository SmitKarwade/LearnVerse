package com.example.learnverse.auth.user;

import com.example.learnverse.auth.modelenum.Role;
import com.mongodb.lang.Nullable;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "users")
public class AppUser {
    @Id
    private String id;

    private String name;
    private String email;
    private String passwordHash;
    private Role role;
    // Only for USER role - interests field
    @Nullable
    @Size(min = 3, max = 10, message = "User must have between 3-10 interests")
    private List<String> interests;
    private Instant createdAt;
}
