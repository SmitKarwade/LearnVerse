package com.example.learnverse.auth.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "app.admin")
@Validated
public class AdminProperties {

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String email;

    @NotBlank(message = "Admin password is required")
    @Size(min = 3, message = "Admin password must be at least 3 characters")
    private String password;

    @NotBlank(message = "Admin name is required")
    private String name;
}