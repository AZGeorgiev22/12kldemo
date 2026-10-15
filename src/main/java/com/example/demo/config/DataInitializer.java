package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDate;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seed(UserRepository users, CourseRepository courses, GradeRepository grades,
                           AssignmentRepository assignments, PasswordEncoder encoder) {
        return args -> {
            if (users.findByUsername("admin").isEmpty()) {
                users.save(new User("admin", encoder.encode("admin123"), "System Administrator", Role.ADMIN));
            }
            if (users.count() > 1) return;
            User teacher = users.save(new User("teacher", encoder.encode("teacher123"), "Мария Иванова", Role.TEACHER));
            User student = users.save(new User("student", encoder.encode("student123"), "Аспарух Георгиев", Role.STUDENT));
            Course java = courses.save(new Course("Java програмиране", "Обектно-ориентирано програмиране и Spring Boot", teacher));
            Course databases = courses.save(new Course("Бази от данни", "Проектиране и работа с релационни бази данни", teacher));
            grades.save(new Grade(student, java, 5.75, "Много добра работа по проекта"));
            grades.save(new Grade(student, databases, 5.25, "Успешно решени практически задачи"));
            assignments.save(new Assignment("REST API проект", "Създайте CRUD API със Spring Boot.", java, LocalDate.now().plusDays(14)));
        };
    }
}
