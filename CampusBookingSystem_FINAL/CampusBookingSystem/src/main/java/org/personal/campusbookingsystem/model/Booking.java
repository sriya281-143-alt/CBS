package org.personal.campusbookingsystem.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A single flat class for a booking. Only the IDs of the user and
 * resource are stored (not object references), which keeps the class
 * simple and maps directly onto one row of bookings.csv.
 */
public class Booking {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private String bookingId;
    private String userId;
    private String creatorName;
    private String resourceId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private String status; // "PENDING", "CONFIRMED", "REJECTED", "CANCELLED"

    public Booking() {
    }

    public Booking(String bookingId, String userId, String creatorName, String resourceId,
                    LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                    String purpose, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.creatorName = creatorName;
        this.resourceId = resourceId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimeRangeText() {
        return startTime.format(TIME_FMT) + " - " + endTime.format(TIME_FMT);
    }

    @Override
    public String toString() {
        return bookingId + " - " + resourceId + " - " + bookingDate + " (" + status + ")";
    }
}
