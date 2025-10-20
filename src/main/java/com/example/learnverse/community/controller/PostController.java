package com.example.learnverse.community.controller;

import com.example.learnverse.community.model.Post;
import com.example.learnverse.community.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/community/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @RequireApprovedTutor  // Use your existing annotation or remove for all users
    public ResponseEntity<Post> createPost(
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication auth) {
        try {
            String authorId = auth.getName();
            Post post = postService.createPost(authorId, content, file);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ⭐ NEW: Smart feed (followed tutors prioritized)
    @GetMapping("/smart-feed")
    public ResponseEntity<Page<Post>> getSmartFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        String userId = auth.getName();
        Page<Post> posts = postService.getSmartFeed(userId, page, size);
        return ResponseEntity.ok(posts);
    }

    // ⭐ NEW: Following feed (ONLY from followed tutors)
    @GetMapping("/following-feed")
    public ResponseEntity<Page<Post>> getFollowingFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        String userId = auth.getName();
        Page<Post> posts = postService.getFollowingFeed(userId, page, size);
        return ResponseEntity.ok(posts);
    }

    // Get feed posts
    @GetMapping("/feed")
    public ResponseEntity<Page<Post>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Post> posts = postService.getFeedPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    // Get user's posts
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Post>> getUserPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Post> posts = postService.getUserPosts(userId, page, size);
        return ResponseEntity.ok(posts);
    }

    // Like/Unlike post
    @PostMapping("/{postId}/like")
    public ResponseEntity<Post> toggleLike(
            @PathVariable String postId,
            Authentication auth) {
        try {
            String userId = auth.getName();
            Post post = postService.toggleLike(postId, userId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Share post
    @PostMapping("/{postId}/share")
    public ResponseEntity<Post> sharePost(
            @PathVariable String postId,
            Authentication auth) {
        try {
            String userId = auth.getName();
            Post post = postService.sharePost(postId, userId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Add comment
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Post> addComment(
            @PathVariable String postId,
            @RequestParam String content,
            Authentication auth) {
        try {
            String authorId = auth.getName();
            Post post = postService.addComment(postId, authorId, content);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Like comment
    @PostMapping("/{postId}/comments/{commentId}/like")
    public ResponseEntity<Post> likeComment(
            @PathVariable String postId,
            @PathVariable String commentId,
            Authentication auth) {
        try {
            String userId = auth.getName();
            Post post = postService.toggleCommentLike(postId, commentId, userId);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Update post
    @PutMapping("/{postId}")
    public ResponseEntity<Post> updatePost(
            @PathVariable String postId,
            @RequestParam String content,
            Authentication auth) {
        try {
            String authorId = auth.getName();
            Post post = postService.updatePost(postId, authorId, content);
            return ResponseEntity.ok(post);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Delete post
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable String postId,
            Authentication auth) {
        try {
            String authorId = auth.getName();
            postService.deletePost(postId, authorId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}