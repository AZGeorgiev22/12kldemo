package com.example.demo.service;

import com.example.demo.model.Event;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

@Service
public class EventReportService {
    public EventReport createReport(List<Event> events, LocalDate selectedDate, int months) {
        YearMonth end = YearMonth.from(selectedDate);
        YearMonth start = end.minusMonths(months - 1L);
        List<Event> inPeriod = events.stream()
                .filter(event -> !event.getEventDate().isBefore(start.atDay(1)))
                .filter(event -> !event.getEventDate().isAfter(end.atEndOfMonth()))
                .toList();
        List<MonthReport> monthReports = new ArrayList<>();
        for (YearMonth month = start; !month.isAfter(end); month = month.plusMonths(1)) {
            YearMonth currentMonth = month;
            monthReports.add(new MonthReport(month, inPeriod.stream()
                    .filter(event -> YearMonth.from(event.getEventDate()).equals(currentMonth))
                    .sorted(Comparator.comparing(Event::getEventDate))
                    .toList()));
        }
        return new EventReport(selectedDate, months, inPeriod.size(),
                inPeriod.stream().map(Event::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                monthReports);
    }

    public record EventReport(LocalDate selectedDate, int months, int eventCount,
                              BigDecimal totalAmount, List<MonthReport> monthsReport) {}

    public record MonthReport(YearMonth month, int eventCount, List<String> eventNames,
                              BigDecimal amount) {
        MonthReport(YearMonth month, List<Event> events) {
            this(month, events.size(), events.stream().map(Event::getName).toList(),
                    events.stream().map(Event::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    }
}
