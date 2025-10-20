package com.example.learnverse.community.repository;

import com.example.learnverse.community.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    // Basic queries
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    // Feed queries
    @Query("{ 'authorType': 'TUTOR' }")
    Page<Post> findTutorPostsOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByAuthorIdInOrderByCreatedAtDesc(List<String> authorIds, Pageable pageable);

    // ⭐ NEW: Smart feed query - followed tutors first
    @Aggregation(pipeline = {
            "{ $addFields: { isFollowing: { $in: ['$authorId', ?0] } } }",
            "{ $sort: { isFollowing: -1, createdAt: -1 } }",
            "{ $skip: ?1 }",
            "{ $limit: ?2 }"
    })
    List<Post> findSmartFeedCustom(List<String> followingIds, int skip, int limit);

    // Search by content
    Page<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String content, Pageable pageable);

    // Count user posts
    long countByAuthorId(String authorId);
}


