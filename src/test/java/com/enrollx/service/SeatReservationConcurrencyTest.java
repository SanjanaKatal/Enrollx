package com.enrollx.service;

import com.enrollx.entity.*;
import com.enrollx.enums.Role;
import com.enrollx.exception.CourseFullException;
import com.enrollx.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class SeatReservationConcurrencyTest {

    @Autowired
    private SeatReservationService seatReservationService;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatReservationRepository seatReservationRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private CourseOffering limitedOffering;
    private final int NUM_THREADS = 5;
    private final List<String> studentUsernames = new ArrayList<>();

    @BeforeEach
    void setupTestData() {
        enrollmentRepository.deleteAll();
        seatReservationRepository.deleteAll();
        courseOfferingRepository.deleteAll();
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();

        Department cs = departmentRepository.save(new Department(null, "CS", "Computer Science"));
        Course course = courseRepository.save(new Course(null, "CS301", "DBMS", "Database Systems", 4, cs));

        // CRITICAL CONCURRENCY SETUP: Capacity is strictly 1 seat!
        limitedOffering = courseOfferingRepository.save(new CourseOffering(
                null, course, "Fall 2026", "2026-2027", 1, "Dr. Codd", "Lab 3", "Mon 10:00"));

        studentUsernames.clear();
        for (int i = 1; i <= NUM_THREADS; i++) {
            String uname = "concurrent_student_" + i;
            studentUsernames.add(uname);
            userRepository.save(new User(null, uname, uname + "@test.edu",
                    passwordEncoder.encode("Pass@123"), "Student " + i, "STU" + (100 + i), Role.ROLE_STUDENT));
        }
    }

    @Test
    @DisplayName("Concurrent Execution Test: Exactly 1 thread grabs the final seat under row-level locking")
    void testConcurrentReservationForFinalSeat() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(NUM_THREADS);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger fullRejectedCounter = new AtomicInteger(0);
        AtomicInteger unexpectedErrorCounter = new AtomicInteger(0);

        for (String username : studentUsernames) {
            executor.submit(() -> {
                try {
                    // Block all threads until the gun goes off
                    startGate.await();

                    // Each thread calls transactional reserveSeat simultaneously
                    seatReservationService.reserveSeat(limitedOffering.getId(), username);
                    successCounter.incrementAndGet();
                } catch (CourseFullException ex) {
                    fullRejectedCounter.incrementAndGet();
                } catch (Exception ex) {
                    unexpectedErrorCounter.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startGate.countDown();

        // Wait up to 10 seconds for all threads to terminate
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(1, successCounter.get(), "EXACTLY one student must succeed in reserving the final seat!");
        assertEquals(NUM_THREADS - 1, fullRejectedCounter.get(), "All other concurrent students must receive CourseFullException!");
        assertEquals(0, unexpectedErrorCounter.get(), "There should be zero unexpected deadlocks or exceptions!");

        // Verify database state: exactly 1 active reservation in DB
        long activeReservations = seatReservationRepository.countActiveReservations(limitedOffering.getId(), Instant.now());
        assertEquals(1, activeReservations, "Database must hold exactly 1 active reservation row!");
    }
}
