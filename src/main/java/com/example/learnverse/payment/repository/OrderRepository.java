package com.example.learnverse.payment.repository;

import com.example.learnverse.payment.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    // Find by order ID
    Optional<Order> findByOrderId(String orderId);

    // Find by Razorpay order ID
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // Find user's orders
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    // Find orders for tutor's activities
    List<Order> findByTutorIdOrderByCreatedAtDesc(String tutorId);

    // Find orders by status
    List<Order> findByStatus(Order.OrderStatus status);

    // Find completed orders within date range (for analytics)
    List<Order> findByStatusAndPaidAtBetween(
            Order.OrderStatus status,
            Instant startDate,
            Instant endDate
    );
}

