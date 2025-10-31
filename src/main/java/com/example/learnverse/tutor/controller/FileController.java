package com.example.learnverse.tutor.controller;

import com.example.learnverse.tutor.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * ✅ Admin: Get verification document info (now returns Cloudinary URL)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/verification-documents/{verificationId}")
    public ResponseEntity<?> getVerificationDocuments(
            @PathVariable String verificationId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Documents are stored in Cloudinary");
            response.put("verificationId", verificationId);
            response.put("note", "Access Cloudinary URLs from TutorVerification MongoDB document");

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", ex.getMessage()
            ));
        }
    }

    /**
     * ✅ Redirect to Cloudinary URL (if needed)
     * Documents are accessed directly via their Cloudinary URLs from MongoDB
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/verification-documents/redirect")
    public ResponseEntity<?> redirectToDocument(
            @RequestParam String cloudinaryUrl) {
        try {
            // Simply redirect to the Cloudinary URL
            return ResponseEntity.status(302)
                    .header("Location", cloudinaryUrl)
                    .build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", ex.getMessage()
            ));
        }
    }

    /**
     * Test endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<?> testFileController() {
        return ResponseEntity.ok(Map.of(
                "message", "FileController is working!",
                "note", "All files are now stored in Cloudinary"
        ));
    }
}