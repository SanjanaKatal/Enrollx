package com.enrollx.util;

import com.enrollx.entity.*;
import com.enrollx.enums.EnrollmentStatus;
import com.enrollx.enums.Role;
import com.enrollx.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${enrollx.init.seed-sample-data:true}")
    private boolean seedSampleData;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           CourseRepository courseRepository,
                           CourseOfferingRepository courseOfferingRepository,
                           EnrollmentRepository enrollmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!seedSampleData) {
            return;
        }

        // 1. Ensure Admin exists
        if (!userRepository.existsByUsername("admin")) {
            log.info("Creating default Administrator account: admin / Admin@123");
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@enrollx.edu");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFullName("System Administrator");
            admin.setRole(Role.ROLE_ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }

        // 2. Ensure Departments exist
        if (departmentRepository.count() == 0) {
            log.info("Seeding academic departments...");
            departmentRepository.save(new Department(null, "CS", "Computer Science and Engineering"));
            departmentRepository.save(new Department(null, "IT", "Information Technology"));
            departmentRepository.save(new Department(null, "ECE", "Electronics and Communication Engineering"));
            departmentRepository.save(new Department(null, "ME", "Mechanical Engineering"));
        }

        // 3. Ensure Sample Students exist
        if (!userRepository.existsByUsername("student1")) {
            log.info("Seeding sample student accounts...");
            createStudent("student1", "alice.johnson@enrollx.edu", "Alice Johnson", "STU2026001");
            createStudent("student2", "bob.smith@enrollx.edu", "Bob Smith", "STU2026002");
            createStudent("student3", "carol.davis@enrollx.edu", "Carol Davis", "STU2026003");
            createStudent("student4", "david.wilson@enrollx.edu", "David Wilson", "STU2026004");
        }

        // 4. Ensure Courses exist
        if (courseRepository.count() == 0) {
            log.info("Seeding course catalog...");
            Department cs = departmentRepository.findByCode("CS").orElse(null);
            Department it = departmentRepository.findByCode("IT").orElse(null);
            Department ece = departmentRepository.findByCode("ECE").orElse(null);

            if (cs != null) {
                Course c1 = courseRepository.save(new Course(null, "CS101", "Introduction to Computer Science", "Foundations of computing, algorithmic thinking, and basic data structures.", 4, cs));
                Course c2 = courseRepository.save(new Course(null, "CS301", "Database Management Systems", "Relational database design, SQL, normalization, concurrency control, and transactions.", 4, cs));
                Course c3 = courseRepository.save(new Course(null, "CS305", "Operating Systems", "Processes, CPU scheduling, synchronization, deadlocks, and virtual memory.", 4, cs));
                Course c4 = courseRepository.save(new Course(null, "CS401", "Distributed Systems & Cloud", "Consensus protocols, CAP theorem, distributed transactions, and cloud architectures.", 4, cs));

                if (courseOfferingRepository.count() == 0) {
                    log.info("Seeding course offerings for Fall 2026...");
                    courseOfferingRepository.save(new CourseOffering(null, c1, "Fall 2026", "2026-2027", 40, "Dr. Alan Turing", "Hall 101", "Mon/Wed 09:00 - 10:30"));
                    CourseOffering cs301Offering = courseOfferingRepository.save(new CourseOffering(null, c2, "Fall 2026", "2026-2027", 3, "Dr. Edgar Codd", "Lab 3", "Tue/Thu 11:00 - 12:30"));
                    courseOfferingRepository.save(new CourseOffering(null, c3, "Fall 2026", "2026-2027", 35, "Dr. Dennis Ritchie", "Room 204", "Mon/Wed 14:00 - 15:30"));
                    courseOfferingRepository.save(new CourseOffering(null, c4, "Fall 2026", "2026-2027", 20, "Dr. Leslie Lamport", "Room 305", "Tue/Thu 14:00 - 15:30"));

                    // Seed 1 confirmed enrollment for student1 in CS301 (leaving 2 seats)
                    userRepository.findByUsername("student1").ifPresent(student -> {
                        enrollmentRepository.save(new Enrollment(cs301Offering, student));
                    });
                }
            }

            if (it != null) {
                Course c = courseRepository.save(new Course(null, "IT202", "Web Application Development", "Full-stack web application development using Spring Boot and HTML5.", 3, it));
                courseOfferingRepository.save(new CourseOffering(null, c, "Fall 2026", "2026-2027", 25, "Prof. Tim Berners-Lee", "Lab 1", "Fri 10:00 - 13:00"));
            }

            if (ece != null) {
                Course c = courseRepository.save(new Course(null, "EC201", "Digital Electronics & Logic Design", "Boolean algebra, logic gates, combinational and sequential circuit design.", 3, ece));
                courseOfferingRepository.save(new CourseOffering(null, c, "Fall 2026", "2026-2027", 30, "Dr. Claude Shannon", "Hall 102", "Mon/Wed 11:00 - 12:30"));
            }
        }
    }

    private void createStudent(String username, String email, String fullName, String studentId) {
        User student = new User();
        student.setUsername(username);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode("Student@123"));
        student.setFullName(fullName);
        student.setStudentId(studentId);
        student.setRole(Role.ROLE_STUDENT);
        student.setEnabled(true);
        userRepository.save(student);
    }
}
