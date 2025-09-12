package com.example.learnverse.activity.filter;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ActivityIndexConfig implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        createIndexes();
    }

    private void createIndexes() {
        MongoCollection<Document> collection = mongoTemplate.getCollection("activities");

        try {
            // 1. Text search index on multiple fields using Document approach
            collection.createIndex(
                    new Document("title", "text")
                            .append("description", "text")
                            .append("subject", "text")
                            .append("tags", "text"),
                    new IndexOptions().name("idx_text_search")
            );

            // Alternative: Create text index on all fields (wildcard text index)
            // collection.createIndex(
            //     new Document("$**", "text"),
            //     new IndexOptions().name("idx_text_search_all")
            // );

            // 2. Basic filtering compound index
            collection.createIndex(
                    Indexes.ascending("isActive", "isPublic", "subject", "activityType"),
                    new IndexOptions().name("idx_basic_filter")
            );

            // 3. Mode and difficulty index
            collection.createIndex(
                    Indexes.ascending("mode", "difficulty", "isActive", "isPublic"),
                    new IndexOptions().name("idx_mode_difficulty")
            );

            // 4. Location-based index
            collection.createIndex(
                    Indexes.ascending("location.city", "location.state", "isActive", "isPublic"),
                    new IndexOptions().name("idx_location_filter")
            );

            // 5. Price range index
            collection.createIndex(
                    Indexes.ascending("pricing.price", "pricing.priceType", "isActive", "isPublic"),
                    new IndexOptions().name("idx_price_filter")
            );

            // 6. Age group index
            collection.createIndex(
                    Indexes.ascending("suitableAgeGroup.minAge", "suitableAgeGroup.maxAge", "isActive", "isPublic"),
                    new IndexOptions().name("idx_age_group")
            );

            // 7. Duration and rating index (mixed ascending/descending)
            collection.createIndex(
                    new Document("duration.totalDuration", 1)
                            .append("reviews.averageRating", -1)
                            .append("isActive", 1)
                            .append("isPublic", 1),
                    new IndexOptions().name("idx_duration_rating")
            );

            // 8. Feature-based index
            collection.createIndex(
                    Indexes.ascending("demoAvailable", "featured", "pricing.installmentAvailable", "isActive", "isPublic"),
                    new IndexOptions().name("idx_features")
            );

            // 9. Schedule-based index
            collection.createIndex(
                    Indexes.ascending("schedule.flexibleScheduling", "schedule.selfPaced", "schedule.sessionDays", "isActive", "isPublic"),
                    new IndexOptions().name("idx_schedule")
            );

            // 10. Sorting indexes
            // For price sorting (ascending)
            collection.createIndex(
                    Indexes.ascending("isActive", "isPublic", "pricing.price"),
                    new IndexOptions().name("idx_sort_price_asc")
            );

            // For price sorting (descending)
            collection.createIndex(
                    new Document("isActive", 1)
                            .append("isPublic", 1)
                            .append("pricing.price", -1),
                    new IndexOptions().name("idx_sort_price_desc")
            );

            // For rating sorting
            collection.createIndex(
                    new Document("isActive", 1)
                            .append("isPublic", 1)
                            .append("reviews.averageRating", -1),
                    new IndexOptions().name("idx_sort_rating")
            );

            // For popularity sorting
            collection.createIndex(
                    new Document("isActive", 1)
                            .append("isPublic", 1)
                            .append("enrollmentInfo.enrolledCount", -1),
                    new IndexOptions().name("idx_sort_popularity")
            );

            // For date sorting (newest)
            collection.createIndex(
                    new Document("isActive", 1)
                            .append("isPublic", 1)
                            .append("createdAt", -1),
                    new IndexOptions().name("idx_sort_newest")
            );

            // For duration sorting
            collection.createIndex(
                    Indexes.ascending("isActive", "isPublic", "duration.totalDuration"),
                    new IndexOptions().name("idx_sort_duration")
            );

            // 11. Tags index for interest matching
            collection.createIndex(
                    Indexes.ascending("tags", "isActive", "isPublic"),
                    new IndexOptions().name("idx_tags")
            );

            // 12. Tutor-specific index
            collection.createIndex(
                    Indexes.ascending("tutorId"),
                    new IndexOptions().name("idx_tutor_id")
            );

            // 13. Free trial index
            collection.createIndex(
                    Indexes.ascending("pricing.freeTrialDays", "demoDetails.freeTrial", "isActive", "isPublic"),
                    new IndexOptions().name("idx_free_trial")
            );

            // 14. Subject and tags combined for interest matching
            collection.createIndex(
                    Indexes.ascending("subject", "tags", "isActive", "isPublic"),
                    new IndexOptions().name("idx_subject_tags")
            );

            // 15. Mixed ascending/descending for complex sorting
            collection.createIndex(
                    new Document("isActive", 1)
                            .append("isPublic", 1)
                            .append("featured", -1)
                            .append("reviews.averageRating", -1)
                            .append("pricing.price", 1),
                    new IndexOptions().name("idx_featured_rating_price")
            );

            // 16. Geospatial index for proximity search
            collection.createIndex(
                    Indexes.geo2dsphere("location.coordinates"),
                    new IndexOptions().name("idx_location_coordinates_2dsphere")
            );

            log.info("Successfully created MongoDB indexes for Activity collection");

        } catch (Exception e) {
            log.error("Error creating MongoDB indexes: ", e);
        }
    }
}

