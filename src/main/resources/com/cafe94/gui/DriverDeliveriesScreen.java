package com.cafe94.gui;

import com.cafe94.domain.Delivery;
import com.cafe94.domain.Item;
import com.cafe94.domain.Order;
import com.cafe94.domain.User;
import com.cafe94.enums.OrderStatus;
import com.cafe94.enums.UserRole; // Needed for role check
import com.cafe94.services.IOrderService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DriverDeliveriesScreen implements Main.NeedsMainApp {

    @FXML private TableView<Delivery> deliveriesTable;
    @FXML private Button deliveredButton;
    @FXML private Label titleLabel;

    private final ObservableList<Delivery> assignedDeliveries =
        FXCollections.observableArrayList();

    private IOrderService orderService;
    private User currentUser;
    private Main mainApp;

    private static final Set<OrderStatus> DRIVER_ACTIVE_STATUSES =
        EnumSet.of(OrderStatus.READY_FOR_DISPATCH,
                   OrderStatus.OUT_FOR_DELIVERY);

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @Override public void setMainApp(Main mainApp) { this.mainApp = mainApp; }
    public void setOrderService(IOrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
        // Optional strict check
        if (this.currentUser.getRole() != UserRole.DRIVER) {
            System.err.println("WARN: Non-driver user accessing screen: " +
                               this.currentUser.getEmail());
            // Potentially disable functionality or show error
        }
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(orderService, "OrderService is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing DriverDeliveriesScreen for " +
                           currentUser.getEmail());

        setupTableColumns();
        deliveriesTable.setItems(assignedDeliveries);
        deliveriesTable.setPlaceholder(
            new Label("No deliveries currently assigned.")
        );

        loadAssignedDeliveries();

        if (titleLabel != null) {
             titleLabel.setText("Your Assigned Deliveries (" +
                                currentUser.getFirstName() + ")");
        }
        // Button action linked via FXML onAction="#handleMarkDelivered"
    }

    private void setupTableColumns() {
        TableColumn<Delivery, Integer> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderID"));
        idCol.setPrefWidth(80);

        TableColumn<Delivery, Integer> custCol = new TableColumn<>("Cust ID");
        custCol.setCellValueFactory(new PropertyValueFactory<>("customerID"));
        custCol.setPrefWidth(100);

        TableColumn<Delivery, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(
            new PropertyValueFactory<>("deliveryAddress"));
        addrCol.setPrefWidth(250);

        TableColumn<Delivery, String> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(cellData -> {
            List<Item> items = cellData.getValue().getItems();
            String summary = items.stream()
                              .map(Item::getName)
                              .collect(Collectors.joining(", "));
            return new javafx.beans.property.SimpleStringProperty(summary);
        });
        itemsCol.setPrefWidth(150);

        TableColumn<Delivery, LocalTime> timeCol = new TableColumn<>("Est.");
        timeCol.setCellValueFactory(
            new PropertyValueFactory<>("estimatedDeliveryTime"));
        timeCol.setPrefWidth(80);
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalTime i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? "N/A" : i.format(TIME_FORMATTER));
            }
        });

        TableColumn<Delivery, OrderStatus> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        deliveriesTable.getColumns().setAll(
            idCol, timeCol, custCol, addrCol, itemsCol, statusCol);

        timeCol.setSortType(TableColumn.SortType.ASCENDING);
        deliveriesTable.getSortOrder().add(timeCol);
    }

    private void loadAssignedDeliveries() {
        if (orderService == null || currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Required service or context not available.");
            return;
        }
        try {
            List<Order> driverOrders =
                orderService.getDriverCurrentOrders(currentUser.getUserID());

            List<Delivery> activeDeliveries = driverOrders.stream()
                .filter(Delivery.class::isInstance)
                .map(Delivery.class::cast)
                .filter(d -> DRIVER_ACTIVE_STATUSES.contains(d.getStatus()))
                .collect(Collectors.toList());

            assignedDeliveries.setAll(activeDeliveries);
            System.out.println("Loaded " + activeDeliveries.size() +
                               " active deliveries for driver " +
                               currentUser.getUserID());
        } catch (NoSuchElementException e) {
             showAlert(Alert.AlertType.ERROR, "Error",
                       "Could not find driver data.");
        } catch (Exception e) {
            System.err.println("Error loading driver deliveries: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load assigned deliveries.");
        }
    }

    @FXML
    private void handleMarkDelivered() {
        Delivery selected = deliveriesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select a delivery to mark as delivered.");
            return;
        }

        if (currentUser == null || orderService == null) {
            showAlert(Alert.AlertType.ERROR, "Internal Error",
                      "Context or service not available.");
            return;
        }

        // Only allow marking if OUT_FOR_DELIVERY
        if (selected.getStatus() != OrderStatus.OUT_FOR_DELIVERY) {
            showAlert(Alert.AlertType.WARNING, "Incorrect Status",
                "Order status must be 'OUT_FOR_DELIVERY'.\nCurrent: " +
                selected.getStatus());
            return;
        }

        Alert confirm = createConfirmationAlert(selected);
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            int orderId = selected.getOrderID();
            System.out.println("Attempting mark delivered: " + orderId);
            try {
                boolean success = orderService.markOrderDelivered(
                    orderId, currentUser);

                if (success) {
                    showAlert(Alert.AlertType.INFORMATION,
                              "Delivery Confirmed",
                              "Order " + orderId + " marked delivered.");
                    assignedDeliveries.remove(selected);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Update Failed",
                        "Could not mark order " + orderId + " delivered.");
                    loadAssignedDeliveries();
                }
            } catch (Exception e) {
                 System.err.println("Error marking order delivered " +
                                    orderId + ": " + e);
                 e.printStackTrace();
                 showAlert(Alert.AlertType.ERROR, "Update Error",
                           "Could not mark delivered:\n" + e.getMessage());
                 loadAssignedDeliveries();
            }
        } else {
            System.out.println("Mark delivered cancelled by user for: " +
                               selected.getOrderID());
        }
    }

    private Alert createConfirmationAlert(Delivery delivery) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delivery");
        alert.setHeaderText("Mark Order Delivered: " + delivery.getOrderID());
        String content = String.format(
            "Confirm delivery of order %d to %s?",
            delivery.getOrderID(), delivery.getDeliveryAddress());
        Label label = new Label(content);
        label.setWrapText(true);
        label.setMaxWidth(400);
        alert.getDialogPane().setContent(label);
        return alert;
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