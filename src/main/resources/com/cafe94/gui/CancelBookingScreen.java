package com.cafe94.gui;

import com.cafe94.domain.Booking;
import com.cafe94.domain.User;
import com.cafe94.enums.BookingStatus;
import com.cafe94.services.IBookingService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CancelBookingScreen implements Main.NeedsMainApp {

    @FXML private TableView<Booking> bookingsTable;
    @FXML private Button cancelButton;
    @FXML private Label titleLabel;

    private final ObservableList<Booking> userBookings =
        FXCollections.observableArrayList();
    private IBookingService bookingService;
    private User currentUser;
    private Main mainApp;

    private static final Set<BookingStatus> CANCELLABLE_STATUSES =
        EnumSet.of(BookingStatus.PENDING_APPROVAL,
                   BookingStatus.CONFIRMED);

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm");

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

        System.out.println("Initializing CancelBookingScreen for user: " +
                           currentUser.getEmail());

        setupTableColumns();
        bookingsTable.setItems(userBookings);
        bookingsTable.setPlaceholder(
            new Label("No upcoming bookings found to cancel.")
        );

        loadUserBookings();

        if (cancelButton != null) {
            cancelButton.setOnAction(e -> handleCancelBooking());
        }
        if (titleLabel != null) {
            titleLabel.setText("Your Upcoming Bookings (" +
                               currentUser.getFirstName() + ")");
        }
    }

    private void setupTableColumns() {
        TableColumn<Booking, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookingID"));
        idCol.setPrefWidth(80);

        TableColumn<Booking, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        dateCol.setPrefWidth(110);
        dateCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDate i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : i.format(DATE_FORMATTER));
            }
        });

        TableColumn<Booking, LocalTime> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("bookingTime"));
        timeCol.setPrefWidth(80);
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalTime i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : i.format(TIME_FORMATTER));
            }
        });

        TableColumn<Booking, Integer> guestsCol = new TableColumn<>("Guests");
        guestsCol.setCellValueFactory(
            new PropertyValueFactory<>("numberOfGuests"));
        guestsCol.setPrefWidth(60);

        TableColumn<Booking, BookingStatus> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        bookingsTable.getColumns().setAll(
            idCol, dateCol, timeCol, guestsCol, statusCol);

        dateCol.setSortType(TableColumn.SortType.ASCENDING);
        bookingsTable.getSortOrder().add(dateCol);
    }

    private void loadUserBookings() {
        try {
            List<Booking> all = bookingService.getCustomerBookings(
                currentUser.getUserID());

            List<Booking> cancellable = all.stream()
                .filter(b -> CANCELLABLE_STATUSES.contains(b.getStatus()))
                .collect(Collectors.toList());

            userBookings.setAll(cancellable);
            System.out.println("Loaded " + cancellable.size() +
                               " cancellable bookings for user " +
                               currentUser.getUserID());
            bookingsTable.sort();

        } catch (NoSuchElementException e) {
             showAlert(Alert.AlertType.ERROR, "Error",
                       "Could not find customer data.");
        } catch (Exception e) {
            System.err.println("Error loading user bookings: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load your bookings.");
        }
    }

    @FXML
    private void handleCancelBooking() {
        Booking selected = bookingsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select a booking from the table to cancel.");
            return;
        }

        Alert confirmAlert = createConfirmationAlert(selected);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            int bookingId = selected.getBookingID();
            System.out.println("Confirmed - attempting cancel booking ID: " +
                               bookingId);
            try {
                boolean success = bookingService.cancelBooking(
                    bookingId, currentUser);

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                              "Booking " + bookingId + " cancelled.");
                    userBookings.remove(selected);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed",
                              "Could not cancel booking " + bookingId +
                              " (service returned false).");
                     loadUserBookings();
                }
            } catch (Exception e) {
                 System.err.println("Error cancel booking " + bookingId +
                                    ": " + e);
                 e.printStackTrace();
                 showAlert(Alert.AlertType.ERROR, "Error",
                           "Could not cancel booking:\n" + e.getMessage());
                 loadUserBookings();
            }
        } else {
            System.out.println("User cancelled action for booking: " +
                               selected.getBookingID());
        }
    }

    private Alert createConfirmationAlert(Booking booking) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Cancellation");
        alert.setHeaderText("Cancel Booking ID: " + booking.getBookingID());
        String content = String.format(
            "Cancel booking for %d guests on %s at %s?",
            booking.getNumberOfGuests(),
            booking.getBookingDate().format(DATE_FORMATTER),
            booking.getBookingTime().format(TIME_FORMATTER)
        );
        alert.setContentText(content);
        return alert;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label contentLabel = new Label(msg);
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(400);
        alert.getDialogPane().setContent(contentLabel);
        alert.showAndWait();
    }
}