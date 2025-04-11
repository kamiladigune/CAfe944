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
import java.util.List;
import java.util.Objects;

public class BookingApproverScreen implements Main.NeedsMainApp {

    @FXML private TableView<Booking> requestTable;
    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    @FXML private Label titleLabel;

    private final ObservableList<Booking> pendingRequests =
        FXCollections.observableArrayList();

    private IBookingService bookingService;
    private User currentUser;
    private Main mainApp;

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm");

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
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

        System.out.println("Initializing BookingApproverScreen for " +
                           currentUser.getEmail());

        setupTableColumns();
        requestTable.setItems(pendingRequests);
        requestTable.setPlaceholder(
            new Label("No pending booking requests.")
        );

        loadPendingBookings();

        if (titleLabel != null) {
             titleLabel.setText("Pending Booking Requests");
        }
    }

    private void setupTableColumns() {
        TableColumn<Booking, Integer> idCol = new TableColumn<>("Booking ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookingID"));
        idCol.setPrefWidth(80);

        TableColumn<Booking, Integer> custCol = new TableColumn<>("Cust ID");
        custCol.setCellValueFactory(new PropertyValueFactory<>("customerID"));
        custCol.setPrefWidth(100);

        TableColumn<Booking, Integer> guestsCol = new TableColumn<>("Guests");
        guestsCol.setCellValueFactory(
            new PropertyValueFactory<>("numberOfGuests"));
        guestsCol.setPrefWidth(60);

        TableColumn<Booking, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        dateCol.setPrefWidth(100);
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

        TableColumn<Booking, BookingStatus> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        requestTable.getColumns().setAll(
            idCol, custCol, guestsCol, dateCol, timeCol, statusCol);
        requestTable.getSortOrder().add(dateCol);
        requestTable.getSortOrder().add(timeCol);
    }

    private void loadPendingBookings() {
        if (bookingService == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Booking service not available.");
            return;
        }
        try {
            List<Booking> pending = bookingService.getBookingByStatus(
                BookingStatus.PENDING_APPROVAL);
            pendingRequests.setAll(pending);
            System.out.println("Loaded " + pending.size() +
                               " pending booking requests.");
            requestTable.sort();
        } catch (Exception e) {
            System.err.println("Error loading pending bookings: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load pending booking requests.");
        }
    }

    @FXML
    private void handleApproveAction() {
        handleRequestAction(true);
    }

    @FXML
    private void handleRejectAction() {
        handleRequestAction(false);
    }

    private void handleRequestAction(boolean approve) {
        Booking selected = requestTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select a booking request.");
            return;
        }

        if (currentUser == null || bookingService == null) {
             showAlert(Alert.AlertType.ERROR, "Internal Error",
                       "Context or service not available.");
             return;
        }

        if (selected.getStatus() != BookingStatus.PENDING_APPROVAL) {
            showAlert(Alert.AlertType.INFORMATION, "Already Processed",
                "Request no longer pending (Status: " +
                selected.getStatus() + ").");
            loadPendingBookings();
            return;
        }

        int bookingId = selected.getBookingID();
        String action = approve ? "approve" : "reject";
        String pastTense = approve ? "approved" : "rejected";

        try {
            boolean success;
            if (approve) {
                System.out.println("Approving booking ID: " + bookingId);
                success = bookingService.approveBooking(bookingId, currentUser);
                if (!success) {
                     showAlert(Alert.AlertType.WARNING, "Approval Failed",
                         "Could not approve booking " + bookingId +
                         ". No suitable table available?");
                    loadPendingBookings();
                    return;
                }
            } else {
                System.out.println("Rejecting booking ID: " + bookingId);
                success = bookingService.rejectBooking(bookingId,
                                                       currentUser, null);
            }

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Booking " + pastTense,
                    "Booking " + bookingId + " successfully " + pastTense + ".");
                pendingRequests.remove(selected);
            } else {
                showAlert(Alert.AlertType.ERROR, "Action Failed",
                    "Could not " + action + " booking " + bookingId + ".");
                loadPendingBookings();
            }
        } catch (Exception e) {
             System.err.println("Error trying to " + action + " booking " +
                                bookingId + ": " + e);
             e.printStackTrace();
             showAlert(Alert.AlertType.ERROR, "Error",
                       "Failed to " + action + " booking:\n" + e.getMessage());
             loadPendingBookings();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
         Alert alert = new Alert(type);
         alert.setTitle(title);
         alert.setHeaderText(null);
         Label content = new Label(msg);
         content.setWrapText(true);
         content.setMaxWidth(400);
         alert.getDialogPane().setContent(content);
         alert.showAndWait();
    }
}