package com.cafe94.gui;

import com.cafe94.domain.Delivery;
import com.cafe94.domain.Driver;
import com.cafe94.domain.Item;
import com.cafe94.domain.Order;
import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.enums.OrderStatus;
import com.cafe94.enums.UserRole;
import com.cafe94.persistence.IUserRepository;
import com.cafe94.services.IOrderService;
import com.cafe94.services.IUserService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class DeliveryApproverScreen implements Main.NeedsMainApp {

    @FXML private TableView<Delivery> requestTable;
    @FXML private ComboBox<Driver> driverComboBox;
    @FXML private Button approveButton;
    @FXML private Label titleLabel;

    private final ObservableList<Delivery> pendingDeliveryRequests =
        FXCollections.observableArrayList();
    private final ObservableList<Driver> availableDrivers =
        FXCollections.observableArrayList();
    private IOrderService orderService;
    private IUserService userService;
    private IUserRepository userRepository;
    private User currentUser;
    private Main mainApp;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER =
        NumberFormat.getCurrencyInstance(Locale.UK);

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setOrderService(IOrderService s) {
        this.orderService = s;
    }
    public void setUserService(IUserService s) {
        this.userService = s;
    }
    public void setUserRepository(IUserRepository r){
        this.userRepository=r;
    }
    public void setCurrentUser(User u) {
        this.currentUser = u;
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(orderService, "OrderService is null");
        Objects.requireNonNull(userRepository, "UserRepo is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing DeliveryApproverScreen for " +
                           currentUser.getEmail());

        setupTableColumns();
        requestTable.setItems(pendingDeliveryRequests);
        requestTable.setPlaceholder(
            new Label("No pending delivery requests found.")
        );

        driverComboBox.setItems(availableDrivers);
        driverComboBox.setPromptText("Select Driver...");
        configureDriverComboBoxConverter();

        loadPendingDeliveries();
        loadAvailableDrivers();

        if (approveButton != null) {
            approveButton.setOnAction(e -> handleApproval());
        }
        if (titleLabel != null) {
             titleLabel.setText("Pending Delivery Order Requests");
        }
    }

    private void configureDriverComboBoxConverter() {
        driverComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Driver driver) {
                return driver == null ? null :
                       String.format("%s %s (ID:%d)",
                           driver.getFirstName(),
                           driver.getLastName(),
                           driver.getUserID());
            }
            @Override
            public Driver fromString(String string) {
                return null;
            }
        });
    }

    private void setupTableColumns() {
        TableColumn<Delivery, Integer> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderID"));
        idCol.setPrefWidth(80);

        TableColumn<Delivery, Integer> custCol = new TableColumn<>("Cust ID");
        custCol.setCellValueFactory(new PropertyValueFactory<>("customerID"));
        custCol.setPrefWidth(80);

        TableColumn<Delivery, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(
            new PropertyValueFactory<>("deliveryAddress"));
        addrCol.setPrefWidth(200);

        TableColumn<Delivery, String> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(cellData -> {
            List<Item> items = cellData.getValue().getItems();
            String summary = items.stream()
                              .map(Item::getName)
                              .collect(Collectors.joining(", "));
            return new javafx.beans.property.SimpleStringProperty(summary);
        });
        itemsCol.setPrefWidth(180);

        TableColumn<Delivery, LocalDateTime> timeCol =
            new TableColumn<>("Requested");
        timeCol.setCellValueFactory(
            new PropertyValueFactory<>("orderTimestamp"));
        timeCol.setPrefWidth(130);
        timeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(LocalDateTime i, boolean e) {
                super.updateItem(i, e);
                setText(e || i == null ? null : i.format(DATE_TIME_FORMATTER));
            }
        });

        TableColumn<Delivery, BigDecimal> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setPrefWidth(90);
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        totalCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null :
                        CURRENCY_FORMATTER.format(i));
            }
        });

        TableColumn<Delivery, OrderStatus> statusCol =
            new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        requestTable.getColumns().setAll(
            idCol, timeCol, custCol, addrCol, itemsCol, totalCol, statusCol);

        timeCol.setSortType(TableColumn.SortType.ASCENDING);
        requestTable.getSortOrder().add(timeCol);
    }

    private void loadPendingDeliveries() {
        try {
            List<Order> orders = orderService.getOrdersByStatus(
                OrderStatus.CONFIRMED);

            List<Delivery> deliveries = orders.stream()
                .filter(Delivery.class::isInstance)
                .map(Delivery.class::cast)
                .collect(Collectors.toList());

            pendingDeliveryRequests.setAll(deliveries);
            System.out.println("Loaded " + deliveries.size() +
                               " pending delivery requests.");
            requestTable.sort();

        } catch (Exception e) {
            System.err.println("Error loading pending deliveries: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load pending delivery requests.");
        }
    }

    private void loadAvailableDrivers() {
        try {
            List<Staff> staff = userRepository.findStaffByRole(
                UserRole.DRIVER);

            List<Driver> drivers = staff.stream()
                .filter(Driver.class::isInstance)
                .map(Driver.class::cast)
                .collect(Collectors.toList());

            availableDrivers.setAll(drivers);
            System.out.println("Loaded " + drivers.size() +
                               " available drivers.");
        } catch (Exception e) {
            System.err.println("Error loading available drivers: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load available drivers list.");
        }
    }

    @FXML
    private void handleApproval() {
        Delivery selectedOrder =
            requestTable.getSelectionModel().getSelectedItem();
        Driver selectedDriver = driverComboBox.getValue();

        if (selectedOrder == null) {
            showAlert(Alert.AlertType.WARNING, "No Order Selected",
                      "Please select an order request from the table.");
            return;
        }
        if (selectedDriver == null) {
            showAlert(Alert.AlertType.WARNING, "No Driver Selected",
                      "Please select a driver from the dropdown list.");
            return;
        }
         if (orderService == null || currentUser == null) {
             showAlert(Alert.AlertType.ERROR, "Internal Error",
                       "Required services not available.");
             return;
         }

        if (selectedOrder.getStatus() != OrderStatus.CONFIRMED ) {
            showAlert(Alert.AlertType.INFORMATION, "Order Status Changed",
                "Order " + selectedOrder.getOrderID() +
                " is no longer awaiting driver assignment (Status: " +
                selectedOrder.getStatus() + ").");
            loadPendingDeliveries();
            return;
        }

        int orderId = selectedOrder.getOrderID();
        int driverId = selectedDriver.getUserID();

        try {
            System.out.println("Assigning driver " + driverId +
                               " to order " + orderId +
                               " by user " + currentUser.getUserID());

            boolean success = orderService.assignDriverToOrder(
                orderId, driverId, currentUser);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Order " + orderId + " assigned to driver " +
                    selectedDriver.getFirstName() + ".");
                pendingDeliveryRequests.remove(selectedOrder);
                driverComboBox.getSelectionModel().clearSelection();
            } else {
                showAlert(Alert.AlertType.ERROR, "Assignment Failed",
                    "Could not assign driver (service returned false).");
                 loadPendingDeliveries();
            }

        } catch (RuntimeException ex) {
            System.err.println("Error assigning driver to order " +
                               orderId + ": " + ex);
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Assignment failed:\n" + ex.getMessage());
            loadPendingDeliveries();
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