package com.example.learnverse.review.controller;

import com.example.learnverse.review.dto.ReviewDTO;
import com.example.learnverse.review.model.Review;
import com.example.learnverse.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Add a review (enrolled users only)
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/activities/{activityId}/reviews")
    public ResponseEntity<?> addReview(
            @PathVariable String activityId,
            @Valid @RequestBody ReviewDTO.CreateReviewRequest request,
            Authentication auth) {
        try {
            String userId = auth.getName();
            Review review = reviewService.addReview(activityId, userId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review added successfully",
                    "review", review
            ));
        } catch (Exception e) {
            log.error("❌ Error adding review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get all reviews for an activity (PUBLIC - no auth required for reading)
     */
    @GetMapping("/activities/{activityId}/reviews")
    public ResponseEntity<?> getActivityReviews(
            @PathVariable String activityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Review> reviews = reviewService.getActivityReviews(activityId, page, size);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reviews", reviews.getContent(),
                    "totalReviews", reviews.getTotalElements(),
                    "totalPages", reviews.getTotalPages(),
                    "currentPage", page
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching reviews: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Update own review
     */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewDTO.UpdateReviewRequest request,
            Authentication auth) {
        try {
            String userId = auth.getName();
            Review review = reviewService.updateReview(reviewId, userId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review updated successfully",
                    "review", review
            ));
        } catch (Exception e) {
            log.error("❌ Error updating review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete own review
     */
    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable String reviewId,
            Authentication auth) {
        try {
            String userId = auth.getName();
            reviewService.deleteReview(reviewId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Review deleted successfully"
            ));
        } catch (Exception e) {
            log.error("❌ Error deleting review: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get user's own reviews
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/reviews/my-reviews")
    public ResponseEntity<?> getMyReviews(Authentication auth) {
        try {
            String userId = auth.getName();
            List<Review> reviews = reviewService.getUserReviews(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reviews", reviews,
                    "total", reviews.size()
            ));
        } catch (Exception e) {
            log.error("❌ Error fetching user reviews: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Check if user has reviewed an activity
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/activities/{activityId}/reviews/check")
    public ResponseEntity<?> checkUserReview(
            @PathVariable String activityId,
            Authentication auth) {
        try {
            String userId = auth.getName();
            boolean hasReviewed = reviewService.hasUserReviewed(activityId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasReviewed", hasReviewed
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
