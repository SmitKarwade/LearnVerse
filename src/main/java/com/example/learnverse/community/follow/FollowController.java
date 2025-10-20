package com.example.learnverse.community.follow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/follow")
public class FollowController {

    @Autowired
    private FollowService followService;

    // Follow a tutor
    @PostMapping("/{tutorId}")
    public ResponseEntity<?> followTutor(  // ⭐ Changed to <?> for better error messages
                                           @PathVariable String tutorId,
                                           Authentication auth) {
        try {
            String userId = auth.getName();
            Follow follow = followService.followTutor(userId, tutorId);
            return ResponseEntity.ok(follow);
        } catch (RuntimeException e) {  // ⭐ Catch specific exception
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Unfollow a tutor
    @DeleteMapping("/{tutorId}")
    public ResponseEntity<?> unfollowTutor(  // ⭐ Changed to <?>
                                             @PathVariable String tutorId,
                                             Authentication auth) {
        try {
            String userId = auth.getName();
            followService.unfollowTutor(userId, tutorId);
            return ResponseEntity.ok(Map.of("message", "Unfollowed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Rest of methods are perfect as-is ✅

    @GetMapping("/status/{tutorId}")
    public ResponseEntity<Map<String, Boolean>> checkFollowStatus(
            @PathVariable String tutorId,
            Authentication auth) {
        String userId = auth.getName();
        boolean isFollowing = followService.isFollowing(userId, tutorId);
        return ResponseEntity.ok(Map.of("isFollowing", isFollowing));
    }

    @GetMapping("/following")
    public ResponseEntity<List<String>> getFollowing(Authentication auth) {
        String userId = auth.getName();
        List<String> following = followService.getFollowingTutorIds(userId);
        return ResponseEntity.ok(following);
    }

    @GetMapping("/followers/{tutorId}")
    public ResponseEntity<Map<String, Object>> getFollowers(@PathVariable String tutorId) {
        List<String> followers = followService.getFollowerIds(tutorId);
        long count = followService.getFollowerCount(tutorId);

        return ResponseEntity.ok(Map.of(
                "followers", followers,
                "count", count
        ));
    }

    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Long>> getFollowStats(@PathVariable String userId) {
        long followersCount = followService.getFollowerCount(userId);
        long followingCount = followService.getFollowingCount(userId);

        return ResponseEntity.ok(Map.of(
                "followersCount", followersCount,
                "followingCount", followingCount
        ));
    }
}