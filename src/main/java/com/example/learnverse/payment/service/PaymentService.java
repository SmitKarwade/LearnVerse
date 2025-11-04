package com.example.learnverse.payment.service;

import com.example.learnverse.activity.model.Activity;
import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.auth.user.AppUser;
import com.example.learnverse.auth.repo.UserRepository;
import com.example.learnverse.enrollment.dto.EnrollmentRequest;
import com.example.learnverse.payment.dto.OrderResponse;
import com.example.learnverse.payment.dto.PaymentVerificationRequest;
import com.example.learnverse.payment.model.Order;
import com.example.learnverse.payment.model.Transaction;
import com.example.learnverse.payment.repository.OrderRepository;
import com.example.learnverse.payment.repository.TransactionRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final OrderRepository orderRepository;
    private final TransactionRepository transactionRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.callback.url}")
    private String callbackUrl;

    private static final AtomicInteger orderCounter = new AtomicInteger(1);
    private static final double PLATFORM_FEE_PERCENTAGE = 0.10; // 10%
    private static final double TAX_PERCENTAGE = 0.18; // 18% GST

    /**
     * Create order for activity enrollment
     */
    @Transactional
    public OrderResponse createOrder(String userId, EnrollmentRequest request) {
        log.info("Creating order for user: {} and activity: {}", userId, request.getActivityId());

        // Get user details
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get activity details
        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));

        // Check if activity requires payment
        if (activity.getPricing() == null || activity.getPricing().getPrice() == null) {
            throw new IllegalArgumentException("Activity pricing not configured");
        }

        // Calculate amounts
        double basePrice = activity.getPricing().getDiscountPrice() != null
                ? activity.getPricing().getDiscountPrice()
                : activity.getPricing().getPrice();

        double discountAmount = activity.getPricing().getPrice() - basePrice;
        double taxAmount = basePrice * TAX_PERCENTAGE;
        double totalAmount = basePrice + taxAmount;

        // Generate custom order ID
        String orderId = generateOrderId();

        // Create Razorpay order
        try {
            org.json.JSONObject razorpayOrderRequest = new org.json.JSONObject();
            razorpayOrderRequest.put("amount", (int) (totalAmount * 100)); // Convert to paise
            razorpayOrderRequest.put("currency", "INR");
            razorpayOrderRequest.put("receipt", orderId);
            razorpayOrderRequest.put("notes", new org.json.JSONObject()
                    .put("activity_id", activity.getId())
                    .put("user_id", userId)
                    .put("activity_title", activity.getTitle())
                    .put("student_name", request.getStudentName())
                    .put("student_email", request.getStudentEmail())
                    .put("student_phone", request.getStudentPhone()));

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(razorpayOrderRequest);

            // Save order to database
            Order order = Order.builder()
                    .orderId(orderId)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .userId(userId)
                    .userName(request.getStudentName())
                    .userEmail(request.getStudentEmail())
                    .userPhone(request.getStudentPhone())              // ✅ ADDED THIS
                    .activityId(activity.getId())
                    .activityTitle(activity.getTitle())
                    .tutorId(activity.getTutorId())
                    .educationalBackground(request.getEducationalBackground())  // ✅ ADDED THIS
                    .reasonForEnrollment(request.getReasonForEnrollment())      // ✅ ADDED THIS
                    .amount(basePrice)
                    .discountAmount(discountAmount)
                    .taxAmount(taxAmount)
                    .totalAmount(totalAmount)
                    .currency("INR")
                    .status(Order.OrderStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();

            orderRepository.save(order);

            log.info("Order created successfully: {}", orderId);

            // Return response for frontend
            return OrderResponse.builder()
                    .orderId(orderId)
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .razorpayKeyId(razorpayKeyId)
                    .activityTitle(activity.getTitle())
                    .amount(basePrice)
                    .discountAmount(discountAmount)
                    .taxAmount(taxAmount)
                    .totalAmount(totalAmount)
                    .currency("INR")
                    .userName(request.getStudentName())
                    .userEmail(request.getStudentEmail())
                    .userPhone(request.getStudentPhone())  // ✅ ADDED THIS
                    .callback_url(callbackUrl)
                    .build();

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage());
        }
    }

    /**
     * Get order by Razorpay order ID
     */
    public Order getOrderByRazorpayOrderId(String razorpayOrderId) {
        return orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    /**
     * Verify payment signature from Razorpay
     */
    @Transactional
    public boolean verifyPayment(PaymentVerificationRequest request) {
        log.info("Verifying payment for order: {}", request.getRazorpayOrderId());

        try {
            // Verify signature
            String generatedSignature = generateSignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId()
            );

            if (!generatedSignature.equals(request.getRazorpaySignature())) {
                log.error("Payment signature verification failed");
                return false;
            }

            // Find order
            Order order = orderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));

            // Update order status
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setRazorpayPaymentId(request.getRazorpayPaymentId());
            order.setRazorpaySignature(request.getRazorpaySignature());
            order.setPaidAt(Instant.now());
            orderRepository.save(order);

            // Create transaction record
            createTransaction(order);

            log.info("Payment verified successfully for order: {}", order.getOrderId());
            return true;

        } catch (Exception e) {
            log.error("Payment verification failed", e);
            return false;
        }
    }

    /**
     * Handle payment failure
     */
    @Transactional
    public void handlePaymentFailure(String razorpayOrderId, String reason) {
        Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        order.setStatus(Order.OrderStatus.FAILED);
        order.setFailureReason(reason);
        order.setFailedAt(Instant.now());
        orderRepository.save(order);

        log.info("Payment failed for order: {} - Reason: {}", order.getOrderId(), reason);
    }

    /**
     * Create transaction record for analytics
     */
    private void createTransaction(Order order) {
        double platformFee = order.getTotalAmount() * PLATFORM_FEE_PERCENTAGE;
        double tutorEarning = order.getTotalAmount() - platformFee;

        Transaction transaction = Transaction.builder()
                .tutorId(order.getTutorId())
                .activityId(order.getActivityId())
                .orderId(order.getOrderId())
                .totalAmount(order.getTotalAmount())
                .platformFee(platformFee)
                .tutorEarning(tutorEarning)
                .taxAmount(order.getTaxAmount())
                .status(Transaction.TransactionStatus.SETTLED)
                .createdAt(Instant.now())
                .settledAt(Instant.now())
                .build();

        transactionRepository.save(transaction);
        log.info("Transaction created for order: {}", order.getOrderId());
    }

    /**
     * Generate unique order ID: ORD_YYYYMMDD_XXXX
     */
    private String generateOrderId() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int counter = orderCounter.getAndIncrement();
        return String.format("ORD_%s_%04d", datePrefix, counter);
    }

    /**
     * Generate HMAC SHA256 signature for payment verification
     */
    private String generateSignature(String orderId, String paymentId) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Get order details
     */
    public Order getOrderById(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }
}
