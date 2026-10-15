package com.example.demo.service;

import com.example.demo.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

@Service
public class UserRegistrationReportService {
    public RegistrationReport create(List<User> users, LocalDate today) {
        LocalDate weekStart = today.minusDays(6);
        LocalDate monthStart = today.minusMonths(1).plusDays(1);
        LocalDate yearStart = today.minusYears(1).plusDays(1);
        List<UserSummary> todayUsers = users.stream()
                .filter(user -> onOrAfter(user, today))
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::summary)
                .toList();
        return new RegistrationReport(today, todayUsers,
                usersSince(users, weekStart), usersSince(users, monthStart), usersSince(users, yearStart),
                users.stream().filter(user -> onOrAfter(user, yearStart))
                        .collect(java.util.stream.Collectors.groupingBy(
                                user -> YearMonth.from(user.getCreatedAt()), java.util.TreeMap::new,
                                java.util.stream.Collectors.counting()))
                        .entrySet().stream().map(entry -> new PeriodCount(entry.getKey().toString(), entry.getValue()))
                        .toList());
    }

    public RegistrationInsight insight(RegistrationReport report) {
        long previousYearEquivalent = report.monthlyCounts().stream().limit(6)
                .mapToLong(PeriodCount::count).sum();
        long recent = report.monthlyCounts().stream().skip(Math.max(0, report.monthlyCounts().size() - 3))
                .mapToLong(PeriodCount::count).sum();
        String trend = recent > previousYearEquivalent ? "growth"
                : recent < previousYearEquivalent ? "decline" : "stable";
        return new RegistrationInsight(trend,
                "Registration activity is " + trend + " based on the latest available periods.");
    }

    private List<UserSummary> usersSince(List<User> users, LocalDate date) {
        return users.stream().filter(user -> onOrAfter(user, date))
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::summary).toList();
    }
    private boolean onOrAfter(User user, LocalDate date) {
        return user.getCreatedAt() != null && !user.getCreatedAt().toLocalDate().isBefore(date);
    }
    private UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getFullName(),
                user.getRole(), user.getCreatedAt());
    }

    public record RegistrationReport(LocalDate today, List<UserSummary> todayUsers,
                                     List<UserSummary> last7Days, List<UserSummary> lastMonth,
                                     List<UserSummary> lastYear,
                                     List<PeriodCount> monthlyCounts) {}
    public record UserSummary(Long id, String username, String fullName,
                              com.example.demo.model.Role role, LocalDateTime createdAt) {}
    public record PeriodCount(String period, long count) {}
    public record RegistrationInsight(String trend, String message) {}
}
