package com.enrollx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_offerings", uniqueConstraints = {
    @UniqueConstraint(name = "uq_course_offering", columnNames = {"course_id", "semester", "academic_year"})
})
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 20)
    private String semester;

    @Column(name = "academic_year", nullable = false, length = 10)
    private String academicYear;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false, length = 100)
    private String instructor;

    @Column(length = 50)
    private String room;

    @Column(length = 100)
    private String schedule;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public CourseOffering() {
    }

    public CourseOffering(Long id, Course course, String semester, String academicYear, int capacity, String instructor, String room, String schedule) {
        this.id = id;
        this.course = course;
        this.semester = semester;
        this.academicYear = academicYear;
        this.capacity = capacity;
        this.instructor = instructor;
        this.room = room;
        this.schedule = schedule;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
