package com.example.learnverse.community.follow;

import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserService userService;

    // Follow a tutor
    public Follow followTutor(String followerId, String tutorId) {
        // Validation: Can't follow yourself
        if (followerId.equals(tutorId)) {
            throw new RuntimeException("You cannot follow yourself");
        }

        // Validation: Check if tutor exists
        AppUser tutor = userService.getUserById(tutorId);
        if (!tutor.getRole().toString().equals("TUTOR")) {
            throw new RuntimeException("You can only follow tutors");
        }

        // Check if already following
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, tutorId)) {
            throw new RuntimeException("Already following this tutor");
        }

        // Create follow relationship
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(tutorId);
        follow.setFollowedAt(LocalDateTime.now());

        return followRepository.save(follow);
    }

    // Unfollow a tutor
    public void unfollowTutor(String followerId, String tutorId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, tutorId)) {
            throw new RuntimeException("You are not following this tutor");
        }

        followRepository.deleteByFollowerIdAndFollowingId(followerId, tutorId);
    }

    // Check if user follows tutor
    public boolean isFollowing(String followerId, String tutorId) {
        return followRepository.existsByFollowerIdAndFollowingId(followerId, tutorId);
    }

    // Get all tutors a user follows
    public List<String> getFollowingTutorIds(String userId) {
        List<Follow> follows = followRepository.findByFollowerId(userId);
        return follows.stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toList());
    }

    // Get all followers of a tutor
    public List<String> getFollowerIds(String tutorId) {
        List<Follow> follows = followRepository.findByFollowingId(tutorId);
        return follows.stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toList());
    }

    // Get follower count
    public long getFollowerCount(String tutorId) {
        return followRepository.countByFollowingId(tutorId);
    }

    // Get following count
    public long getFollowingCount(String userId) {
        return followRepository.countByFollowerId(userId);
    }
}
