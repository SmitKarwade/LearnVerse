package com.example.learnverse.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UserInterestDto {
    @NotEmpty(message = "Interests cannot be empty")
    @Size(min = 3, max = 10, message = "User must have between 3-10 interests")
    private List<String> interests;
}

