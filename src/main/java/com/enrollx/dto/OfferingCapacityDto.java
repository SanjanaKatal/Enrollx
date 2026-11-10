package com.enrollx.dto;

public class OfferingCapacityDto {

    private Long offeringId;
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private String departmentCode;
    private String departmentName;
    private int credits;
    private String semester;
    private String academicYear;
    private String instructor;
    private String room;
    private String schedule;
    private int capacity;
    private long enrolledCount;
    private long reservedCount;
    private int availableSeats;
    private boolean alreadyEnrolled;
    private boolean hasActiveReservation;
    private Long activeReservationId;

    public OfferingCapacityDto() {
    }

    public int getFilledSeats() {
        return (int) (enrolledCount + reservedCount);
    }

    public int getPercentFilled() {
        if (capacity <= 0) return 100;
        int pct = (int) Math.round(((double) getFilledSeats() / capacity) * 100.0);
        return Math.min(pct, 100);
    }

    public boolean isFull() {
        return availableSeats <= 0;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
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

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public long getEnrolledCount() {
        return enrolledCount;
    }

    public void setEnrolledCount(long enrolledCount) {
        this.enrolledCount = enrolledCount;
    }

    public long getReservedCount() {
        return reservedCount;
    }

    public void setReservedCount(long reservedCount) {
        this.reservedCount = reservedCount;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public boolean isAlreadyEnrolled() {
        return alreadyEnrolled;
    }

    public void setAlreadyEnrolled(boolean alreadyEnrolled) {
        this.alreadyEnrolled = alreadyEnrolled;
    }

    public boolean isHasActiveReservation() {
        return hasActiveReservation;
    }

    public void setHasActiveReservation(boolean hasActiveReservation) {
        this.hasActiveReservation = hasActiveReservation;
    }

    public Long getActiveReservationId() {
        return activeReservationId;
    }

    public void setActiveReservationId(Long activeReservationId) {
        this.activeReservationId = activeReservationId;
    }
}
