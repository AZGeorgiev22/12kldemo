package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private String fullName;
    private String email;
    private String phone;
    @Column(nullable = false)
    private boolean active = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Role role;
    @Column
    private LocalDateTime createdAt;

    protected User() {}
    public User(String username, String password, String fullName, Role role) {
        this.username = username; this.password = password; this.fullName = fullName; this.role = role;
        this.createdAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public boolean isActive() { return active; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void updateProfile(String fullName, String email, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }
    public void changeRole(Role role) { this.role = role; }
    public void deactivate() { this.active = false; }
}
