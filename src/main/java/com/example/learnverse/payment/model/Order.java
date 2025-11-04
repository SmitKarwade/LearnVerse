package com.example.learnverse.payment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private String id;

    // Custom order ID format: ORD_YYYYMMDD_XXXX
    private String orderId;

    // Razorpay IDs
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    // User & Activity details
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;           // ✅ ADDED THIS FIELD
    private String activityId;
    private String activityTitle;
    private String tutorId;

    // Student enrollment details (captured during enrollment)
    private String educationalBackground;      // ✅ ADDED THIS FIELD
    private String reasonForEnrollment;        // ✅ ADDED THIS FIELD

    // Payment details
    private Double amount;              // Base amount in rupees
    private Double discountAmount;      // Discount applied
    private Double taxAmount;           // GST 18%
    private Double totalAmount;         // Final amount to pay
    private String currency;            // "INR"

    // Status tracking
    private OrderStatus status;
    private String paymentMethod;       // "UPI", "Card", "NetBanking", "Wallet"

    // Timestamps
    private Instant createdAt;
    private Instant paidAt;
    private Instant failedAt;
    private Instant refundedAt;

    // Additional info
    private String failureReason;
    private String refundReason;

    public enum OrderStatus {
        PENDING,      // Order created, awaiting payment
        PROCESSING,   // Payment in progress
        COMPLETED,    // Payment successful
        FAILED,       // Payment failed
        REFUNDED,     // Payment refunded
        CANCELLED     // Order cancelled
    }
}