package com.enrollx.dto;

public class SeatStatusDto {

    private Long offeringId;
    private int capacity;
    private long enrolledCount;
    private long reservedCount;
    private int availableSeats;
    private boolean isFull;

    public SeatStatusDto() {
    }

    public SeatStatusDto(Long offeringId, int capacity, long enrolledCount, long reservedCount, int availableSeats) {
        this.offeringId = offeringId;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
        this.reservedCount = reservedCount;
        this.availableSeats = availableSeats;
        this.isFull = availableSeats <= 0;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public void setOfferingId(Long offeringId) {
        this.offeringId = offeringId;
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

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }
}
