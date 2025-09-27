package com.example.learnverse.tutor.enumcl;

public enum VerificationStatus {
    PENDING("Verification request submitted and pending review"),
    APPROVED("Tutor verified and approved to create activities"),
    REJECTED("Verification request rejected");

    private final String description;

    VerificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // Helper method to check if tutor can create activities
    public boolean canCreateActivities() {
        return this == APPROVED;
    }

    // Helper method to check if verification is complete (either approved or rejected)
    public boolean isComplete() {
        return this == APPROVED || this == REJECTED;
    }
}
