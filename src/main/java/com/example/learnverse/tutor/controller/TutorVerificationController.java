package com.example.learnverse.tutor.controller;


import com.example.learnverse.auth.refresh.RefreshToken;
import com.example.learnverse.auth.refresh.RefreshTokenService;
import com.example.learnverse.auth.service.AuthService;
import com.example.learnverse.auth.service.UserService;
import com.example.learnverse.auth.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.learnverse.tutor.service.TutorVerificationService;
import com.example.learnverse.auth.jwt.JwtUtil; // Your existing JWT utility
import com.example.learnverse.tutor.model.TutorVerification;
import com.example.learnverse.tutor.enumcl.VerificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tutor-verification")
@CrossOrigin(origins = "*") // Configure as needed for your frontend
public class TutorVerificationController {

    @Autowired
    private TutorVerificationService verificationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private TutorVerificationService tutorVerificationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    /**
     * Submit tutor verification request
     */
    @PostMapping("/submit")
    public ResponseEntity<?> registerTutor(
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String bio,
            @RequestParam List<String> qualifications,
            @RequestParam String experience,
            @RequestParam List<String> specializations,
            @RequestParam MultipartFile profilePicture,
            @RequestParam MultipartFile idDocument,
            @RequestParam MultipartFile certificate,
            @RequestParam Boolean termsAccepted,
            Authentication auth) {

        try {
            String userId = auth.getName();
            AppUser user = userService.getUserById(userId);

            if (!user.getEmail().equals(email)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Email must match your account email"
                ));
            }

