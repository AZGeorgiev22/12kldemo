package com.example.demo.service;

import com.example.demo.model.Assignment;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssignmentService {
    private static final Comparator<Assignment> CHRONOLOGICAL_ORDER =
            Comparator.comparing(Assignment::getDueDate)
                    .thenComparing(Assignment::getDueTime,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Assignment::getId,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    public List<Assignment> filterAndSort(List<Assignment> source, Integer week,
                                          LocalDate date, DayOfWeek day, Integer hour) {
        return source.stream()
                .filter(assignment -> date == null || assignment.getDueDate().equals(date))
                .filter(assignment -> day == null || assignment.getDueDate().getDayOfWeek() == day)
                .filter(assignment -> week == null || assignment.getDueDate()
                        .get(WeekFields.ISO.weekOfWeekBasedYear()) == week)
                .filter(assignment -> hour == null || assignment.getDueTime() != null
                        && assignment.getDueTime().getHour() == hour)
                .sorted(CHRONOLOGICAL_ORDER)
                .toList();
    }

    public List<ReportGroup> report(List<Assignment> source, LocalDate from, LocalDate to,
                                    ReportCriterion criterion) {
        return source.stream()
                .filter(assignment -> from == null || !assignment.getDueDate().isBefore(from))
                .filter(assignment -> to == null || !assignment.getDueDate().isAfter(to))
                .collect(Collectors.groupingBy(criterion.keyExtractor(), TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ReportGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    public enum ReportCriterion {
        COURSE("course", assignment -> assignment.getCourse().getName()),
        DAY("day", assignment -> assignment.getDueDate().getDayOfWeek().toString()),
        WEEK("week", assignment -> String.valueOf(assignment.getDueDate()
                .get(WeekFields.ISO.weekOfWeekBasedYear())));

        private final String value;
        private final Function<Assignment, String> keyExtractor;

        ReportCriterion(String value, Function<Assignment, String> keyExtractor) {
            this.value = value;
            this.keyExtractor = keyExtractor;
        }

        public Function<Assignment, String> keyExtractor() {
            return keyExtractor;
        }

        public static ReportCriterion from(String value) {
            for (ReportCriterion criterion : values()) {
                if (criterion.value.equalsIgnoreCase(value)) return criterion;
            }
            throw new IllegalArgumentException("Критерият трябва да е course, day или week");
        }
    }

    public record ReportGroup(String group, long count) {}
}
