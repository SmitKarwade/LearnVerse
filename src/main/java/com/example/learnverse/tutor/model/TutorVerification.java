package com.example.learnverse.tutor.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import com.example.learnverse.tutor.enumcl.VerificationStatus;
import java.time.LocalDateTime;

@Getter
@Document(collection = "tutor_verifications")
public class TutorVerification {

    // Getters and Setters
    @Setter
    @Id
    private String id;

    @Setter
    @Indexed(unique = true)
    private String email;

    @Setter
    private String fullName;
    @Setter
    private String phone;

    // File storage paths
    @Setter
    private String idDocumentPath;
    @Setter
    private String certificatePath;

    // Original file names for display
    @Setter
    private String idDocumentOriginalName;
    @Setter
    private String certificateOriginalName;

    // Verification details
    @Setter
    private Boolean termsAccepted;
    private VerificationStatus status;
    @Setter
    private String rejectionReason; // Only filled when status is REJECTED

    // Timestamps
    @Setter
    private LocalDateTime createdAt;
    @Setter
    private LocalDateTime updatedAt;

    // Constructors
    public TutorVerification() {
        this.createdAt = LocalDateTime.now();
        this.status = VerificationStatus.PENDING;
    }

    public TutorVerification(String email, String fullName, String phone, Boolean termsAccepted) {
        this();
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.termsAccepted = termsAccepted;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now(); // Auto-update timestamp when status changes
    }

    // Helper methods
    public boolean isApproved() {
        return this.status == VerificationStatus.APPROVED;
    }

    public boolean isPending() {
        return this.status == VerificationStatus.PENDING;
    }

    public boolean isRejected() {
        return this.status == VerificationStatus.REJECTED;
    }

    @Override
    public String toString() {
        return "TutorVerification{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", status=" + status +
                ", termsAccepted=" + termsAccepted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}

