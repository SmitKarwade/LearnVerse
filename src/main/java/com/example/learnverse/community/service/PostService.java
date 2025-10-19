package com.example.learnverse.community.service;

import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.community.websocket.WebSocketNotificationService;
import com.example.learnverse.community.model.Post;
import com.example.learnverse.community.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.learnverse.community.cloudinary.CloudinaryService;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.Map;

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

    // Create new post
    public Post createPost(String authorId, String content, MultipartFile file) {
        AppUser author = userService.getUserById(authorId);

        Post post = new Post();
        post.setAuthorId(authorId);
        post.setAuthorName(author.getName());
        post.setAuthorType(author.getRole().toString());
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

    // Add comment to post
    public Post addComment(String postId, String authorId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        // Get commenter details - your method returns AppUser directly
        AppUser commenter = userService.getUserById(authorId);

        // Create embedded comment
        Post.Comment comment = new Post.Comment(
                authorId,
                commenter.getName(),
                commenter.getRole().toString(),
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

    private String extractPublicIdFromUrl(String url) {
        // Extract public ID from Cloudinary URL
        String[] parts = url.split("/");
        String fileWithExtension = parts[parts.length - 1];
        return "community_posts/" + fileWithExtension.split("\\.")[0];
    }
}