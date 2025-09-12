package com.example.learnverse.activity.repository;

import com.example.learnverse.activity.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, String> {

    // Existing methods
    @Query("{ $and: [ " +
            "  { $or: [ " +
            "    { 'subject': { $in: ?0 } }, " +
            "    { 'tags': { $in: ?0 } } " +
            "  ] }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    List<Activity> findActivitiesByUserInterests(List<String> interests);

    List<Activity> findByIsActiveAndIsPublic(Boolean isActive, Boolean isPublic);

    // Text search on title, description, and subject
    @Query("{ $and: [ " +
            "  { $text: { $search: ?0 } }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    Page<Activity> findByTextSearch(String searchQuery, Pageable pageable);

    // Price range filter
    @Query("{ $and: [ " +
            "  { 'pricing.price': { $gte: ?0, $lte: ?1 } }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    Page<Activity> findByPriceRange(Integer minPrice, Integer maxPrice, Pageable pageable);

    // Rating filter
    @Query("{ $and: [ " +
            "  { 'reviews.averageRating': { $gte: ?0 } }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    Page<Activity> findByMinRating(Double minRating, Pageable pageable);

    // Duration filter
    @Query("{ $and: [ " +
            "  { 'duration.totalDuration': { $gte: ?0, $lte: ?1 } }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    Page<Activity> findByDurationRange(Integer minDuration, Integer maxDuration, Pageable pageable);

    // Age group filter
    @Query("{ $and: [ " +
            "  { 'suitableAgeGroup.minAge': { $lte: ?1 } }, " +
            "  { 'suitableAgeGroup.maxAge': { $gte: ?0 } }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    Page<Activity> findByAgeRange(Integer minAge, Integer maxAge, Pageable pageable);

    // Free trial available
    @Query("{ $and: [ " +
            "  { $or: [ " +
            "    { 'pricing.freeTrialDays': { $gt: 0 } }, " +
            "    { 'demoDetails.freeTrial': true } " +
            "  ] }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    Page<Activity> findWithFreeTrial(Pageable pageable);

    // Installment available
    @Query("{ $and: [ " +
            "  { 'pricing.installmentAvailable': true }, " +
            "  { 'isActive': true }, " +
            "  { 'isPublic': true } " +
            "] }")
    Page<Activity> findWithInstallment(Pageable pageable);
}