package com.example.learnverse.tutor.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    private Cloudinary cloudinary;

    public FileStorageService(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    /**
     * Store document files (ID, certificate) to local storage
     */
    public String storeFile(MultipartFile file, String tutorId, String fileType) {
        // Create tutor-specific directory
        Path tutorDirectory = this.fileStorageLocation.resolve("tutors").resolve(tutorId);

        try {
            Files.createDirectories(tutorDirectory);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create tutor directory", ex);
        }

        // Generate unique filename
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        if (fileName.contains(".")) {
            fileExtension = fileName.substring(fileName.lastIndexOf("."));
        }

        String storedFileName = fileType + "_" + UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = tutorDirectory.resolve(storedFileName);

        try {
            // Copy file to the target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path for storage in database
            return "tutors/" + tutorId + "/" + storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + storedFileName + ". Please try again!", ex);
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
                            "folder", "learnverse/tutor-profiles",
                            "public_id", verificationId + "/" + fileName,
                            "resource_type", "image",
                            "transformation", new Transformation()
                                    .width(400).height(400)
                                    .crop("fill")
                                    .gravity("face")
                                    .quality("auto")
                                    .fetchFormat("auto")
                    )
            );

            // Return the Cloudinary secure URL
            return (String) uploadResult.get("secure_url");

        } catch (Exception e) {
            throw new IOException("Failed to store profile picture: " + e.getMessage(), e);
        }
    }

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
     * Delete local verification files
     */
    public void deleteVerificationFiles(String verificationId) {
        try {
            Path tutorDir = fileStorageLocation.resolve("tutors").resolve(verificationId);
            if (Files.exists(tutorDir)) {
                Files.walk(tutorDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete verification files for " + verificationId + ": " + e.getMessage());
        }

        // ✅ Also delete from Cloudinary
        try {
            cloudinary.api().deleteResourcesByPrefix(
                    "learnverse/tutor-profiles/" + verificationId,
                    ObjectUtils.emptyMap()
            );
        } catch (Exception e) {
            System.err.println("Failed to delete Cloudinary files: " + e.getMessage());
        }
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}