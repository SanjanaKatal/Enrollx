package com.enrollx.scheduler;

import com.enrollx.entity.SeatReservation;
import com.enrollx.enums.ReservationStatus;
import com.enrollx.repository.SeatReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationCleanupScheduler.class);

    private final SeatReservationRepository seatReservationRepository;

    public ReservationCleanupScheduler(SeatReservationRepository seatReservationRepository) {
        this.seatReservationRepository = seatReservationRepository;
    }

    /**
     * Periodic background task that transitions stale PENDING reservations past their
     * expiry timestamp to EXPIRED status.
     * Note: Core business logic independently checks Instant.now() so consistency
     * is maintained even if this scheduler is delayed.
     */
    @Scheduled(fixedDelayString = "${enrollx.scheduler.cleanup-interval-ms:30000}")
    @Transactional
    public void cleanupExpiredReservations() {
        Instant now = Instant.now();
        List<SeatReservation> expiredReservations = seatReservationRepository.findExpiredPendingReservations(now);

        if (!expiredReservations.isEmpty()) {
            log.info("ReservationCleanupScheduler: Found {} expired reservations to mark as EXPIRED", expiredReservations.size());
            for (SeatReservation reservation : expiredReservations) {
                reservation.setStatus(ReservationStatus.EXPIRED);
            }
            seatReservationRepository.saveAll(expiredReservations);
        }
    }
}
