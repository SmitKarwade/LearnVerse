package com.example.learnverse.tutor.controller;


import com.example.learnverse.auth.service.AuthService;
import com.example.learnverse.auth.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.learnverse.tutor.service.TutorVerificationService;
import com.example.learnverse.auth.jwt.JwtUtil; // Your existing JWT utility
import com.example.learnverse.tutor.model.TutorVerification;
import com.example.learnverse.tutor.enumcl.VerificationStatus;
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

    /**
     * Submit tutor verification request
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerTutor(
            HttpServletRequest request,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) MultipartFile idDocument,
            @RequestParam(required = false) MultipartFile certificate,
            @RequestParam(required = false) Boolean termsAccepted) {

        try {
            // Validate terms acceptance
            if (termsAccepted == null || !termsAccepted) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Terms and conditions must be accepted"));
            }

            // Create verification request
            TutorVerification verification = verificationService.createVerificationRequest(
                    email, fullName, phone, idDocument, certificate, termsAccepted);

            // Response for successful registration
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Verification request submitted successfully");
            response.put("verificationId", verification.getId());
            response.put("status", verification.getStatus().name());
            response.put("submittedAt", verification.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
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

    @PostMapping("/admin/approve/{verificationId}")
    public ResponseEntity<?> approveVerification(@PathVariable String verificationId) {
        try {
            TutorVerification verification = verificationService.approveVerification(verificationId);

            // Upgrade user role from USER to TUTOR
            AppUser user = authService.getUserByEmail(verification.getEmail());
            authService.upgradeUserToTutor(user.getId());

            // Generate NEW JWT token with TUTOR role
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "TUTOR");
            claims.put("status", "APPROVED");
            claims.put("tutorId", verification.getId());
            claims.put("fullName", verification.getFullName());
            claims.put("email", verification.getEmail());

            String jwtToken = jwtUtil.generateAccessToken(user.getId(), claims);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tutor verification approved successfully");
            response.put("tutorEmail", verification.getEmail());
            response.put("jwtToken", jwtToken); // New TUTOR token
            response.put("tokenExpiresIn", jwtUtil.getAccessExpSeconds());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * Admin: Reject tutor verification
     */
    @PostMapping("/admin/reject/{verificationId}")
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
     * Helper method to map verification with document access links
     */
    private Map<String, Object> mapVerificationWithDocumentLinks(TutorVerification verification) {
        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("id", verification.getId());
        verificationData.put("email", verification.getEmail());
        verificationData.put("fullName", verification.getFullName());
        verificationData.put("phone", verification.getPhone());
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
}
