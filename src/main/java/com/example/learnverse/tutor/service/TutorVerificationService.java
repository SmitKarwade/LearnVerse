package com.example.learnverse.tutor.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.tutor.storage.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.learnverse.tutor.model.TutorVerification;
import com.example.learnverse.tutor.repo.TutorVerificationRepository;
import com.example.learnverse.tutor.enumcl.VerificationStatus;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class TutorVerificationService {

    @Autowired
    private TutorVerificationRepository repository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private  UserRepository userRepository;

    @Autowired
    private  ActivityRepository activityRepository;

    public TutorVerification createVerificationRequest(
            String email,
            String fullName,
            String phone,
            String bio,
            List<String> qualifications,
            String experience,
            List<String> specializations,
            MultipartFile profilePicture,
            MultipartFile idDocument,
            MultipartFile certificate,
            Boolean termsAccepted) {

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
        validateFiles(profilePicture, idDocument, certificate);


        TutorVerification verification = new TutorVerification();
        verification.setEmail(email);
        verification.setFullName(fullName);
        verification.setPhone(phone);
        verification.setBio(bio);
        verification.setQualifications(qualifications);
        verification.setExperience(experience);
        verification.setSpecializations(specializations);
        verification.setTermsAccepted(termsAccepted);
        verification.setStatus(VerificationStatus.PENDING);
        verification.setCreatedAt(LocalDateTime.now());

        // Save initially to get ID for file storage
        verification = repository.save(verification);

        try {
            String profilePicturePath = fileStorageService.storeProfilePicture(profilePicture, verification.getId());
            verification.setProfilePicturePath(profilePicturePath);
            verification.setProfilePictureOriginalName(profilePicture.getOriginalFilename());

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

    public TutorVerification getVerificationById(String verificationId) {
        return repository.findById(verificationId)
                .orElseThrow(() -> new RuntimeException("Verification request not found"));
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

    /**
     * Update profile picture for verified tutor
     */
    public TutorVerification updateProfilePicture(String email, MultipartFile newProfilePicture) throws IOException {
        TutorVerification verification = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tutor verification not found"));

        if (!verification.isApproved()) {
            throw new RuntimeException("Only verified tutors can update their profile picture");
        }

        // Validate new picture
        if (newProfilePicture == null || newProfilePicture.isEmpty()) {
            throw new RuntimeException("Profile picture is required");
        }

        if (!newProfilePicture.getContentType().startsWith("image/")) {
            throw new RuntimeException("Profile picture must be an image file");
        }

        try {
            // Delete old profile picture from Cloudinary (optional but recommended)
            if (verification.getProfilePicturePath() != null) {
                try {
                    // Extract public_id from URL and delete
                    String publicId = extractPublicIdFromUrl(verification.getProfilePicturePath());
                    fileStorageService.deleteCloudinaryImage(publicId);
                } catch (Exception e) {
                    // Continue even if deletion fails
                    System.err.println("Failed to delete old profile picture: " + e.getMessage());
                }
            }

            // Upload new profile picture
            String newProfilePicturePath = fileStorageService.storeProfilePicture(
                    newProfilePicture,
                    verification.getId()
            );

            verification.setProfilePicturePath(newProfilePicturePath);
            verification.setProfilePictureOriginalName(newProfilePicture.getOriginalFilename());
            verification.setUpdatedAt(LocalDateTime.now());

            verification = repository.save(verification);

            // ✅ Auto-update all tutor's activities with new profile picture
            updateActivitiesProfilePicture(verification.getEmail(), newProfilePicturePath);

            return verification;

        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update profile picture: " + e.getMessage(), e);
        }
    }

    /**
     * Update profile picture in all tutor's activities
     */
    private void updateActivitiesProfilePicture(String tutorEmail, String newProfilePicturePath) {
        try {
            // Find user by email
            AppUser user = userRepository.findByEmail(tutorEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update all activities by this tutor
            List<Activity> tutorActivities = activityRepository.findByTutorId(user.getId());

            for (Activity activity : tutorActivities) {
                if (activity.getInstructorDetails() != null) {
                    activity.getInstructorDetails().setProfileImage(newProfilePicturePath);
                    activity.setUpdatedAt(new java.util.Date());
                    activityRepository.save(activity);
                }
            }

            System.out.println("✅ Updated profile picture in " + tutorActivities.size() + " activities");

        } catch (Exception e) {
            System.err.println("Failed to update activities: " + e.getMessage());
            // Don't throw - profile picture is updated in verification even if activities fail
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     */
    private String extractPublicIdFromUrl(String cloudinaryUrl) {
        // Example URL: https://res.cloudinary.com/cloud/image/upload/v123/folder/image.jpg
        // Extract: folder/image (without extension)
        try {
            String[] parts = cloudinaryUrl.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Remove version number if present (v1234567890/)
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


    /**
     * Validate all required files
     */
    private void validateFiles(MultipartFile profilePicture, MultipartFile idDocument, MultipartFile certificate) {
        // ✅ Validate profile picture
        if (profilePicture == null || profilePicture.isEmpty()) {
            throw new RuntimeException("Profile picture is required");
        }

        if (!profilePicture.getContentType().startsWith("image/")) {
            throw new RuntimeException("Profile picture must be an image (JPEG, JPG, or PNG)");
        }

        // Validate ID document
        if (idDocument == null || idDocument.isEmpty()) {
            throw new RuntimeException("ID document is required");
        }

        // Validate certificate
        if (certificate == null || certificate.isEmpty()) {
            throw new RuntimeException("Certificate is required");
        }

        // Check file types
        String idContentType = idDocument.getContentType();
        String certContentType = certificate.getContentType();

        if (!isValidDocumentFileType(idContentType) || !isValidDocumentFileType(certContentType)) {
            throw new RuntimeException("Documents must be PDF, JPEG, JPG, or PNG files");
        }

        // Check file sizes (10MB limit)
        long maxSize = 10 * 1024 * 1024; // 10MB

        if (profilePicture.getSize() > maxSize) {
            throw new RuntimeException("Profile picture size should not exceed 10MB");
        }

        if (idDocument.getSize() > maxSize || certificate.getSize() > maxSize) {
            throw new RuntimeException("Document file size should not exceed 10MB");
        }
    }

    private boolean isValidDocumentFileType(String contentType) {
        return contentType != null && (
                contentType.equals("application/pdf") ||
                        contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png")
        );
    }
}
