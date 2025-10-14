package com.example.learnverse.auth.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProfileSetupRequest {
    private String firstName;
    private String lastName;
    private String location;
    private String gender;
    private Integer age;
    private String currentEducationLevel;
    private String currentRole;
    private List<String> interests;
    private String careerGoal;
    private List<String> targetSkills;
    private String currentFocusArea;
    private String communicationStyle;
    private Boolean wantsStepByStepGuidance;
}