            TutorVerification verification = tutorVerificationService.createVerificationRequest(
                    email, fullName, phone, bio, qualifications, experience, specializations, profilePicture,
                    idDocument, certificate, termsAccepted
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Tutor verification submitted successfully",
                    "verificationId", verification.getId(),
                    "status", verification.getStatus(),
                    "profilePictureUrl", verification.getProfilePicturePath()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Check verification status
     */
    @GetMapping("/status/{email}")
    public ResponseEntity<?> checkVerificationStatus(@PathVariable String email) {
        try {
            var verification = verificationService.getVerificationByEmail(email);

            if (verification.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            TutorVerification v = verification.get();
            Map<String, Object> response = new HashMap<>();
            response.put("email", v.getEmail());
            response.put("status", v.getStatus().name());
            response.put("statusDescription", v.getStatus().getDescription());
            response.put("submittedAt", v.getCreatedAt());
            response.put("canCreateActivities", v.getStatus().canCreateActivities());

            if (v.getStatus() == VerificationStatus.REJECTED) {
                response.put("rejectionReason", v.getRejectionReason());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: Get all verifications
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllVerifications() {
        try {
            List<TutorVerification> verifications = verificationService.getAllVerifications();
            return ResponseEntity.ok(verifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/admin/approve/{verificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveVerification(
            @PathVariable String verificationId,
            HttpServletRequest request) { // ✅ Add HttpServletRequest
        try {
            System.out.println("✅ APPROVAL PROCESS START");
            System.out.println("Verification ID: " + verificationId);

            TutorVerification verification = verificationService.approveVerification(verificationId);
            System.out.println("Verification approved in DB");

            // Upgrade user role from USER to TUTOR
            AppUser user = authService.getUserByEmail(verification.getEmail());
            System.out.println("Found user: " + user.getEmail());

            authService.upgradeUserToTutor(user.getId());
            System.out.println("User upgraded to TUTOR");

            // ✅ Generate access token with TUTOR role
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "TUTOR");
            claims.put("status", "APPROVED");
            claims.put("tutorId", verification.getId());
            claims.put("fullName", verification.getFullName());
            claims.put("email", verification.getEmail());

            String accessToken = jwtUtil.generateAccessToken(user.getId(), claims);

            // ✅ CORRECT: Generate refresh token using RefreshTokenService
            String deviceInfo = getDeviceInfo(request);
            RefreshToken refreshTokenObj = refreshTokenService.createRefreshToken(user.getId(), deviceInfo);

            System.out.println("New tokens generated");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tutor verification approved successfully");
            response.put("tutorEmail", verification.getEmail());
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshTokenObj.getToken()); // ✅ Get token string
            response.put("tokenType", "Bearer");
            response.put("accessTokenExpiresIn", jwtUtil.getAccessExpSeconds());
            response.put("refreshTokenExpiresIn", calculateRefreshTokenExpSeconds(refreshTokenObj));

            System.out.println("✅ APPROVAL PROCESS SUCCESS");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ APPROVAL PROCESS FAILED");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/admin/reject/{verificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectVerification(
            @PathVariable String verificationId,
            @RequestParam String reason) {

        try {
            TutorVerification verification = verificationService.rejectVerification(verificationId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tutor verification rejected");
            response.put("tutorEmail", verification.getEmail());
            response.put("status", verification.getStatus().name());
            response.put("rejectionReason", reason);
            response.put("rejectedAt", verification.getUpdatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: Get pending verifications with document links
     */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingVerifications() {
        try {
            List<TutorVerification> pendingVerifications = verificationService.getPendingVerifications();

            // Add document access URLs
            List<Map<String, Object>> response = pendingVerifications.stream()
                    .map(this::mapVerificationWithDocumentLinks)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: Get specific verification with document links
     */
    @GetMapping("/admin/verification/{verificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getVerificationDetails(@PathVariable String verificationId) {
        try {
            TutorVerification verification = verificationService.getVerificationById(verificationId);
            Map<String, Object> response = mapVerificationWithDocumentLinks(verification);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Update profile picture (Tutor only)
     */
    @PutMapping("/update-profile-picture")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<?> updateProfilePicture(
            @RequestParam MultipartFile profilePicture,
            Authentication auth) {

        try {
            String userId = auth.getName();
            AppUser user = userService.getUserById(userId);

            TutorVerification verification = verificationService.updateProfilePicture(
                    user.getEmail(),
                    profilePicture
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile picture updated successfully",
                    "profilePictureUrl", verification.getProfilePicturePath(),
                    "updatedAt", verification.getUpdatedAt()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get current tutor profile picture
     */
    @GetMapping("/profile-picture")
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<?> getProfilePicture(Authentication auth) {
        try {
            String userId = auth.getName();
            AppUser user = userService.getUserById(userId);

            TutorVerification verification = verificationService.getVerificationByEmail(user.getEmail())
                    .orElseThrow(() -> new RuntimeException("Tutor verification not found"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "profilePicture", Map.of(
                            "url", verification.getProfilePicturePath(),
                            "originalName", verification.getProfilePictureOriginalName(),
                            "updatedAt", verification.getUpdatedAt()
                    )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }


    /**
     * Helper method to map verification with document access links
     */
    /**
     * Helper method to map verification with document access links
     */
    private Map<String, Object> mapVerificationWithDocumentLinks(TutorVerification verification) {
        Map<String, Object> verificationData = new HashMap<>();

        // Basic info
        verificationData.put("id", verification.getId());
        verificationData.put("email", verification.getEmail());
        verificationData.put("fullName", verification.getFullName());
        verificationData.put("phone", verification.getPhone());

        // ✅ ADD THESE QUALIFICATION FIELDS
        verificationData.put("bio", verification.getBio());
        verificationData.put("qualifications", verification.getQualifications());
        verificationData.put("experience", verification.getExperience());
        verificationData.put("specializations", verification.getSpecializations());

        if (verification.getProfilePicturePath() != null) {
            verificationData.put("profilePicture", Map.of(
                    "url", verification.getProfilePicturePath(),
                    "originalName", verification.getProfilePictureOriginalName()
            ));
        }

        // Status info
        verificationData.put("status", verification.getStatus().name());
        verificationData.put("statusDescription", verification.getStatus().getDescription());
        verificationData.put("termsAccepted", verification.getTermsAccepted());
        verificationData.put("createdAt", verification.getCreatedAt());
        verificationData.put("updatedAt", verification.getUpdatedAt());

        // Document information
        Map<String, Object> documents = new HashMap<>();

        if (verification.getIdDocumentPath() != null) {
            documents.put("idDocument", Map.of(
                    "originalName", verification.getIdDocumentOriginalName(),
                    "downloadUrl", "/api/files/verification-documents/" + verification.getIdDocumentPath(),
                    "viewUrl", "/api/files/verification-documents/view/" + verification.getIdDocumentPath()
            ));
        }

        if (verification.getCertificatePath() != null) {
            documents.put("certificate", Map.of(
                    "originalName", verification.getCertificateOriginalName(),
                    "downloadUrl", "/api/files/verification-documents/" + verification.getCertificatePath(),
                    "viewUrl", "/api/files/verification-documents/view/" + verification.getCertificatePath()
            ));
        }

        verificationData.put("documents", documents);

        if (verification.getRejectionReason() != null) {
            verificationData.put("rejectionReason", verification.getRejectionReason());
        }

        return verificationData;
    }


    /**
     * Helper method for JWT token validation (for future use)
     */
    public boolean validateTutorToken(String token) {
        try {
            var claims = jwtUtil.validateAndParse(token).getPayload();
            String role = claims.get("role", String.class);
            String status = claims.get("status", String.class);

            return "TUTOR".equals(role) && "APPROVED".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Utility method to create error responses
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    /**
     * Get device info from request
     */
    private String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        return (userAgent != null ? userAgent : "Unknown") + " - " + remoteAddr;
    }

    /**
     * Calculate refresh token expiry in seconds
     */
    private long calculateRefreshTokenExpSeconds(RefreshToken refreshToken) {
        Instant now = Instant.now();
        Instant expiry = refreshToken.getExpiryDate();
        return expiry.getEpochSecond() - now.getEpochSecond();
    }

}
