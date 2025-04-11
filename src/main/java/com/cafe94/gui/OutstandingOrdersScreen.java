package com.cafe94.gui;

import com.cafe94.domain.Delivery;
import com.cafe94.domain.EatIn;
import com.cafe94.domain.Item;
import com.cafe94.domain.Order;
import com.cafe94.domain.Takeaway;
import com.cafe94.domain.User;
import com.cafe94.enums.OrderStatus;
import com.cafe94.enums.UserRole;
import com.cafe94.services.IOrderService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OutstandingOrdersScreen implements Main.NeedsMainApp {

    @FXML private TableView<Order> ordersTable;
    @FXML private Button readyButton;
    @FXML private Label titleLabel;

    private final ObservableList<Order> outstandingOrders =
        FXCollections.observableArrayList();

    private IOrderService orderService;
    private User currentUser;
    private Main mainApp;

    @Override public void setMainApp(Main mainApp) { this.mainApp = mainApp; }
    public void setOrderService(IOrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
        
        if (this.currentUser != null &&
            this.currentUser.getRole() != UserRole.CHEF) {
             System.err.println("WARN: Non-Chef accessing Chef screen: " +
                                this.currentUser.getEmail());
        }
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(orderService, "OrderService is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing OutstandingOrdersScreen for " +
                           currentUser.getEmail());

        setupTableColumns();
        ordersTable.setItems(outstandingOrders);
        ordersTable.setPlaceholder(new Label("Loading orders..."));

        loadOutstandingOrders();

        if (titleLabel != null) {
            titleLabel.setText("Outstanding Food Orders");
        }
    }

    private void setupTableColumns() {
        TableColumn<Order, Integer> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderID"));
        idCol.setPrefWidth(80);

        TableColumn<Order, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> {
            Order order = cellData.getValue();
            String type = "Unknown";
            if (order instanceof EatIn) type = "Eat-In";
            else if (order instanceof Takeaway) type = "Takeaway";
            else if (order instanceof Delivery) type = "Delivery";
            return new SimpleStringProperty(type);
        });
        typeCol.setPrefWidth(80);

        TableColumn<Order, String> itemsCol = new TableColumn<>("Items");
        itemsCol.setCellValueFactory(cellData -> {
            List<Item> items = cellData.getValue().getItems();
            String summary = items.stream()
                              .map(Item::getName)
                              .collect(Collectors.joining(", "));
            return new SimpleStringProperty(summary);
        });
        itemsCol.setPrefWidth(300);

        TableColumn<Order, OrderStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        ordersTable.getColumns().setAll(idCol, typeCol, itemsCol, statusCol);
        idCol.setSortType(TableColumn.SortType.ASCENDING);
        ordersTable.getSortOrder().add(idCol);
    }

    private void loadOutstandingOrders() {
        if (orderService == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Order service not available.");
            ordersTable.setPlaceholder(new Label("Error loading orders."));
            return;
        }
        try {
            List<Order> orders = orderService.getOutstandingOrders();
            outstandingOrders.setAll(orders);
            ordersTable.setPlaceholder(new Label("No outstanding orders."));
            System.out.println("Loaded " + orders.size() + " orders.");
            ordersTable.sort();
        } catch (Exception e) {
            System.err.println("Error loading outstanding orders: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load outstanding orders.");
            ordersTable.setPlaceholder(new Label("Error loading orders."));
        }
    }

    @FXML
    private void markOrderReady() {
        Order selected = ordersTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select an order from the table.");
            return;
        }

        if (currentUser == null || orderService == null) {
            showAlert(Alert.AlertType.ERROR, "Internal Error",
                      "Context or service not available.");
            return;
        }

        if (selected.getStatus() != OrderStatus.PREPARING) {
            showAlert(Alert.AlertType.WARNING, "Incorrect Status",
                "Order must be 'PREPARING' to be marked ready.\nCurrent: " +
                selected.getStatus());
            return;
        }

        int orderId = selected.getOrderID();
        System.out.println("Attempting mark ready for order: " + orderId);

        try {
            Order updated = orderService.markOrderReady(orderId, currentUser);

            if (updated != null) {
                showAlert(Alert.AlertType.INFORMATION, "Order Ready",
                          "Order " + orderId + " marked as ready.");
                loadOutstandingOrders();
            } else {
                 showAlert(Alert.AlertType.ERROR, "Update Failed",
                           "Could not mark order " + orderId + " as ready.");
                 loadOutstandingOrders();
            }
        } catch (Exception e) {
             System.err.println("Error marking order ready " + orderId + ": " + e);
             e.printStackTrace();
             showAlert(Alert.AlertType.ERROR, "Update Error",
                       "Could not mark order ready:\n" + e.getMessage());
             loadOutstandingOrders();
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