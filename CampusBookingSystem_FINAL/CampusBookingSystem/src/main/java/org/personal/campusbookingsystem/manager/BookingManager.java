package org.personal.campusbookingsystem.manager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.personal.campusbookingsystem.exception.InvalidBookingDurationException;
import org.personal.campusbookingsystem.exception.ResourceUnavailableException;
import org.personal.campusbookingsystem.exception.UnauthorizedAccessException;
import org.personal.campusbookingsystem.model.Booking;
import org.personal.campusbookingsystem.model.Resource;
import org.personal.campusbookingsystem.model.User;
import org.personal.campusbookingsystem.util.CsvUtil;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all bookings in memory as an {@link ObservableList} and keeps
 * them in sync with data/bookings.csv (via {@link CsvUtil}). Also
 * contains the booking validation rules: no double-booking, no invalid
 * durations, and role checks for approving or cancelling a booking.
 */
public class BookingManager {

    private static final String DATA_DIR = "data";
    private static final String FILE_NAME = "bookings.csv";
    private static final int MAX_DURATION_HOURS = 3;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ObservableList<Booking> bookings = FXCollections.observableArrayList();
    private final ResourceManager resourceManager;
    private int nextIdNumber = 1;

    public BookingManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        loadBookings();
    }

    private void loadBookings() {
        bookings.clear();
        List<String[]> rows = CsvUtil.readRows(Path.of(DATA_DIR, FILE_NAME));
        for (int i = 0; i < rows.size(); i++) {
            String[] p = rows.get(i);
            if (i == 0 && p.length > 0 && "bookingId".equalsIgnoreCase(p[0])) {
                continue; // header row
            }
            if (p.length < 9) {
                continue;
            }
            Booking b = new Booking(
                    p[0], p[1], p[2], p[3],
                    LocalDate.parse(p[4], DATE_FMT),
                    LocalTime.parse(p[5], TIME_FMT),
                    LocalTime.parse(p[6], TIME_FMT),
                    p[7], p[8]);
            bookings.add(b);

            int number = parseIdNumber(b.getBookingId());
            if (number >= nextIdNumber) {
                nextIdNumber = number + 1;
            }
        }
    }

    private int parseIdNumber(String bookingId) {
        try {
            return Integer.parseInt(bookingId.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void saveBookings() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"bookingId", "userId", "creatorName", "resourceId",
                "bookingDate", "startTime", "endTime", "purpose", "status"});
        for (Booking b : bookings) {
            rows.add(new String[]{
                    b.getBookingId(), b.getUserId(), b.getCreatorName(), b.getResourceId(),
                    b.getBookingDate().format(DATE_FMT),
                    b.getStartTime().format(TIME_FMT),
                    b.getEndTime().format(TIME_FMT),
                    b.getPurpose() == null ? "" : b.getPurpose(),
                    b.getStatus()
            });
        }
        CsvUtil.writeRows(Path.of(DATA_DIR, FILE_NAME), rows);
    }

    public Booking createBooking(Booking booking, User actingUser)
            throws InvalidBookingDurationException, ResourceUnavailableException {

        // 1) Duration validation
        if (!booking.getEndTime().isAfter(booking.getStartTime())) {
            throw new InvalidBookingDurationException("End time must be after start time.");
        }
        Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
        if (duration.toMinutes() > MAX_DURATION_HOURS * 60) {
            throw new InvalidBookingDurationException("Booking duration cannot exceed " + MAX_DURATION_HOURS + " hours.");
        }

        // 2) Resource availability
        Resource resource = resourceManager.findById(booking.getResourceId());
        if (resource == null || !resource.isAvailable()) {
            throw new ResourceUnavailableException("This resource is not available right now.");
        }

        // 3) Double-booking / time-slot conflict check
        for (Booking existing : bookings) {
            boolean sameResource = existing.getResourceId().equals(booking.getResourceId());
            boolean sameDate = existing.getBookingDate().equals(booking.getBookingDate());
            boolean active = !existing.getStatus().equalsIgnoreCase("CANCELLED")
                    && !existing.getStatus().equalsIgnoreCase("REJECTED");
            boolean overlaps = booking.getStartTime().isBefore(existing.getEndTime())
                    && existing.getStartTime().isBefore(booking.getEndTime());

            if (sameResource && sameDate && active && overlaps) {
                throw new ResourceUnavailableException("This resource is already booked for the selected time slot.");
            }
        }

        // 4) Assign ID + initial status - students need Staff/Admin approval, Staff/Admin are auto-confirmed
        booking.setBookingId("BK" + String.format("%04d", nextIdNumber++));

        if (actingUser != null && actingUser.isStudent()) {
            booking.setStatus("PENDING");
        } else {
            booking.setStatus("CONFIRMED");
            resource.setStatus("OCCUPIED");
            resourceManager.touch(resource); // publish the in-place status change to any bound UI
            resourceManager.saveResources();
        }

        bookings.add(booking); // ObservableList add -> bound UI (tables, FilteredLists, size() bindings) updates automatically
        saveBookings();
        return booking;
    }

    /** The live, observable backing list - safe to bind directly to a TableView or wrap in a FilteredList. */
    public ObservableList<Booking> getAllBookings() {
        return bookings;
    }

    public List<Booking> getBookingsForUser(String userId) {
        List<Booking> mine = new ArrayList<>();
        for (Booking b : bookings) {
            if (b.getUserId().equals(userId)) {
                mine.add(b);
            }
        }
        return mine;
    }

    public Booking findBookingById(String bookingId) {
        for (Booking b : bookings) {
            if (b.getBookingId().equalsIgnoreCase(bookingId)) {
                return b;
            }
        }
        return null;
    }

    public void updateBookingStatus(String bookingId, String newStatus, User actingUser) throws UnauthorizedAccessException {
        if (actingUser == null || actingUser.isStudent()) {
            throw new UnauthorizedAccessException("Only Staff or Admin can approve/reject bookings.");
        }
        Booking booking = findBookingById(bookingId);
        if (booking == null) {
            return;
        }
        booking.setStatus(newStatus);
        touch(booking); // publish the in-place status change to any bound UI (e.g. the pending-bookings table)

        Resource resource = resourceManager.findById(booking.getResourceId());
        if (resource != null) {
            if ("CONFIRMED".equalsIgnoreCase(newStatus)) {
                resource.setStatus("OCCUPIED");
            } else if ("REJECTED".equalsIgnoreCase(newStatus)) {
                resource.setStatus("AVAILABLE");
            }
            resourceManager.touch(resource);
            resourceManager.saveResources();
        }

        saveBookings();
    }

    public void cancelBooking(String bookingId, User actingUser) throws UnauthorizedAccessException {
        Booking booking = findBookingById(bookingId);
        if (booking == null) {
            return;
        }

        boolean isOwner = actingUser != null && booking.getUserId().equals(actingUser.getUserId());
        boolean isAdmin = actingUser != null && actingUser.isAdmin();

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedAccessException("You do not have permission to cancel this booking.");
        }

        booking.setStatus("CANCELLED");
        touch(booking); // publish the in-place status change to any bound UI

        Resource resource = resourceManager.findById(booking.getResourceId());
        if (resource != null) {
            resource.setStatus("AVAILABLE");
            resourceManager.touch(resource);
            resourceManager.saveResources();
        }

        saveBookings();
    }

    /**
     * Re-publishes an already-mutated Booking into the ObservableList at its
     * current index, so bound TableViews/FilteredLists/Bindings notice the
     * change even though only a field (status) was mutated in place.
     */
    private void touch(Booking booking) {
        int index = bookings.indexOf(booking);
        if (index >= 0) {
            bookings.set(index, booking);
        }
    }
}
