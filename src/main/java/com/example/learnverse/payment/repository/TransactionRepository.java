package com.example.learnverse.payment.repository;

import com.example.learnverse.payment.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {

    // Find tutor's transactions
    List<Transaction> findByTutorIdOrderByCreatedAtDesc(String tutorId);

    // Find transactions by status
    List<Transaction> findByTutorIdAndStatus(String tutorId, Transaction.TransactionStatus status);

    // Calculate total earnings for tutor
    @Query(value = "{ 'tutorId': ?0, 'status': { $in: ['SETTLED', 'WITHDRAWN'] } }")
    List<Transaction> findSettledTransactionsByTutorId(String tutorId);

    // Find transactions within date range
    List<Transaction> findByTutorIdAndCreatedAtBetween(
            String tutorId,
            Instant startDate,
            Instant endDate
    );
}
