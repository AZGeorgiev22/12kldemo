package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
public class Assignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    private String description;
    @ManyToOne(optional = false) private Course course;
    @Column(nullable = false) private LocalDate dueDate;
    private LocalTime dueTime;

    protected Assignment() {}
    public Assignment(String title, String description, Course course, LocalDate dueDate) {
        this(title, description, course, dueDate, null);
    }
    public Assignment(String title, String description, Course course, LocalDate dueDate, LocalTime dueTime) {
        this.title = title; this.description = description; this.course = course; this.dueDate = dueDate;
        this.dueTime = dueTime;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Course getCourse() { return course; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalTime getDueTime() { return dueTime; }
}
