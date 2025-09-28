package com.example.learnverse.tutor.controller;

import com.example.learnverse.tutor.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Admin: Download verification documents
     * URL: /api/files/verification-documents/tutors/{tutorId}/{fileName}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/verification-documents/tutors/{tutorId}/{fileName:.+}")
    public ResponseEntity<Resource> downloadVerificationDocument(
            @PathVariable String tutorId,
            @PathVariable String fileName) {
        try {
            // Build path: tutors/{tutorId}/{fileName}
            Path filePath = fileStorageService.getFileStorageLocation()
                    .resolve("tutors")
                    .resolve(tutorId)
                    .resolve(fileName)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Detect content type
                String contentType = null;
                try {
                    contentType = Files.probeContentType(filePath);
                } catch (IOException ex) {
                    contentType = "application/octet-stream";
                }

                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Admin: View document in browser (for PDFs/images)
     * URL: /api/files/verification-documents/view/tutors/{tutorId}/{fileName}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/verification-documents/view/tutors/{tutorId}/{fileName:.+}")
    public ResponseEntity<Resource> viewVerificationDocument(
            @PathVariable String tutorId,
            @PathVariable String fileName) {

        System.out.println("=== FILE VIEW DEBUG ===");
        System.out.println("TutorId: " + tutorId);
        System.out.println("FileName: " + fileName);

        try {
            // Build path: tutors/{tutorId}/{fileName}
            Path filePath = fileStorageService.getFileStorageLocation()
                    .resolve("tutors")
                    .resolve(tutorId)
                    .resolve(fileName)
                    .normalize();

            System.out.println("File path: " + filePath);
            System.out.println("File exists: " + Files.exists(filePath));

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = null;
                try {
                    contentType = Files.probeContentType(filePath);
                } catch (IOException ex) {
                    contentType = "application/octet-stream";
                }

                // Set appropriate content type for viewing
                if (contentType == null) {
                    if (fileName.toLowerCase().endsWith(".pdf")) {
                        contentType = "application/pdf";
                    } else if (fileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$")) {
                        contentType = "image/" + fileName.substring(fileName.lastIndexOf(".") + 1);
                    } else {
                        contentType = "application/octet-stream";
                    }
                }

                System.out.println("Serving with content type: " + contentType);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .body(resource);
            } else {
                System.out.println("File not found or not readable");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<?> testFileController() {
        return ResponseEntity.ok("FileController is working!");
    }
}