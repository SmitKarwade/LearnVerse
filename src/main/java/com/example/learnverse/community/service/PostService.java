package com.example.learnverse.community.service;

import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.community.follow.FollowService;
import com.example.learnverse.community.websocket.WebSocketNotificationService;
import com.example.learnverse.community.model.Post;
import com.example.learnverse.community.repository.PostRepository;
import com.example.learnverse.tutor.model.TutorVerification;
import com.example.learnverse.tutor.repo.TutorVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.learnverse.community.cloudinary.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketNotificationService webSocketService;

    @Autowired
    private FollowService followService;

    @Autowired
    private TutorVerificationRepository tutorVerificationRepository;

    public Post createPost(String authorId, String content, MultipartFile file) {
        AppUser author = userService.getUserById(authorId);

        Post post = new Post();
        post.setAuthorId(authorId);
        post.setAuthorName(author.getName());
        post.setAuthorType(author.getRole().toString());
        post.setAuthorProfilePicture(getProfilePicture(author));
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        // Handle file upload
        if (file != null && !file.isEmpty()) {
            Map<String, String> uploadResult = cloudinaryService.uploadFile(file, "community_posts");
            post.setMediaUrl(uploadResult.get("url"));

            String contentType = file.getContentType();
            if (contentType.startsWith("image/")) {
                post.setMediaType("image");
            } else if (contentType.startsWith("video/")) {
                post.setMediaType("video");
            } else {
                post.setMediaType("file");
            }
        } else {
            post.setMediaType("none");
        }

        Post savedPost = postRepository.save(post);

        // Send real-time notification to followers (optional)
        webSocketService.broadcastNewPost(savedPost);

        return savedPost;
    }

    // ✅ UPDATED: Add comment to post
    public Post addComment(String postId, String authorId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Get commenter details
        AppUser commenter = userService.getUserById(authorId);

        // Create embedded comment with profile picture
        Post.Comment comment = new Post.Comment(
                authorId,
                commenter.getName(),
                commenter.getRole().toString(),
                getProfilePicture(commenter),
                content
        );

        // Add to post's comments list
        post.getComments().add(comment);
        post.setCommentsCount(post.getComments().size());
        post.setUpdatedAt(LocalDateTime.now());

        Post savedPost = postRepository.save(post);

        // Real-time notification
        webSocketService.broadcastNewComment(postId, comment);

        return savedPost;
    }

    // Like/Unlike post
    public Post toggleLike(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.getLikedBy().contains(userId)) {
            post.getLikedBy().remove(userId); // Unlike
        } else {
            post.getLikedBy().add(userId);    // Like

            // Real-time notification
            webSocketService.broadcastPostLike(postId, userId);
        }

        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    // Like/Unlike comment
    public Post toggleCommentLike(String postId, String commentId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Find the comment
        Post.Comment comment = post.getComments().stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (comment.getLikedBy().contains(userId)) {
            comment.getLikedBy().remove(userId); // Unlike
        } else {
            comment.getLikedBy().add(userId);    // Like
        }

        post.setUpdatedAt(LocalDateTime.now());
        return postRepository.save(post);
    }

    // Share post
    public Post sharePost(String postId, String userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getShares().contains(userId)) {
            post.getShares().add(userId);
            post.setUpdatedAt(LocalDateTime.now());

            // Real-time notification
            webSocketService.broadcastPostShare(postId, userId);
        }

        return postRepository.save(post);
    }

    // ⭐ FIXED: Smart Feed with followed tutors prioritized
    public Page<Post> getSmartFeed(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Get tutors the user follows
        List<String> followingTutorIds = followService.getFollowingTutorIds(userId);

        if (followingTutorIds.isEmpty()) {
            // User doesn't follow anyone - show all posts
            return postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        // ⭐ SIMPLE APPROACH: Fetch all posts and sort in memory
        List<Post> allPosts = postRepository.findAll();

        // Sort: Posts from followed tutors first, then others (newest first)
        List<Post> sortedPosts = allPosts.stream()
                .sorted((post1, post2) -> {
                    boolean post1IsFollowed = followingTutorIds.contains(post1.getAuthorId());
                    boolean post2IsFollowed = followingTutorIds.contains(post2.getAuthorId());

                    // If both are followed or both not followed, sort by date
                    if (post1IsFollowed == post2IsFollowed) {
                        return post2.getCreatedAt().compareTo(post1.getCreatedAt());
                    }

                    // Followed posts come first
                    return post1IsFollowed ? -1 : 1;
                })
                .collect(Collectors.toList());

        // Apply pagination manually
        int start = page * size;
        int end = Math.min(start + size, sortedPosts.size());

        if (start >= sortedPosts.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, sortedPosts.size());
        }

        List<Post> paginatedPosts = sortedPosts.subList(start, end);
        return new PageImpl<>(paginatedPosts, pageable, sortedPosts.size());
    }

    // ⭐ NEW: Get feed from ONLY followed tutors
    public Page<Post> getFollowingFeed(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        List<String> followingTutorIds = followService.getFollowingTutorIds(userId);

        if (followingTutorIds.isEmpty()) {
            return Page.empty();
        }

        return postRepository.findByAuthorIdInOrderByCreatedAtDesc(followingTutorIds, pageable);
    }

    // Get feed posts
    public Page<Post> getFeedPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    // Get tutor posts only
    public Page<Post> getTutorPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findTutorPostsOrderByCreatedAtDesc(pageable);
    }

    // Get user's posts
    public Page<Post> getUserPosts(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(userId, pageable);
    }

    // Update post (only by author)
    public Post updatePost(String postId, String authorId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthorId().equals(authorId)) {
            throw new RuntimeException("You can only update your own posts");
        }

        post.setContent(content);
        post.setUpdatedAt(LocalDateTime.now());

        return postRepository.save(post);
    }

    // Delete post (only by author)
    public void deletePost(String postId, String authorId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthorId().equals(authorId)) {
            throw new RuntimeException("You can only delete your own posts");
        }

        // Delete media from Cloudinary if exists
        if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
            try {
                String publicId = extractPublicIdFromUrl(post.getMediaUrl());
                cloudinaryService.deleteFile(publicId);
            } catch (Exception e) {
                // Log error but don't fail deletion
            }
        }

        postRepository.deleteById(postId);
    }

    private String getProfilePicture(AppUser user) {
        // Check if user is a tutor and has verification
        if ("TUTOR".equals(user.getRole().toString())) {
            Optional<TutorVerification> verification = tutorVerificationRepository.findByEmail(user.getEmail());
            if (verification.isPresent() && verification.get().getProfilePicturePath() != null) {
                return verification.get().getProfilePicturePath();
            }
        }
        // Return default or null if no profile picture
        return null;
    }

    private String extractPublicIdFromUrl(String url) {
        // Extract public ID from Cloudinary URL
        String[] parts = url.split("/");
        String fileWithExtension = parts[parts.length - 1];
        return "community_posts/" + fileWithExtension.split("\\.")[0];
    }
}