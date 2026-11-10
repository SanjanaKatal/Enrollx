package com.enrollx.dto;

import jakarta.validation.constraints.*;

public class CourseOfferingDto {

    private Long id;

    @NotNull(message = "Please select a course")
    private Long courseId;

    @NotBlank(message = "Semester is required (e.g. Fall 2026)")
    private String semester;

    @NotBlank(message = "Academic year is required (e.g. 2026-2027)")
    private String academicYear;

    @NotNull(message = "Seat capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 500, message = "Capacity cannot exceed 500")
    private Integer capacity;

    @NotBlank(message = "Instructor name is required")
    private String instructor;

    private String room;
    private String schedule;
    private boolean active = true;

    public CourseOfferingDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
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
}
