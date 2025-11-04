package com.example.learnverse.tutor.service;

import com.example.learnverse.activity.repository.ActivityRepository;
import com.example.learnverse.enrollment.model.Enrollment;
import com.example.learnverse.enrollment.repository.EnrollmentRepository;
import com.example.learnverse.payment.model.Transaction;
import com.example.learnverse.payment.repository.TransactionRepository;
import com.example.learnverse.tutor.dto.TutorDashboardStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TutorDashboardService {

    private final TransactionRepository transactionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ActivityRepository activityRepository;

    /**
     * Get complete dashboard stats for tutor
     */
    public TutorDashboardStats getDashboardStats(String tutorId) {
        log.info("Fetching dashboard stats for tutor: {}", tutorId);

        return TutorDashboardStats.builder()
                .totalEarnings(calculateTotalEarnings(tutorId))
                .monthlyEarnings(calculateMonthlyEarnings(tutorId))
                .pendingEarnings(calculatePendingEarnings(tutorId))
                .totalStudents(countTotalStudents(tutorId))
                .totalActivities(countTotalActivities(tutorId))
                .topActivities(getTopActivities(tutorId))
                .recentEnrollments(getRecentEnrollments(tutorId))
                .revenueChart(getRevenueChartData(tutorId))
                .build();
    }

    /**
     * Calculate total earnings (all time)
     */
    private Double calculateTotalEarnings(String tutorId) {
        List<Transaction> transactions = transactionRepository
                .findByTutorIdOrderByCreatedAtDesc(tutorId);

        return transactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SETTLED ||
                        t.getStatus() == Transaction.TransactionStatus.WITHDRAWN)
                .mapToDouble(Transaction::getTutorEarning)
                .sum();
    }

    /**
     * Calculate current month earnings
     */
    private Double calculateMonthlyEarnings(String tutorId) {
        LocalDateTime startOfMonth = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        Instant startInstant = startOfMonth.atZone(ZoneId.systemDefault()).toInstant();
        Instant now = Instant.now();

        List<Transaction> monthlyTransactions = transactionRepository
                .findByTutorIdAndCreatedAtBetween(tutorId, startInstant, now);

        return monthlyTransactions.stream()
                .filter(t -> t.getStatus() == Transaction.TransactionStatus.SETTLED ||
                        t.getStatus() == Transaction.TransactionStatus.WITHDRAWN)
                .mapToDouble(Transaction::getTutorEarning)
                .sum();
    }

    /**
     * Calculate pending earnings (not yet settled)
     */
    private Double calculatePendingEarnings(String tutorId) {
        List<Transaction> pendingTransactions = transactionRepository
                .findByTutorIdAndStatus(tutorId, Transaction.TransactionStatus.PENDING);

        return pendingTransactions.stream()
                .mapToDouble(Transaction::getTutorEarning)
                .sum();
    }

    /**
     * Count total unique students enrolled
     */
    private Integer countTotalStudents(String tutorId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByTutorIdOrderByEnrolledAtDesc(tutorId);

        return (int) enrollments.stream()
                .map(Enrollment::getUserId)
                .distinct()
                .count();
    }

    /**
     * Count total activities created by tutor
     */
    private Integer countTotalActivities(String tutorId) {
        return (int) activityRepository.countByTutorId(tutorId);
    }

    /**
     * Get top performing activities
     */
    private List<TutorDashboardStats.ActivityPerformance> getTopActivities(String tutorId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByTutorIdOrderByEnrolledAtDesc(tutorId);

        // Group by activity
        Map<String, List<Enrollment>> enrollmentsByActivity = enrollments.stream()
                .collect(Collectors.groupingBy(Enrollment::getActivityId));

        // Calculate performance for each activity
        return enrollmentsByActivity.entrySet().stream()
                .map(entry -> {
                    String activityId = entry.getKey();
                    List<Enrollment> activityEnrollments = entry.getValue();

                    // Calculate total revenue from transactions
                    Double revenue = transactionRepository
                            .findSettledTransactionsByTutorId(tutorId)
                            .stream()
                            .filter(t -> t.getActivityId().equals(activityId))
                            .mapToDouble(Transaction::getTutorEarning)
                            .sum();

                    return TutorDashboardStats.ActivityPerformance.builder()
                            .activityId(activityId)
                            .activityTitle(activityEnrollments.get(0).getActivityTitle())
                            .enrolledStudents(activityEnrollments.size())
                            .totalRevenue(revenue)
                            .averageRating(4.5) // TODO: Calculate from reviews
                            .build();
                })
                .sorted(Comparator.comparing(TutorDashboardStats.ActivityPerformance::getTotalRevenue).reversed())
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Get recent enrollments (last 10)
     */
    private List<TutorDashboardStats.RecentEnrollment> getRecentEnrollments(String tutorId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByTutorIdOrderByEnrolledAtDesc(tutorId);

        return enrollments.stream()
                .limit(10)
                .map(enrollment -> TutorDashboardStats.RecentEnrollment.builder()
                        .enrollmentId(enrollment.getId())
                        .studentName(enrollment.getUserName())
                        .activityTitle(enrollment.getActivityTitle())
                        .enrolledAt(getTimeAgo(enrollment.getEnrolledAt()))
                        .amountEarned(enrollment.getAmountPaid() != null ?
                                enrollment.getAmountPaid() * 0.9 : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get revenue chart data (last 6 months)
     */
    private List<TutorDashboardStats.MonthlyRevenue> getRevenueChartData(String tutorId) {
        List<TutorDashboardStats.MonthlyRevenue> chartData = new ArrayList<>();

        YearMonth currentMonth = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);

            LocalDateTime startOfMonth = month.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = month.atEndOfMonth().atTime(23, 59, 59);

            Instant start = startOfMonth.atZone(ZoneId.systemDefault()).toInstant();
            Instant end = endOfMonth.atZone(ZoneId.systemDefault()).toInstant();

            List<Transaction> monthTransactions = transactionRepository
                    .findByTutorIdAndCreatedAtBetween(tutorId, start, end);

            Double monthRevenue = monthTransactions.stream()
                    .filter(t -> t.getStatus() == Transaction.TransactionStatus.SETTLED ||
                            t.getStatus() == Transaction.TransactionStatus.WITHDRAWN)
                    .mapToDouble(Transaction::getTutorEarning)
                    .sum();

            List<Enrollment> monthEnrollments = enrollmentRepository
                    .findByTutorIdOrderByEnrolledAtDesc(tutorId)
                    .stream()
                    .filter(e -> e.getEnrolledAt().isAfter(start) &&
                            e.getEnrolledAt().isBefore(end))
                    .collect(Collectors.toList());

            chartData.add(TutorDashboardStats.MonthlyRevenue.builder()
                    .month(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .year(month.getYear())
                    .revenue(monthRevenue)
                    .enrollments(monthEnrollments.size())
                    .build());
        }

        return chartData;
    }

    /**
     * Convert Instant to "time ago" string
     */
    private String getTimeAgo(Instant instant) {
        long minutes = ChronoUnit.MINUTES.between(instant, Instant.now());
        long hours = ChronoUnit.HOURS.between(instant, Instant.now());
        long days = ChronoUnit.DAYS.between(instant, Instant.now());

        if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else {
            return days + " days ago";
        }
    }
}
