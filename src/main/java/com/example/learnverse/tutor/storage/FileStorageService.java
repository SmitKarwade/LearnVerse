package com.example.learnverse.tutor.storage;

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
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${app.file.upload-dir:./uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

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
            // Copy file to the target location (Replacing existing file with the same name)
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return relative path for storage in database
            return "tutors/" + tutorId + "/" + storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + storedFileName + ". Please try again!", ex);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path file = this.fileStorageLocation.resolve(filePath).normalize();
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            // Log the exception but don't throw - file deletion is not critical
            System.err.println("Could not delete file: " + filePath);
        }
    }

    // Add to your FileStorageService
    public void deleteVerificationFiles(String verificationId) {
        try {
            Path tutorDir = fileStorageLocation.resolve("tutors").resolve(verificationId);
            if (Files.exists(tutorDir)) {
                // Delete all files in the tutor's directory
                Files.walk(tutorDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception e) {
            // Log but don't throw - file cleanup is not critical
            System.err.println("Failed to delete verification files for " + verificationId + ": " + e.getMessage());
        }
    }


    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}
