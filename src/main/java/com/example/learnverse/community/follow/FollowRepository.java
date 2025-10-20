package com.example.learnverse.community.follow;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends MongoRepository<Follow, String> {

    // Check if user follows tutor
    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    // Get all tutors a user follows
    List<Follow> findByFollowerId(String followerId);

    // Get all followers of a tutor
    List<Follow> findByFollowingId(String followingId);

    // Count followers
    long countByFollowingId(String followingId);

    // Count following
    long countByFollowerId(String followerId);

    // Delete follow relationship
    @Transactional
    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);

    // Find specific follow
    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);
}
