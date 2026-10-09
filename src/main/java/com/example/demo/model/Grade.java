package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Grade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private User student;
    @ManyToOne(optional = false) private Course course;
    @Column(name = "grade_value", nullable = false) private double value;
    private String comment;
    @Column(nullable = false) private LocalDate date = LocalDate.now();

    protected Grade() {}
    public Grade(User student, Course course, double value, String comment) {
        this.student = student; this.course = course; this.value = value; this.comment = comment;
    }
    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Course getCourse() { return course; }
    public double getValue() { return value; }
    public String getComment() { return comment; }
    public LocalDate getDate() { return date; }
}
