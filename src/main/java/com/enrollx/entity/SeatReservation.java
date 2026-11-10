package com.enrollx.entity;

import com.enrollx.enums.ReservationStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "seat_reservations", indexes = {
    @Index(name = "idx_res_offering_status_expires", columnList = "offering_id, status, expires_at"),
    @Index(name = "idx_res_user_status", columnList = "user_id, status")
})
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public SeatReservation() {
    }

    public SeatReservation(CourseOffering courseOffering, User user, Instant expiresAt) {
        this.courseOffering = courseOffering;
        this.user = user;
        this.reservedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.status = ReservationStatus.PENDING;
        this.version = 0L;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CourseOffering getCourseOffering() {
        return courseOffering;
    }

    public void setCourseOffering(CourseOffering courseOffering) {
        this.courseOffering = courseOffering;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(Instant reservedAt) {
        this.reservedAt = reservedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
