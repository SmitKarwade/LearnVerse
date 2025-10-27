package com.example.learnverse.community.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    @lombok.Data
    @lombok.Builder
    public static class VideoUploadResult {
        private String videoUrl;
        private String thumbnailUrl;
        private String publicId;
        private Integer durationMinutes;
    }
}