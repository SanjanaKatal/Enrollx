package com.enrollx.service;

import com.enrollx.dto.ReservationResponseDto;
import com.enrollx.entity.CourseOffering;
import com.enrollx.entity.SeatReservation;
import com.enrollx.entity.User;
import com.enrollx.enums.EnrollmentStatus;
import com.enrollx.enums.ReservationStatus;
import com.enrollx.exception.CourseFullException;
import com.enrollx.exception.DuplicateEnrollmentException;
import com.enrollx.exception.ReservationExpiredException;
import com.enrollx.exception.ResourceNotFoundException;
import com.enrollx.repository.CourseOfferingRepository;
import com.enrollx.repository.EnrollmentRepository;
import com.enrollx.repository.SeatReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class SeatReservationService {

    private static final Logger log = LoggerFactory.getLogger(SeatReservationService.class);

    private final SeatReservationRepository seatReservationRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;

    @Value("${enrollx.reservation.duration-minutes:5}")
    private int reservationDurationMinutes;

    public SeatReservationService(SeatReservationRepository seatReservationRepository,
                                  CourseOfferingRepository courseOfferingRepository,
                                  EnrollmentRepository enrollmentRepository,
                                  UserService userService) {
        this.seatReservationRepository = seatReservationRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userService = userService;
    }

    /**
     * CORE CONCURRENCY FEATURE: Real-time pessimistic row-level seat locking.
     * 1. Begins atomic database transaction.
     * 2. Acquires exclusive row lock (SELECT ... FOR UPDATE) on CourseOffering.
     * 3. Validates duplicate enrollment and active reservation prevention.
     * 4. Calculates confirmed enrollments + active unexpired reservations.
     * 5. Atomically reserves seat if capacity is available.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SeatReservation reserveSeat(Long offeringId, String username) {
        log.info("Attempting to reserve seat for offering ID: {} by user: {}", offeringId, username);

        User user = userService.findByUsername(username);

        // 1. Acquire pessimistic write lock on the CourseOffering row
        CourseOffering offering = courseOfferingRepository.findWithLockById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found with ID: " + offeringId));

        if (!offering.isActive()) {
            throw new IllegalArgumentException("This course offering is currently inactive.");
        }

        // 2. Prevent duplicate enrollment
        boolean alreadyEnrolled = enrollmentRepository.existsByCourseOfferingIdAndUserIdAndStatus(
                offeringId, user.getId(), EnrollmentStatus.ENROLLED);
        if (alreadyEnrolled) {
            throw new DuplicateEnrollmentException("You are already enrolled in " + 
                    offering.getCourse().getCode() + " (" + offering.getCourse().getTitle() + ").");
        }

        Instant now = Instant.now();

        // 3. Prevent duplicate active reservation for same offering
        seatReservationRepository.findActiveReservationForUserAndOffering(user.getId(), offeringId, now)
                .ifPresent(existing -> {
                    throw new DuplicateEnrollmentException("You already hold an active reservation for " + 
                            offering.getCourse().getCode() + ". Please confirm or cancel it first.");
                });

        // 4. Calculate confirmed enrollments and active reservations
        long confirmedCount = enrollmentRepository.countByCourseOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED);
        long activeReservedCount = seatReservationRepository.countActiveReservations(offeringId, now);

        int availableSeats = offering.getCapacity() - (int) (confirmedCount + activeReservedCount);

        log.debug("Offering ID: {} | Capacity: {} | Enrolled: {} | Reserved: {} | Available: {}",
                offeringId, offering.getCapacity(), confirmedCount, activeReservedCount, availableSeats);

        // 5. Enforce seat capacity limit
        if (availableSeats <= 0) {
            log.warn("Seat reservation rejected: Offering {} is FULL.", offeringId);
            throw new CourseFullException("Sorry, all seats for " + offering.getCourse().getCode() + 
                    " are currently filled or reserved. Please check back later.");
        }

        // 6. Create temporary reservation with configured TTL (e.g. 5 minutes)
        Instant expiresAt = now.plus(Duration.ofMinutes(reservationDurationMinutes));
        SeatReservation reservation = new SeatReservation(offering, user, expiresAt);

        SeatReservation savedReservation = seatReservationRepository.save(reservation);
        log.info("Seat reserved successfully! Reservation ID: {}, Expires at: {}", savedReservation.getId(), expiresAt);

        return savedReservation;
    }

    /**
     * Retrieves reservation and verifies whether it has expired against server-side time.
     */
    @Transactional
    public ReservationResponseDto getReservationDetails(Long reservationId, String username) {
        SeatReservation reservation = seatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (!reservation.getUser().getUsername().equals(username)) {
            throw new SecurityException("Unauthorized access to reservation.");
        }

        Instant now = Instant.now();
        boolean isExpired = false;

        // Auto-update status if expired
        if (reservation.getStatus() == ReservationStatus.PENDING && reservation.getExpiresAt().isBefore(now)) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            seatReservationRepository.save(reservation);
            isExpired = true;
        }

        long secondsRemaining = Math.max(0, Duration.between(now, reservation.getExpiresAt()).getSeconds());
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            secondsRemaining = 0;
        }

        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setReservationId(reservation.getId());
        dto.setOfferingId(reservation.getCourseOffering().getId());
        dto.setCourseCode(reservation.getCourseOffering().getCourse().getCode());
        dto.setCourseTitle(reservation.getCourseOffering().getCourse().getTitle());
        dto.setSemester(reservation.getCourseOffering().getSemester());
        dto.setInstructor(reservation.getCourseOffering().getInstructor());
        dto.setReservedAt(reservation.getReservedAt());
        dto.setExpiresAt(reservation.getExpiresAt());
        dto.setSecondsRemaining(secondsRemaining);
        dto.setStatus(reservation.getStatus().name());
        dto.setExpired(isExpired || reservation.getStatus() == ReservationStatus.EXPIRED);

        return dto;
    }

    /**
     * Cancels an active reservation, instantly unlocking the seat for others.
     */
    @Transactional
    public void cancelReservation(Long reservationId, String username) {
        SeatReservation reservation = seatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with ID: " + reservationId));

        if (!reservation.getUser().getUsername().equals(username)) {
            throw new SecurityException("Unauthorized attempt to cancel reservation.");
        }

        if (reservation.getStatus() == ReservationStatus.PENDING) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            seatReservationRepository.save(reservation);
            log.info("Reservation ID: {} successfully cancelled by user: {}", reservationId, username);
        }
    }

    @Transactional(readOnly = true)
    public List<SeatReservation> getActiveReservationsForUser(String username) {
        User user = userService.findByUsername(username);
        return seatReservationRepository.findActiveReservationsForUser(user.getId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public List<SeatReservation> getUserReservationHistory(String username) {
        User user = userService.findByUsername(username);
        return seatReservationRepository.findByUserIdOrderByReservedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<SeatReservation> getAllActiveReservations() {
        return seatReservationRepository.findByStatusOrderByReservedAtDesc(ReservationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countActiveReservations() {
        return seatReservationRepository.countTotalActiveReservations(Instant.now());
    }
}
