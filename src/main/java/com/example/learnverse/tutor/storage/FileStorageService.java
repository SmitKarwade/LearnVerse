package com.example.learnverse.tutor.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final Cloudinary cloudinary;

    /**
     * ✅ Store verification documents to Cloudinary (ID, Certificate)
     */
    public String storeFile(MultipartFile file, String verificationId, String fileType) throws IOException {
        try {
            if (file.isEmpty()) {
                throw new IOException("File is empty");
            }

            String fileName = fileType + "_" + UUID.randomUUID().toString();

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "learnverse/tutor-verification/" + verificationId,
                            "resource_type", "auto",  // Auto-detect: PDF, JPG, PNG
                            "public_id", fileName
                    )
            );

            // ✅ Return Cloudinary URL (NOT local path)
            String cloudinaryUrl = (String) uploadResult.get("secure_url");
            return cloudinaryUrl;

        } catch (Exception e) {
            throw new IOException("Failed to store document to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Store profile picture to Cloudinary (optimized for images)
     */
    public String storeProfilePicture(MultipartFile file, String verificationId) throws IOException {
        try {
            // Validate it's an image
            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                throw new IOException("Profile picture must be an image file (JPEG, JPG, or PNG)");
            }

            String fileName = "profile_" + UUID.randomUUID().toString();

            // Upload to Cloudinary with image transformations
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "learnverse/tutor-profiles/" + verificationId,
                            "public_id", fileName,
                            "resource_type", "image",
                            "transformation", new Transformation()
                                    .width(400).height(400)
                                    .crop("fill")
                                    .gravity("face")
                                    .quality("auto")
                                    .fetchFormat("auto")
                    )
            );

            // ✅ Return Cloudinary URL
            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to store profile picture: " + e.getMessage(), e);
        }
    }

    /**
     * ✅ Delete verification files from Cloudinary
     */
    public void deleteVerificationFiles(String verificationId) {
        try {
            // Delete entire folder from Cloudinary
            cloudinary.api().deleteResourcesByPrefix(
                    "learnverse/tutor-verification/" + verificationId,
                    ObjectUtils.emptyMap()
            );

            cloudinary.api().deleteResourcesByPrefix(
                    "learnverse/tutor-profiles/" + verificationId,
                    ObjectUtils.emptyMap()
            );

            System.out.println("✅ Deleted Cloudinary files for verification: " + verificationId);

        } catch (Exception e) {
            System.err.println("Failed to delete Cloudinary files: " + e.getMessage());
            // Don't throw - continue even if deletion fails
        }
    }

    /**
     * ✅ Delete specific image from Cloudinary
     */
    public void deleteCloudinaryImage(String publicId) {
        try {
            if (publicId != null && !publicId.isEmpty()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                System.out.println("✅ Deleted Cloudinary image: " + publicId);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete Cloudinary image: " + e.getMessage());
        }
    }

    /**
     * ✅ Extract public_id from Cloudinary URL for deletion
     */
    private String extractPublicIdFromUrl(String cloudinaryUrl) {
        try {
            String[] parts = cloudinaryUrl.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Remove version number if present
                path = path.replaceFirst("v\\d+/", "");
                // Remove file extension
                path = path.substring(0, path.lastIndexOf('.'));
                return path;
            }
        } catch (Exception e) {
            System.err.println("Failed to extract public_id: " + e.getMessage());
        }
        return null;
    }
}