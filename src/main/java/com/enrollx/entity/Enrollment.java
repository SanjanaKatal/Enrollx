package com.enrollx.entity;

import com.enrollx.enums.EnrollmentStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_offering_user", columnNames = {"offering_id", "user_id"})
}, indexes = {
    @Index(name = "idx_enrollments_offering", columnList = "offering_id"),
    @Index(name = "idx_enrollments_user", columnList = "user_id")
})
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @Column(name = "dropped_at")
    private Instant droppedAt;

    public Enrollment() {
    }

    public Enrollment(CourseOffering courseOffering, User user) {
        this.courseOffering = courseOffering;
        this.user = user;
        this.enrolledAt = Instant.now();
        this.status = EnrollmentStatus.ENROLLED;
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

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public Instant getDroppedAt() {
        return droppedAt;
    }

    public void setDroppedAt(Instant droppedAt) {
        this.droppedAt = droppedAt;
    }
}
