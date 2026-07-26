package org.personal.campusbookingsystem.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.personal.campusbookingsystem.exception.InvalidBookingDurationException;
import org.personal.campusbookingsystem.exception.ResourceUnavailableException;
import org.personal.campusbookingsystem.manager.BookingManager;
import org.personal.campusbookingsystem.manager.ResourceManager;
import org.personal.campusbookingsystem.model.Booking;
import org.personal.campusbookingsystem.model.Resource;
import org.personal.campusbookingsystem.model.User;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Controls the Booking Form dialog. Validates and submits a new booking
 * through BookingManager, showing InvalidBookingDurationException and
 * ResourceUnavailableException messages as a plain inline error label
 * instead of letting the app crash.
 */
public class BookingController {

    @FXML private Label resourceNameLabel;
    @FXML private Label resourceTypeLabel;
    @FXML private Label resourceCapacityLabel;

    @FXML private DatePicker bookingDatePicker;
    @FXML private ComboBox<String> startTimeCombo;
    @FXML private ComboBox<String> endTimeCombo;
    @FXML private TextField purposeField;
    @FXML private TextArea notesArea;
    @FXML private Label bookingErrorLabel;

    private Resource resource;
    private User currentUser;
    private ResourceManager resourceManager;
    private BookingManager bookingManager;
    private Runnable onBookingCompleted;

    /** Called by ResourcesController right after loading this dialog. */
    public void setUp(Resource resource, User currentUser, ResourceManager resourceManager, BookingManager bookingManager) {
        this.resource = resource;
        this.currentUser = currentUser;
        this.resourceManager = resourceManager;
        this.bookingManager = bookingManager;

        resourceNameLabel.setText(resource.getName());
        resourceTypeLabel.setText("Type: " + resource.getResourceType());
        resourceCapacityLabel.setText("Capacity: Up to " + resource.getCapacity() + " people");
    }

    public void setOnBookingCompleted(Runnable callback) {
        this.onBookingCompleted = callback;
    }

    @FXML
    public void initialize() {
        var times = FXCollections.observableArrayList(
                "08:00", "09:00", "10:00", "11:00", "12:00", "13:00",
                "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00");
        startTimeCombo.setItems(times);
        endTimeCombo.setItems(times);
        bookingDatePicker.setValue(LocalDate.now());
    }

    @FXML
    private void handleConfirmBooking(ActionEvent event) {

        if (bookingDatePicker.getValue() == null
                || startTimeCombo.getValue() == null
                || endTimeCombo.getValue() == null
                || purposeField.getText() == null || purposeField.getText().isBlank()) {
            showError("Please fill in date, start/end time and purpose of booking.");
            return;
        }

        if (bookingDatePicker.getValue().isBefore(LocalDate.now())) {
            showError("Booking date cannot be in the past.");
            return;
        }

        LocalTime start = LocalTime.parse(startTimeCombo.getValue());
        LocalTime end = LocalTime.parse(endTimeCombo.getValue());

        String purpose = purposeField.getText();
        if (notesArea.getText() != null && !notesArea.getText().isBlank()) {
            purpose += " | Notes: " + notesArea.getText();
        }

        Booking booking = new Booking();
        booking.setUserId(currentUser.getUserId());
        booking.setCreatorName(currentUser.getName());
        booking.setResourceId(resource.getResourceId());
        booking.setBookingDate(bookingDatePicker.getValue());
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setPurpose(purpose);

        try {
            bookingManager.createBooking(booking, currentUser);

            Alert confirmed = new Alert(Alert.AlertType.INFORMATION,
                    "Booking " + booking.getBookingId() + " " +
                            (booking.getStatus().equalsIgnoreCase("PENDING")
                                    ? "submitted and awaiting approval." : "confirmed!"));
            confirmed.setHeaderText("Success");
            confirmed.showAndWait();

            if (onBookingCompleted != null) {
                onBookingCompleted.run();
            }
            closeDialog();

        } catch (InvalidBookingDurationException | ResourceUnavailableException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeDialog();
    }

    private void showError(String message) {
        bookingErrorLabel.setText(message);
        bookingErrorLabel.setVisible(true);
        bookingErrorLabel.setManaged(true);
    }

    private void closeDialog() {
        Stage stage = (Stage) purposeField.getScene().getWindow();
        stage.close();
    }
}
