package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final UserRepository users; private final CourseRepository courses;
    private final GradeRepository grades; private final AssignmentRepository assignments;
    private final PasswordEncoder encoder;
    public ApiController(UserRepository users, CourseRepository courses, GradeRepository grades, AssignmentRepository assignments, PasswordEncoder encoder) {
        this.users = users; this.courses = courses; this.grades = grades; this.assignments = assignments; this.encoder = encoder;
    }

    @GetMapping("/me")
    Map<String, Object> me(Authentication auth) {
        User user = current(auth);
        return Map.of("username", user.getUsername(), "fullName", user.getFullName(), "role", user.getRole());
    }
    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.password() == null || request.fullName() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Всички полета са задължителни");
        if (users.findByUsername(request.username()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Потребителското име вече съществува");
        User user = users.save(new User(request.username(), encoder.encode(request.password()), request.fullName(), Role.STUDENT));
        return Map.of("username", user.getUsername(), "message", "Регистрацията е успешна");
    }
    @GetMapping("/courses")
    List<Map<String, Object>> courses() {
        return courses.findAll().stream().map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId()); item.put("name", c.getName()); item.put("description", c.getDescription());
            return item;
        }).toList();
    }
    @GetMapping("/grades")
    List<Map<String, Object>> grades(Authentication auth) {
        User user = current(auth);
        var result = user.getRole() == Role.STUDENT ? grades.findByStudentUsername(user.getUsername()) : grades.findAll();
        return result.stream().map(g -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", g.getId()); item.put("student", g.getStudent().getFullName());
            item.put("course", g.getCourse().getName()); item.put("value", g.getValue());
            item.put("comment", Objects.toString(g.getComment(), "")); item.put("date", g.getDate());
            return item;
        }).toList();
    }
    @PostMapping("/grades")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> addGrade(@RequestBody GradeRequest request, Authentication auth) {
        requireTeacher(auth);
        User student = users.findById(request.studentId()).orElseThrow(() -> notFound("Студентът"));
        Course course = courses.findById(request.courseId()).orElseThrow(() -> notFound("Дисциплината"));
        if (request.value() < 2 || request.value() > 6) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Оценката трябва да е между 2 и 6");
        Grade grade = grades.save(new Grade(student, course, request.value(), request.comment()));
        return Map.of("id", grade.getId(), "message", "Оценката е добавена");
    }
    @GetMapping("/assignments")
    List<Map<String, Object>> assignments() {
        return assignments.findAll().stream().map(a -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId()); item.put("title", a.getTitle());
            item.put("description", Objects.toString(a.getDescription(), ""));
            item.put("course", a.getCourse().getName()); item.put("dueDate", a.getDueDate());
            return item;
        }).toList();
    }
    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> addAssignment(@RequestBody AssignmentRequest request, Authentication auth) {
        requireTeacher(auth);
        Course course = courses.findById(request.courseId()).orElseThrow(() -> notFound("Дисциплината"));
        Assignment a = assignments.save(new Assignment(request.title(), request.description(), course, request.dueDate()));
        return Map.of("id", a.getId(), "message", "Задачата е създадена");
    }
    private User current(Authentication auth) { return users.findByUsername(auth.getName()).orElseThrow(() -> notFound("Потребителят")); }
    private void requireTeacher(Authentication auth) { if (!current(auth).getRole().equals(Role.TEACHER)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Само преподаватели имат достъп"); }
    private ResponseStatusException notFound(String item) { return new ResponseStatusException(HttpStatus.NOT_FOUND, item + " не е намерен"); }
    public record RegisterRequest(String username, String password, String fullName) {}
    public record GradeRequest(Long studentId, Long courseId, double value, String comment) {}
    public record AssignmentRequest(String title, String description, Long courseId, LocalDate dueDate) {}
}
