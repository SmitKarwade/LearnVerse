package com.example.learnverse.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String orderId;
    private String razorpayOrderId;
    private String razorpayKeyId;           // Public key for frontend

    private String activityTitle;
    private Double amount;
    private Double discountAmount;
    private Double taxAmount;
    private Double totalAmount;
    private String currency;

    private String userName;
    private String userEmail;
    private String userPhone;

    // For frontend to initialize Razorpay
    private String callback_url;
    private String cancel_url;
}

