package com.example.learnverse.payment.controller;

import com.example.learnverse.payment.dto.PaymentVerificationRequest;
import com.example.learnverse.payment.model.Order;
import com.example.learnverse.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Verify payment after user completes payment on Razorpay
     * POST /api/payments/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request
    ) {
        log.info("Verifying payment for order: {}", request.getRazorpayOrderId());

        try {
            boolean isValid = paymentService.verifyPayment(request);

            if (isValid) {
                // Get order details to return
                Order order = paymentService.getOrderById(
                        paymentService.getOrderByRazorpayOrderId(request.getRazorpayOrderId()).getOrderId()
                );

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Payment verified successfully",
                        "orderId", order.getOrderId()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Payment verification failed"
                ));
            }
        } catch (Exception e) {
            log.error("Payment verification error", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Payment verification failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Handle payment failure
     * POST /api/payments/failure
     */
    @PostMapping("/failure")
    public ResponseEntity<Map<String, Serializable>> handlePaymentFailure(
            @RequestBody Map<String, String> request
    ) {
        log.info("Handling payment failure for order: {}", request.get("razorpayOrderId"));

        try {
            String razorpayOrderId = request.get("razorpayOrderId");
            String reason = request.getOrDefault("reason", "Payment failed");

            paymentService.handlePaymentFailure(razorpayOrderId, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Payment failure recorded"
            ));
        } catch (Exception e) {
            log.error("Failed to handle payment failure", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", "false",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get order details
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Order> getOrder(
            @AuthenticationPrincipal String userId,
            @PathVariable String orderId
    ) {
        log.info("Fetching order: {} for user: {}", orderId, userId);

        try {
            Order order = paymentService.getOrderById(orderId);

            // Verify user owns this order
            if (!order.getUserId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }

            return ResponseEntity.ok(order);
        } catch (Exception e) {
            log.error("Failed to fetch order", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Razorpay Webhook (for server-side payment confirmation)
     * POST /api/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-Razorpay-Signature") String signature
    ) {
        log.info("Received Razorpay webhook");

        try {
            // TODO: Verify webhook signature
            // TODO: Process webhook event (payment.captured, payment.failed, etc.)

            String event = (String) payload.get("event");
            log.info("Webhook event: {}", event);

            // Handle different events
            if ("payment.captured".equals(event)) {
                // Payment successful - auto-verify
                @SuppressWarnings("unchecked")
                Map<String, Object> paymentData = (Map<String, Object>) payload.get("payload");
                // Process payment confirmation
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
}