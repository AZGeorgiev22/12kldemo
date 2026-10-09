package com.example.demo.model;

import jakarta.persistence.*;

@Entity
public class Course {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String description;
    @ManyToOne(optional = false) private User teacher;

    protected Course() {}
    public Course(String name, String description, User teacher) {
        this.name = name; this.description = description; this.teacher = teacher;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public User getTeacher() { return teacher; }
}
