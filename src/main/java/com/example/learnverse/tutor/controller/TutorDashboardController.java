package com.example.learnverse.tutor.controller;

import com.example.learnverse.tutor.dto.TutorDashboardStats;
import com.example.learnverse.tutor.service.TutorDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutor/dashboard")
@PreAuthorize("hasRole('TUTOR')")
@RequiredArgsConstructor
@Slf4j
public class TutorDashboardController {

    private final TutorDashboardService dashboardService;

    /**
     * Get complete dashboard stats
     * GET /api/tutor/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<TutorDashboardStats> getDashboardStats(
            @AuthenticationPrincipal String tutorId
    ) {
        log.info("Fetching dashboard stats for tutor: {}", tutorId);

        try {
            TutorDashboardStats stats = dashboardService.getDashboardStats(tutorId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Failed to fetch dashboard stats", e);
            return ResponseEntity.badRequest().build();
        }
    }
}