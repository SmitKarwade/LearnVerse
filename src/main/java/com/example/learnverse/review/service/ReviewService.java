package com.example.learnverse.review.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.enrollment.model.CourseEnrollment;
import com.example.learnverse.enrollment.model.EnrollmentStatus;
import com.example.learnverse.enrollment.repository.CourseEnrollmentRepository;
import com.example.learnverse.review.dto.ReviewDTO;
import com.example.learnverse.review.model.Review;
import com.example.learnverse.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Add a new review (only enrolled users can review)
     */
    @Transactional
    public Review addReview(String activityId, String userId, ReviewDTO.CreateReviewRequest request) {
        log.info("📝 Adding review for activity: {} by user: {}", activityId, userId);

        // 1. Verify activity exists
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 2. Check if user is enrolled
        Optional<CourseEnrollment> enrollmentOpt = enrollmentRepository
                .findByUserIdAndActivityId(userId, activityId);

        if (enrollmentOpt.isEmpty()) {
            throw new RuntimeException("You must be enrolled in this activity to leave a review");
        }

        CourseEnrollment enrollment = enrollmentOpt.get();

        // Optional: Only allow reviews if enrollment is active or completed
        if (enrollment.getStatus() != EnrollmentStatus.ENROLLED &&
                enrollment.getStatus() != EnrollmentStatus.IN_PROGRESS &&
                enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new RuntimeException("You can only review activities you are actively enrolled in or have completed");
        }

        // 3. Check if user already reviewed (one review per user per activity)
        Optional<Review> existingReview = reviewRepository
                .findByActivityIdAndUserId(activityId, userId);

        if (existingReview.isPresent()) {
            throw new RuntimeException("You have already reviewed this activity. Please update your existing review instead.");
        }

        // 4. Get user details
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 5. Create review
        Review review = Review.builder()
                .activityId(activityId)
                .userId(userId)
                .userName(user.getName() != null ? user.getName() : user.getEmail().split("@")[0])
                .rating(request.getRating())
                .feedback(request.getFeedback())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isEdited(false)
                .isVerifiedEnrollment(true)
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("✅ Review added: {}", savedReview.getId());

        // 6. Update activity's review statistics
        updateActivityReviewStats(activityId);

        return savedReview;
    }

    /**
     * Update existing review (only owner can update)
     */
    @Transactional
    public Review updateReview(String reviewId, String userId, ReviewDTO.UpdateReviewRequest request) {
        log.info("✏️ Updating review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Verify ownership
        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("You can only update your own reviews");
        }

        // Update fields
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getFeedback() != null) {
            review.setFeedback(request.getFeedback());
        }

        review.setUpdatedAt(LocalDateTime.now());
        review.setIsEdited(true);

        Review updated = reviewRepository.save(review);
        log.info("✅ Review updated: {}", reviewId);

        // Update activity stats
        updateActivityReviewStats(review.getActivityId());

        return updated;
    }

    /**
     * Delete review (only owner can delete)
     */
    @Transactional
    public void deleteReview(String reviewId, String userId) {
        log.info("🗑️ Deleting review: {} by user: {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Verify ownership
        if (!review.getUserId().equals(userId)) {
            throw new RuntimeException("You can only delete your own reviews");
        }

        String activityId = review.getActivityId();
        reviewRepository.deleteById(reviewId);
        log.info("✅ Review deleted: {}", reviewId);

        // Update activity stats
        updateActivityReviewStats(activityId);
    }

    /**
     * Get all reviews for an activity (public - no auth required)
     */
    public Page<Review> getActivityReviews(String activityId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByActivityIdOrderByCreatedAtDesc(activityId, pageable);
    }

    /**
     * Get user's own reviews
     */
    public List<Review> getUserReviews(String userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Check if user has reviewed an activity
     */
    public boolean hasUserReviewed(String activityId, String userId) {
        return reviewRepository.findByActivityIdAndUserId(activityId, userId).isPresent();
    }

    /**
     * Update Activity's review statistics (auto-calculation)
     */
    @Transactional
    public void updateActivityReviewStats(String activityId) {
        log.info("🔄 Updating review stats for activity: {}", activityId);

        // Get all reviews for this activity
        List<Review> reviews = reviewRepository.findByActivityId(activityId);

        if (reviews.isEmpty()) {
            // No reviews - reset stats
            Query query = new Query(Criteria.where("_id").is(activityId));
            Update update = new Update()
                    .set("reviews.averageRating", null)
                    .set("reviews.totalReviews", 0)
                    .set("reviews.ratingDistribution", null);

            mongoTemplate.updateFirst(query, update, Activity.class);
            log.info("✅ Review stats reset (no reviews)");
            return;
        }

        // Calculate average rating
        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        int totalReviews = reviews.size();

        // Calculate rating distribution (1-5)
        Map<String, Integer> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            int rating = i;
            long count = reviews.stream()
                    .filter(r -> r.getRating() == rating)
                    .count();
            ratingDistribution.put(String.valueOf(i), (int) count);
        }

        // Update activity
        Query query = new Query(Criteria.where("_id").is(activityId));
        Update update = new Update()
                .set("reviews.averageRating", Math.round(averageRating * 10.0) / 10.0) // Round to 1 decimal
                .set("reviews.totalReviews", totalReviews)
                .set("reviews.ratingDistribution", ratingDistribution);

        mongoTemplate.updateFirst(query, update, Activity.class);

        log.info("✅ Review stats updated - Avg: {}, Total: {}", averageRating, totalReviews);
    }
}