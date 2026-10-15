package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AssignmentService;
import com.example.demo.service.EventReportService;
import com.example.demo.service.EventReportPdfService;
import com.example.demo.service.UserRegistrationReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final UserRepository users; private final CourseRepository courses;
    private final GradeRepository grades; private final AssignmentRepository assignments;
    private final AssignmentService assignmentService;
    private final EventRepository events;
    private final EventReportService eventReportService;
    private final EventReportPdfService eventReportPdfService;
    private final UserRegistrationReportService userRegistrationReportService;
    private final PasswordEncoder encoder;
    public ApiController(UserRepository users, CourseRepository courses, GradeRepository grades, AssignmentRepository assignments,
                         AssignmentService assignmentService, EventRepository events, EventReportService eventReportService,
                         EventReportPdfService eventReportPdfService,
                         UserRegistrationReportService userRegistrationReportService,
                         PasswordEncoder encoder) {
        this.users = users; this.courses = courses; this.grades = grades; this.assignments = assignments;
        this.assignmentService = assignmentService; this.events = events; this.eventReportService = eventReportService;
        this.eventReportPdfService = eventReportPdfService;
        this.userRegistrationReportService = userRegistrationReportService;
        this.encoder = encoder;
    }

    @GetMapping("/me")
    Map<String, Object> me(Authentication auth) {
        User user = current(auth);
        return userView(user);
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
    @GetMapping("/users")
    List<Map<String, Object>> users(Authentication auth) {
        requireAdmin(auth);
        return users.findAll().stream().map(this::userView).toList();
    }
    @GetMapping("/users/registration-report")
    Map<String, Object> registrationReport(Authentication auth) {
        requireAdmin(auth);
        UserRegistrationReportService.RegistrationReport report =
                userRegistrationReportService.create(users.findAll(), LocalDate.now());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("report", report);
        result.put("insight", userRegistrationReportService.insight(report));
        return result;
    }
    @GetMapping("/users/search")
    Map<String, Object> searchUsers(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "fullName") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        requireAdmin(auth);
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Страницата трябва да е 0 или повече, а размерът трябва да е между 1 и 100");
        Set<String> allowedSorts = Set.of("username", "fullName", "email", "phone", "role", "active");
        if (!allowedSorts.contains(sort))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Невалидно поле за сортиране");
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Посоката трябва да е asc или desc");
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Sort sorting = Sort.by(sortDirection, sort).and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(page, size, sorting);
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        Specification<User> specification = normalizedQuery.isEmpty() ? null :
                (root, criteriaQuery, builder) -> {
                    String pattern = "%" + normalizedQuery + "%";
                    return builder.or(
                            builder.like(builder.lower(root.get("username")), pattern),
                            builder.like(builder.lower(root.get("fullName")), pattern),
                            builder.like(builder.lower(root.get("email")), pattern),
                            builder.like(builder.lower(root.get("phone")), pattern),
                            builder.like(builder.lower(root.get("role").as(String.class)), pattern));
                };
        Page<User> result = users.findAll(specification, pageable);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.getContent().stream().map(this::userView).toList());
        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("first", result.isFirst());
        response.put("last", result.isLast());
        return response;
    }
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> createUser(@RequestBody UserCreateRequest request, Authentication auth) {
        requireAdmin(auth);
        validateUserInput(request.fullName(), request.email(), request.phone());
        if (request.username() == null || request.username().isBlank() || request.password() == null
                || request.password().isBlank() || request.role() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Потребителско име, парола и роля са задължителни");
        if (request.role() != Role.ADMIN && request.role() != Role.VET && request.role() != Role.RECEPTION)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Новият потребител трябва да е ADMIN, VET или RECEPTION");
        if (users.findByUsername(request.username().trim()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Потребителското име вече съществува");
        User user = new User(request.username().trim(), encoder.encode(request.password()),
                request.fullName().trim(), request.role());
        user.updateProfile(user.getFullName(), request.email(), request.phone());
        return userView(users.save(user));
    }
    @PatchMapping("/users/{id}/profile")
    Map<String, Object> updateUserProfile(@PathVariable Long id, @RequestBody ProfileRequest request,
                                          Authentication auth) {
        requireAdmin(auth);
        User user = findUser(id);
        validateUserInput(request.fullName(), request.email(), request.phone());
        user.updateProfile(request.fullName().trim(), request.email(), request.phone());
        return userView(users.save(user));
    }
    @PutMapping("/profile")
    Map<String, Object> updateOwnProfile(@RequestBody ProfileRequest request, Authentication auth) {
        User user = current(auth);
        validateUserInput(request.fullName(), request.email(), request.phone());
        user.updateProfile(request.fullName().trim(), request.email(), request.phone());
        return userView(users.save(user));
    }
    @PatchMapping("/users/{id}/role")
    Map<String, Object> changeUserRole(@PathVariable Long id, @RequestBody RoleRequest request,
                                       Authentication auth) {
        requireAdmin(auth);
        if (request.role() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ролята е задължителна");
        User user = findUser(id);
        user.changeRole(request.role());
        return userView(users.save(user));
    }
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivateUser(@PathVariable Long id, Authentication auth) {
        requireAdmin(auth);
        User user = findUser(id);
        if (user.getId().equals(current(auth).getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Администраторът не може да деактивира сам себе си");
        user.deactivate();
        users.save(user);
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
    List<Map<String, Object>> assignments(@RequestParam(name = "week", required = false) Integer week,
                                          @RequestParam(name = "date", required = false) LocalDate date,
                                          @RequestParam(name = "day", required = false) String day,
                                          @RequestParam(name = "hour", required = false) Integer hour) {
        validateWeekAndHour(week, hour);
        DayOfWeek dayOfWeek = parseDay(day);
        return assignmentService.filterAndSort(assignments.findAll(), week, date, dayOfWeek, hour).stream().map(a -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId()); item.put("title", a.getTitle());
            item.put("description", Objects.toString(a.getDescription(), ""));
            item.put("course", a.getCourse().getName()); item.put("dueDate", a.getDueDate());
            item.put("dueTime", a.getDueTime());
            return item;
        }).toList();
    }
    @GetMapping("/reports/assignments")
    List<AssignmentService.ReportGroup> assignmentReport(
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to,
            @RequestParam(defaultValue = "course") String groupBy) {
        if (from != null && to != null && from.isAfter(to))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Началната дата не може да е след крайната дата");
        try {
            return assignmentService.report(assignments.findAll(), from, to,
                    AssignmentService.ReportCriterion.from(groupBy));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }
    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> addAssignment(@RequestBody AssignmentRequest request, Authentication auth) {
        requireTeacher(auth);
        Course course = courses.findById(request.courseId()).orElseThrow(() -> notFound("Дисциплината"));
        Assignment a = assignments.save(new Assignment(request.title(), request.description(), course,
                request.dueDate(), request.dueTime()));
        return Map.of("id", a.getId(), "message", "Задачата е създадена");
    }
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Object> addEvent(@RequestBody EventRequest request, Authentication auth) {
        requireTeacher(auth);
        if (request.name() == null || request.name().isBlank() || request.eventDate() == null
                || request.amount() == null || request.amount().signum() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Името, датата и неотрицателната сума са задължителни");
        Event event = events.save(new Event(request.name().trim(), request.eventDate(), request.amount()));
        return Map.of("id", event.getId(), "message", "Евентът е създаден");
    }
    @GetMapping("/reports/events")
    EventReportService.EventReport eventReport(
            @RequestParam(defaultValue = "1") int months,
            @RequestParam(required = false) LocalDate date) {
        if (months != 1 && months != 6 && months != 12)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Периодът трябва да е 1, 6 или 12 месеца");
        return eventReportService.createReport(events.findAll(), date == null ? LocalDate.now() : date, months);
    }
    @GetMapping("/reports/events/pdf")
    ResponseEntity<byte[]> eventReportPdf(@RequestParam(defaultValue = "1") int months,
                                          @RequestParam(required = false) LocalDate date) {
        if (months != 1 && months != 6 && months != 12)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Периодът трябва да е 1, 6 или 12 месеца");
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        byte[] pdf = eventReportPdfService.create(
                eventReportService.createReport(events.findAll(), selectedDate, months));
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("event-report-" + selectedDate + "-" + months + "m.pdf").build().toString())
                .header("Content-Type", "application/pdf")
                .body(pdf);
    }
    private User current(Authentication auth) { return users.findByUsername(auth.getName()).orElseThrow(() -> notFound("Потребителят")); }
    private User findUser(Long id) { return users.findById(id).orElseThrow(() -> notFound("Потребителят")); }
    private void requireAdmin(Authentication auth) {
        if (current(auth).getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Само администратори имат достъп");
    }
    private void validateUserInput(String fullName, String email, String phone) {
        if (fullName == null || fullName.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Името е задължително");
        if (email != null && !email.isBlank() && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Невалиден email адрес");
        if (phone != null && phone.length() > 30)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Телефонът е прекалено дълъг");
    }
    private Map<String, Object> userView(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("fullName", user.getFullName());
        result.put("email", Objects.toString(user.getEmail(), ""));
        result.put("phone", Objects.toString(user.getPhone(), ""));
        result.put("role", user.getRole());
        result.put("active", user.isActive());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }
    private void requireTeacher(Authentication auth) { if (!current(auth).getRole().equals(Role.TEACHER)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Само преподаватели имат достъп"); }
    private ResponseStatusException notFound(String item) { return new ResponseStatusException(HttpStatus.NOT_FOUND, item + " не е намерен"); }
    private void validateWeekAndHour(Integer week, Integer hour) {
        if (week != null && (week < 1 || week > 53))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Седмицата трябва да е между 1 и 53");
        if (hour != null && (hour < 0 || hour > 23))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Часът трябва да е между 0 и 23");
    }
    private DayOfWeek parseDay(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return DayOfWeek.of(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            try {
                return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Денят трябва да е число от 1 до 7 или име на ден на английски");
            }
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Денят трябва да е число от 1 до 7");
        }
    }
    public record RegisterRequest(String username, String password, String fullName) {}
    public record UserCreateRequest(String username, String password, String fullName,
                                    String email, String phone, Role role) {}
    public record ProfileRequest(String fullName, String email, String phone) {}
    public record RoleRequest(Role role) {}
    public record GradeRequest(Long studentId, Long courseId, double value, String comment) {}
    public record AssignmentRequest(String title, String description, Long courseId, LocalDate dueDate, LocalTime dueTime) {}
    public record EventRequest(String name, LocalDate eventDate, BigDecimal amount) {}
}
