package com.enrollx.controller;

import com.enrollx.dto.ReservationResponseDto;
import com.enrollx.dto.SeatStatusDto;
import com.enrollx.service.CourseOfferingService;
import com.enrollx.service.SeatReservationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CourseApiController {

    private final CourseOfferingService courseOfferingService;
    private final SeatReservationService seatReservationService;

    public CourseApiController(CourseOfferingService courseOfferingService,
                               SeatReservationService seatReservationService) {
        this.courseOfferingService = courseOfferingService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * REST endpoint for real-time seat availability polling.
     */
    @GetMapping("/offerings/{id}/seat-status")
    public ResponseEntity<SeatStatusDto> getSeatStatus(@PathVariable("id") Long offeringId) {
        SeatStatusDto status = courseOfferingService.getSeatStatus(offeringId);
        return ResponseEntity.ok(status);
    }

    /**
     * REST endpoint for active reservation countdown timer check.
     */
    @GetMapping("/reservations/{id}/status")
    public ResponseEntity<ReservationResponseDto> getReservationStatus(@PathVariable("id") Long reservationId,
                                                                       @AuthenticationPrincipal UserDetails userDetails) {
        ReservationResponseDto response = seatReservationService.getReservationDetails(reservationId, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
