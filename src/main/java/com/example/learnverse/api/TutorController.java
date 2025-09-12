package com.example.learnverse.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('TUTOR')")
    public Object tutorDashboard() {
        return new Object() {
            public final String message = "tutor-only";
        };
    }
}
