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
import java.util.NoSuchElementException;
import java.util.Objects;

public class BookingHistoryScreen implements Main.NeedsMainApp {

    @FXML private TableView<Booking> historyTable;
    @FXML private Label titleLabel;

    private final ObservableList<Booking> bookingHistory =
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

        System.out.println("Initializing BookingHistoryScreen for user: " +
                           currentUser.getEmail());

        setupTableColumns();
        historyTable.setItems(bookingHistory);
        historyTable.setPlaceholder(
            new Label("Loading booking history...")
        );

        loadBookingHistory();

        if (titleLabel != null) {
             titleLabel.setText("Your Booking History (" +
                                currentUser.getFirstName() + ")");
        }
    }

    private void setupTableColumns() {
        TableColumn<Booking, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookingID"));
        idCol.setPrefWidth(100);

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

        TableColumn<Booking, Integer> tableCol = new TableColumn<>("Table");
        tableCol.setCellValueFactory(new PropertyValueFactory<>("tableNumber"));
        tableCol.setPrefWidth(70);
         tableCol.setCellFactory(col -> new TableCell<>() {
             @Override protected void updateItem(Integer i, boolean empty) {
                 super.updateItem(i, empty);
                 setText(empty || i == null || i <= 0 ? "N/A" : "T" + i);
             }
         });

        TableColumn<Booking, BookingStatus> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(150);

        historyTable.getColumns().setAll(
            idCol, dateCol, timeCol, guestsCol, tableCol, statusCol);

        dateCol.setSortType(TableColumn.SortType.DESCENDING);
        historyTable.getSortOrder().add(dateCol);
    }

    private void loadBookingHistory() {
        if (bookingService == null || currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Required context or service not available.");
            historyTable.setPlaceholder(
                new Label("Error loading booking history.")
            );
            return;
        }

        try {
            int customerId = currentUser.getUserID();
            System.out.println("Loading booking history for customer ID: " +
                               customerId);
            List<Booking> history =
                bookingService.getCustomerBookings(customerId);

            bookingHistory.setAll(history);
            historyTable.setPlaceholder(
                new Label("You have no past bookings.")
            );
            historyTable.sort();
            System.out.println("Loaded " + history.size() +
                               " bookings into history view.");

        } catch (NoSuchElementException e) {
             showAlert(Alert.AlertType.ERROR, "Error",
                       "Could not find customer data for history.");
             historyTable.setPlaceholder(new Label("Error loading history."));
        } catch (Exception e) {
            System.err.println("Error loading booking history: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load your booking history.");
            historyTable.setPlaceholder(new Label("Error loading history."));
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