package com.example.learnverse.tutor.service;

import com.example.learnverse.tutor.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.learnverse.tutor.model.TutorVerification;
import com.example.learnverse.tutor.repo.TutorVerificationRepository;
import com.example.learnverse.tutor.enumcl.VerificationStatus;
import java.util.List;
import java.util.Optional;

@Service
public class TutorVerificationService {

    @Autowired
    private TutorVerificationRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    public TutorVerification createVerificationRequest(String email, String fullName, String phone,
                                                       MultipartFile idDocument, MultipartFile certificate, Boolean termsAccepted) {

        // Check existing verification status
        Optional<TutorVerification> existingVerification = repository.findByEmail(email);
        if (existingVerification.isPresent()) {
            TutorVerification existing = existingVerification.get();

            switch (existing.getStatus()) {
                case PENDING:
                    throw new RuntimeException(String.format(
                            "Your tutor verification request is already submitted and pending review. " +
                                    "Submitted on: %s. Please wait for admin approval.",
                            existing.getCreatedAt()
                    ));

                case APPROVED:
                    throw new RuntimeException(
                            "You are already a verified tutor! You can start creating activities."
                    );

                case REJECTED:
                    // Allow resubmission with helpful message
                    String message = String.format(
                            "Your previous verification was rejected. Reason: %s. " +
                                    "You can resubmit with updated documents.",
                            existing.getRejectionReason() != null ? existing.getRejectionReason() : "No reason provided"
                    );

                    // Delete the old rejected request
                    try {
                        // Also clean up old files if they exist
                        fileStorageService.deleteVerificationFiles(existing.getId());
                    } catch (Exception e) {
                        // Continue even if file deletion fails
                    }
                    repository.deleteById(existing.getId());

                    // Log the resubmission
                    System.out.println("User " + email + " is resubmitting verification after rejection");
                    break;
            }
        }

        // Validate files
        validateFiles(idDocument, certificate);

        // Create new verification request
        TutorVerification verification = new TutorVerification(email, fullName, phone, termsAccepted);

        // Save initially to get ID for file storage
        verification = repository.save(verification);

        try {
            // Store files
            String idDocumentPath = fileStorageService.storeFile(idDocument, verification.getId(), "id");
            String certificatePath = fileStorageService.storeFile(certificate, verification.getId(), "certificate");

            // Update verification with file paths
            verification.setIdDocumentPath(idDocumentPath);
            verification.setCertificatePath(certificatePath);
            verification.setIdDocumentOriginalName(idDocument.getOriginalFilename());
            verification.setCertificateOriginalName(certificate.getOriginalFilename());

            return repository.save(verification);

        } catch (Exception e) {
            // Clean up if file storage fails
            repository.deleteById(verification.getId());
            throw new RuntimeException("Failed to store verification documents", e);
        }
    }

    public TutorVerification approveVerification(String verificationId) {
        TutorVerification verification = repository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new RuntimeException("Verification request is not in pending status");
        }

        verification.setStatus(VerificationStatus.APPROVED);
        return repository.save(verification);
    }

    public TutorVerification rejectVerification(String verificationId, String reason) {
        TutorVerification verification = repository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new RuntimeException("Verification request is not in pending status");
        }

        verification.setStatus(VerificationStatus.REJECTED);
        verification.setRejectionReason(reason);
        return repository.save(verification);
    }

    public List<TutorVerification> getPendingVerifications() {
        return repository.findByStatus(VerificationStatus.PENDING);
    }

    public List<TutorVerification> getAllVerifications() {
        return repository.findAllOrderByCreatedAtDesc();
    }

    public Optional<TutorVerification> getVerificationByEmail(String email) {
        return repository.findByEmail(email);
    }

    public boolean isTutorApproved(String email) {
        Optional<TutorVerification> verification = repository.findByEmailAndStatus(email, VerificationStatus.APPROVED);
        return verification.isPresent();
    }

    private void validateFiles(MultipartFile idDocument, MultipartFile certificate) {
        if (idDocument == null || idDocument.isEmpty()) {
            throw new RuntimeException("ID document is required");
        }

        if (certificate == null || certificate.isEmpty()) {
            throw new RuntimeException("Certificate is required");
        }

        // Check file types
        String idContentType = idDocument.getContentType();
        String certContentType = certificate.getContentType();

        if (!isValidFileType(idContentType) || !isValidFileType(certContentType)) {
            throw new RuntimeException("Only PDF, JPEG, JPG, and PNG files are allowed");
        }

        // Check file sizes (10MB limit already handled by Spring Boot config)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (idDocument.getSize() > maxSize || certificate.getSize() > maxSize) {
            throw new RuntimeException("File size should not exceed 10MB");
        }
    }

    private boolean isValidFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png")
        );
    }
}
