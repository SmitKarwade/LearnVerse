package com.example.learnverse.community.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public Map<String, String> uploadFile(MultipartFile file, String folder) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto",
                    "quality", "auto:good",
                    "fetch_format", "auto",
                    "transformation", new Transformation()
                            .width(800).height(600).crop("limit")
                            .quality("auto:good")
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            Map<String, String> result = new HashMap<>();
            result.put("url", uploadResult.get("secure_url").toString());
            result.put("publicId", uploadResult.get("public_id").toString());
            result.put("format", uploadResult.get("format").toString());

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage());
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    public VideoUploadResult uploadVideo(MultipartFile file, String activityId) throws IOException {
        log.info("📤 Uploading video to Cloudinary - Size: {} MB", file.getSize() / (1024.0 * 1024.0));

        // Upload video
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "video",
                        "folder", "learnverse/videos/" + activityId,
                        "chunk_size", 6000000
                )
        );

        String videoUrl = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");

        String thumbnailUrl = cloudinary.url()
                .resourceType("video")
                .transformation(new Transformation()
                        .startOffset("1")
                        .width(640)
                        .height(360)
                        .crop("fill")
                        .quality("auto"))
                .format("jpg")
                .generate(publicId);

        // Get duration if available (convert seconds to minutes)
        Integer duration = null;
        if (uploadResult.get("duration") != null) {
            try {
                double seconds = ((Number) uploadResult.get("duration")).doubleValue();
                duration = (int) Math.ceil(seconds / 60.0);
            } catch (Exception e) {
                log.warn("⚠️ Could not parse video duration: {}", e.getMessage());
            }
        }

        log.info("✅ Video uploaded - URL: {}, Thumbnail: {}, Duration: {} mins",
                videoUrl, thumbnailUrl, duration);

        return VideoUploadResult.builder()
                .videoUrl(videoUrl)
                .thumbnailUrl(thumbnailUrl)
                .publicId(publicId)
                .durationMinutes(duration)
                .build();
    }

    /**
     * Delete video from Cloudinary
     */
    public void deleteVideo(String publicId) throws IOException {
        log.info("🗑️ Deleting video from Cloudinary: {}", publicId);

        cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", "video")
        );

        log.info("✅ Video deleted from Cloudinary");
    }

    /**
     * Upload resource file (PDF, slides, etc.)
     */
    public String uploadResource(MultipartFile file, String activityId, String resourceType) throws IOException {
        log.info("📤 Uploading resource to Cloudinary - Type: {}", resourceType);

        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "auto", // Auto-detect file type
                        "folder", "learnverse/resources/" + activityId + "/" + resourceType
                )
        );

        String resourceUrl = (String) uploadResult.get("secure_url");
        log.info("✅ Resource uploaded: {}", resourceUrl);

        return resourceUrl;
    }

    @Service
    @RequiredArgsConstructor
    public class FileStorageService {

        private final Cloudinary cloudinary;
        private final String uploadDir = "verification-documents";

        /**
         * Store file to Cloudinary
         */
        public String storeFile(MultipartFile file, String verificationId, String fileType) throws IOException {
            try {
                String fileName = fileType + "_" + UUID.randomUUID().toString() +
                        "_" + file.getOriginalFilename();

                // Upload to Cloudinary
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "learnverse/tutor-verification/" + verificationId,
                                "resource_type", "auto",
                                "public_id", fileName
                        )
                );

                // Return the secure URL
                return (String) uploadResult.get("secure_url");

            } catch (Exception e) {
                throw new IOException("Failed to store file: " + e.getMessage(), e);
            }
        }

        /**
         * Store profile picture (optimized for images)
         */
        public String storeProfilePicture(MultipartFile file, String verificationId) throws IOException {
            try {
                // Validate it's an image
                if (!file.getContentType().startsWith("image/")) {
                    throw new IOException("Profile picture must be an image file");
                }

                String fileName = "profile_" + UUID.randomUUID().toString();

                // Upload to Cloudinary with image transformations
                Map uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", "learnverse/tutor-profiles/" + verificationId,
                                "resource_type", "image",
                                "public_id", fileName,
                                "transformation", new Transformation()
                                        .width(400).height(400)
                                        .crop("fill")
                                        .gravity("face")
                                        .quality("auto")
                                        .fetchFormat("auto")
                        )
                );

                return (String) uploadResult.get("secure_url");

            } catch (Exception e) {
                throw new IOException("Failed to store profile picture: " + e.getMessage(), e);
            }
        }

        /**
         * Delete verification files from Cloudinary
         */
        public void deleteVerificationFiles(String verificationId) {
            try {
                // Delete entire folder
                cloudinary.api().deleteResourcesByPrefix(
                        "learnverse/tutor-verification/" + verificationId,
                        ObjectUtils.emptyMap()
                );

                cloudinary.api().deleteResourcesByPrefix(
                        "learnverse/tutor-profiles/" + verificationId,
                        ObjectUtils.emptyMap()
                );
            } catch (Exception e) {
                // Log but don't fail
                System.err.println("Error deleting files: " + e.getMessage());
            }
        }
    }


    @lombok.Data
    @lombok.Builder
    public static class VideoUploadResult {
        private String videoUrl;
        private String thumbnailUrl;
        private String publicId;
        private Integer durationMinutes;
    }
}