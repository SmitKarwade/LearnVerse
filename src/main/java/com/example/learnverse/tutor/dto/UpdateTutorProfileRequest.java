package com.example.learnverse.tutor.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateTutorProfileRequest {
    private String bio;
    private List<String> qualifications;
    private String experience;
    private List<String> specializations;
}
