package com.example.learnverse.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    private String id;

    private String tutorId;
    private String activityId;
    private String orderId;

    // Financial breakdown
    private Double totalAmount;         // Total paid by student
    private Double platformFee;         // 10% platform fee
    private Double tutorEarning;        // 90% goes to tutor
    private Double taxAmount;           // GST component

    // Status
    private TransactionStatus status;

    // Timestamps
    private Instant createdAt;
    private Instant settledAt;          // When tutor receives payment
    private Instant withdrawnAt;        // When tutor withdraws

    public enum TransactionStatus {
        PENDING,      // Transaction created
        SETTLED,      // Money available for tutor
        ON_HOLD,      // Under review
        WITHDRAWN,    // Tutor has withdrawn
        REFUNDED      // Refunded to student
    }
}