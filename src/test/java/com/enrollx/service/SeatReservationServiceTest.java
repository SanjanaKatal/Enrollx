package com.enrollx.service;

import com.enrollx.entity.*;
import com.enrollx.enums.EnrollmentStatus;
import com.enrollx.enums.ReservationStatus;
import com.enrollx.enums.Role;
import com.enrollx.exception.CourseFullException;
import com.enrollx.exception.DuplicateEnrollmentException;
import com.enrollx.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class SeatReservationServiceTest {

    @Autowired
    private SeatReservationService seatReservationService;

    @Autowired
    private EnrollmentService enrollmentService;

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

    private User studentA;
    private User studentB;
    private CourseOffering singleSeatOffering;

    @BeforeEach
    void setupData() {
        enrollmentRepository.deleteAll();
        seatReservationRepository.deleteAll();
        courseOfferingRepository.deleteAll();
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
        userRepository.deleteAll();

        Department cs = departmentRepository.save(new Department(null, "CS", "Computer Science"));
        Course course = courseRepository.save(new Course(null, "CS301", "DBMS", "Database Systems", 4, cs));

        singleSeatOffering = courseOfferingRepository.save(new CourseOffering(
                null, course, "Fall 2026", "2026-2027", 1, "Dr. Edgar Codd", "Lab 1", "Mon 10:00"));

        studentA = new User(null, "alice", "alice@test.edu", passwordEncoder.encode("Pass@123"), "Alice", "STU01", Role.ROLE_STUDENT);
        studentB = new User(null, "bob", "bob@test.edu", passwordEncoder.encode("Pass@123"), "Bob", "STU02", Role.ROLE_STUDENT);

        userRepository.save(studentA);
        userRepository.save(studentB);
    }

    @Test
    @DisplayName("Should successfully reserve an available seat")
    void testSuccessfulReservation() {
        SeatReservation res = seatReservationService.reserveSeat(singleSeatOffering.getId(), "alice");

        assertNotNull(res.getId());
        assertEquals(ReservationStatus.PENDING, res.getStatus());
        assertTrue(res.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Should reject reservation when seat capacity is fully booked")
    void testCourseFullRejection() {
        // Alice reserves the only seat
        seatReservationService.reserveSeat(singleSeatOffering.getId(), "alice");

        // Bob attempts to reserve the same offering -> should fail with CourseFullException
        assertThrows(CourseFullException.class, () -> {
            seatReservationService.reserveSeat(singleSeatOffering.getId(), "bob");
        });
    }

    @Test
    @DisplayName("Should prevent the same student from holding duplicate reservations for the same offering")
    void testDuplicateReservationPrevention() {
        seatReservationService.reserveSeat(singleSeatOffering.getId(), "alice");

        assertThrows(DuplicateEnrollmentException.class, () -> {
            seatReservationService.reserveSeat(singleSeatOffering.getId(), "alice");
        });
    }

    @Test
    @DisplayName("Should atomically confirm reservation into permanent enrollment")
    void testConfirmReservation() {
        SeatReservation res = seatReservationService.reserveSeat(singleSeatOffering.getId(), "alice");
        Enrollment enrollment = enrollmentService.confirmReservation(res.getId(), "alice");

        assertNotNull(enrollment.getId());
        assertEquals(EnrollmentStatus.ENROLLED, enrollment.getStatus());

        SeatReservation updatedRes = seatReservationRepository.findById(res.getId()).orElseThrow();
        assertEquals(ReservationStatus.CONFIRMED, updatedRes.getStatus());
    }

    @Test
    @DisplayName("Cancelling reservation should release the seat immediately for another student")
    void testCancelReservationReleasesSeat() {
        SeatReservation resAlice = seatReservationService.reserveSeat(singleSeatOffering.getId(), "alice");

        // Alice cancels hold
        seatReservationService.cancelReservation(resAlice.getId(), "alice");

        // Now Bob can reserve the freed seat!
        SeatReservation resBob = seatReservationService.reserveSeat(singleSeatOffering.getId(), "bob");
        assertNotNull(resBob.getId());
        assertEquals("bob", resBob.getUser().getUsername());
    }
}
