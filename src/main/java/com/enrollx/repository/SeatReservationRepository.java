package com.enrollx.repository;

import com.enrollx.entity.SeatReservation;
import com.enrollx.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SeatReservation r WHERE r.id = :id")
    Optional<SeatReservation> findWithLockById(@Param("id") Long id);

    @Query("SELECT COUNT(r) FROM SeatReservation r " +
           "WHERE r.courseOffering.id = :offeringId " +
           "AND r.status = com.enrollx.enums.ReservationStatus.PENDING " +
           "AND r.expiresAt > :now")
    long countActiveReservations(@Param("offeringId") Long offeringId, @Param("now") Instant now);

    @Query("SELECT r FROM SeatReservation r " +
           "WHERE r.user.id = :userId " +
           "AND r.courseOffering.id = :offeringId " +
           "AND r.status = com.enrollx.enums.ReservationStatus.PENDING " +
           "AND r.expiresAt > :now")
    Optional<SeatReservation> findActiveReservationForUserAndOffering(
            @Param("userId") Long userId,
            @Param("offeringId") Long offeringId,
            @Param("now") Instant now);

    @Query("SELECT r FROM SeatReservation r " +
           "WHERE r.user.id = :userId " +
           "AND r.status = com.enrollx.enums.ReservationStatus.PENDING " +
           "AND r.expiresAt > :now " +
           "ORDER BY r.expiresAt ASC")
    List<SeatReservation> findActiveReservationsForUser(
            @Param("userId") Long userId,
            @Param("now") Instant now);

    @Query("SELECT r FROM SeatReservation r " +
           "WHERE r.status = com.enrollx.enums.ReservationStatus.PENDING " +
           "AND r.expiresAt <= :now")
    List<SeatReservation> findExpiredPendingReservations(@Param("now") Instant now);

    List<SeatReservation> findByUserIdOrderByReservedAtDesc(Long userId);

    List<SeatReservation> findByCourseOfferingIdOrderByReservedAtDesc(Long offeringId);

    List<SeatReservation> findByStatusOrderByReservedAtDesc(ReservationStatus status);

    @Query("SELECT COUNT(r) FROM SeatReservation r WHERE r.status = com.enrollx.enums.ReservationStatus.PENDING AND r.expiresAt > :now")
    long countTotalActiveReservations(@Param("now") Instant now);
}
