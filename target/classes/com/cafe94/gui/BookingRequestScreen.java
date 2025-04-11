package com.cafe94.gui;

import com.cafe94.domain.Booking;
import com.cafe94.domain.User;
import com.cafe94.services.IBookingService;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class BookingRequestScreen implements Main.NeedsMainApp {

    @FXML private Spinner<Integer> guestsSpinner;
    @FXML private DatePicker datePicker;
    @FXML private TextField timeField;
    @FXML private Spinner<Integer> durationSpinner;
    @FXML private Button submitButton;
    @FXML private Label sceneTitle;

    private IBookingService bookingService;
    private User currentUser;
    private Main mainApp;

    // --- Formatters ---
    private static final DateTimeFormatter TIME_PARSER =
        DateTimeFormatter.ofPattern("HH:mm");
    // ** ADDED MISSING FORMATTER DEFINITION **
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ISO_DATE; // Standard YYYY-MM-DD format

    @Override public void setMainApp(Main mainApp) { this.mainApp = mainApp; }
    public void setBookingService(IBookingService bookingService) {
        this.bookingService = Objects.requireNonNull(bookingService);
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(bookingService, "BookingService is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing BookingRequestScreen for " +
                           currentUser.getEmail());

        datePicker.setValue(LocalDate.now());
        timeField.setPromptText("e.g., 19:30");
        // Initial spinner values often set in FXML

        if (sceneTitle != null) {
             sceneTitle.setText("Request a Table Booking");
        }
    }

    @FXML
    private void handleSubmitRequest() {
        if (currentUser == null || bookingService == null) {
            showAlert(Alert.AlertType.ERROR, "Internal Error",
                      "Context or service not available.");
            return;
        }

        LocalDate date = datePicker.getValue();
        if (date == null || date.isBefore(LocalDate.now())) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                      "Please select a valid future date.");
            return;
        }

        LocalTime time;
        try {
            time = LocalTime.parse(timeField.getText(), TIME_PARSER);
        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                      "Invalid time format. Use HH:MM (e.g., 19:00).");
            return;
        }

        LocalDateTime bookingDateTime = LocalDateTime.of(date, time);
        if (bookingDateTime.isBefore(LocalDateTime.now().plusMinutes(5))) {
             showAlert(Alert.AlertType.WARNING, "Validation Error",
                       "Please book at least 5 minutes in the future.");
             return;
        }

        int guests = guestsSpinner.getValue();
        int durationHours = durationSpinner.getValue();
        Duration duration = Duration.ofHours(durationHours);
        int customerId = currentUser.getUserID();

        try {
            System.out.println("Submitting booking request to service...");
            Booking newBooking = bookingService.requestBooking(
                customerId, guests, date, time, duration, null);

            if (newBooking != null) {
                showAlert(Alert.AlertType.INFORMATION,
                    "Booking Request Submitted",
                    "Request (ID: " + newBooking.getBookingID() + ") for " +
                    guests + " guests on " + date.format(DATE_FORMATTER) + // Uses DATE_FORMATTER
                    " at " + time.format(TIME_PARSER) +
                    " sent for approval.");
                clearForm();
            } else {
                 showAlert(Alert.AlertType.ERROR, "Submission Failed",
                           "Booking request could not be submitted.");
            }
        } catch (Exception e) {
             System.err.println("Error submitting booking request: " + e);
             e.printStackTrace();
             showAlert(Alert.AlertType.ERROR, "Submission Error",
                       "Could not submit request:\n" + e.getMessage());
        }
    }

    private void clearForm() {
        guestsSpinner.getValueFactory().setValue(2);
        datePicker.setValue(LocalDate.now());
        timeField.setText("19:00"); // Reset default if desired
        durationSpinner.getValueFactory().setValue(1);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label content = new Label(msg);
        content.setWrapText(true);
        content.setMaxWidth(350);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
}