package com.enrollx.service;

import com.enrollx.entity.CourseOffering;
import com.enrollx.entity.Enrollment;
import com.enrollx.entity.SeatReservation;
import com.enrollx.entity.User;
import com.enrollx.enums.EnrollmentStatus;
import com.enrollx.enums.ReservationStatus;
import com.enrollx.exception.DuplicateEnrollmentException;
import com.enrollx.exception.ReservationExpiredException;
import com.enrollx.exception.ResourceNotFoundException;
import com.enrollx.repository.EnrollmentRepository;
import com.enrollx.repository.SeatReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final SeatReservationRepository seatReservationRepository;
    private final UserService userService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             SeatReservationRepository seatReservationRepository,
                             UserService userService) {
        this.enrollmentRepository = enrollmentRepository;
        this.seatReservationRepository = seatReservationRepository;
        this.userService = userService;
    }

    /**
     * ATOMIC CONFIRMATION WORKFLOW:
     * 1. Begins atomic transaction.
     * 2. Locks reservation row to prevent race conditions during confirmation.
     * 3. Validates reservation status and ensures server-side time has not exceeded expiry.
     * 4. Updates reservation to CONFIRMED.
     * 5. Inserts permanent ENROLLMENT record.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Enrollment confirmReservation(Long reservationId, String username) {
        log.info("Confirming reservation ID: {} for user: {}", reservationId, username);

        SeatReservation reservation = seatReservationRepository.findWithLockById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (!reservation.getUser().getUsername().equals(username)) {
            throw new SecurityException("You are not authorized to confirm this reservation.");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Reservation cannot be confirmed because status is " + reservation.getStatus());
        }

        Instant now = Instant.now();
        if (reservation.getExpiresAt().isBefore(now)) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            seatReservationRepository.save(reservation);
            log.warn("Reservation {} has expired at {} (current time: {})", reservationId, reservation.getExpiresAt(), now);
            throw new ReservationExpiredException("Your reservation has expired! The seat was released to other students.");
        }

        CourseOffering offering = reservation.getCourseOffering();
        User user = reservation.getUser();

        // Enforce DB unique constraint check at business level
        if (enrollmentRepository.existsByCourseOfferingIdAndUserIdAndStatus(offering.getId(), user.getId(), EnrollmentStatus.ENROLLED)) {
            throw new DuplicateEnrollmentException("You are already enrolled in this course offering.");
        }

        // 1. Mark reservation as confirmed
        reservation.setStatus(ReservationStatus.CONFIRMED);
        seatReservationRepository.save(reservation);

        // 2. Create permanent enrollment record
        Enrollment enrollment = new Enrollment(offering, user);
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        log.info("Enrollment confirmed successfully! Enrollment ID: {} for student: {}", savedEnrollment.getId(), username);
        return savedEnrollment;
    }

    /**
     * Drops an enrolled course. Frees capacity for other students immediately.
     */
    @Transactional
    public void dropCourse(Long enrollmentId, String username, boolean isAdmin) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with ID: " + enrollmentId));

        if (!isAdmin && !enrollment.getUser().getUsername().equals(username)) {
            throw new SecurityException("Unauthorized attempt to drop course.");
        }

        if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
            enrollment.setStatus(EnrollmentStatus.DROPPED);
            enrollment.setDroppedAt(Instant.now());
            enrollmentRepository.save(enrollment);
            log.info("Enrollment ID: {} marked as DROPPED by user: {}", enrollmentId, username);
        }
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getStudentActiveEnrollments(String username) {
        User user = userService.findByUsername(username);
        return enrollmentRepository.findByUserIdAndStatusOrderByEnrolledAtDesc(user.getId(), EnrollmentStatus.ENROLLED);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getStudentEnrollmentHistory(String username) {
        User user = userService.findByUsername(username);
        return enrollmentRepository.findByUserIdOrderByEnrolledAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAllByOrderByEnrolledAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Object[]> getEnrollmentStatistics() {
        return enrollmentRepository.findEnrollmentStatistics();
    }

    @Transactional(readOnly = true)
    public long countConfirmedEnrollments() {
        return enrollmentRepository.countByStatus(EnrollmentStatus.ENROLLED);
    }
}
