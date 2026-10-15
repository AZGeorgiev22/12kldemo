package com.example.demo.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private LocalDate eventDate;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;

    protected Event() {}
    public Event(String name, LocalDate eventDate, BigDecimal amount) {
        this.name = name;
        this.eventDate = eventDate;
        this.amount = amount;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getEventDate() { return eventDate; }
    public BigDecimal getAmount() { return amount; }
}
