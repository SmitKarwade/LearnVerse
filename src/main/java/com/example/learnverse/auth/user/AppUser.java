package com.example.learnverse.auth.user;

import com.example.learnverse.auth.modelenum.Role;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {

    @Id
    private String id;

    // Authentication Info
    private String email;

    private String passwordHash;
    private Role role;
    private Boolean isActive;
    private Date createdAt;
    private Date updatedAt;

    // Basic Info (for backward compatibility)
    private String name;
    private List<String> interests;

    // Enhanced Profile
    private UserProfile profile;
    private Boolean profileCompleted;

    // AI Assistant Settings
    private Boolean aiAssistantEnabled;
}