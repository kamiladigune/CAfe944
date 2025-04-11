package com.cafe94.gui;

import com.cafe94.domain.Delivery;
import com.cafe94.domain.EatIn;
import com.cafe94.domain.Item;
import com.cafe94.domain.Order;
import com.cafe94.domain.Takeaway;
import com.cafe94.domain.User;
import com.cafe94.enums.OrderStatus;
import com.cafe94.services.IOrderService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderHistoryScreen implements Main.NeedsMainApp {

    @FXML private TableView<Order> historyTable;
    @FXML private Label titleLabel;

    private final ObservableList<Order> orderHistory =
        FXCollections.observableArrayList();

    private IOrderService orderService;
    private User currentUser;
    private Main mainApp;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final NumberFormat CURRENCY_FORMATTER =
        NumberFormat.getCurrencyInstance(Locale.UK);

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setOrderService(IOrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(orderService, "OrderService is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing OrderHistoryScreen for " +
                           currentUser.getEmail());

        setupTableColumns();
        historyTable.setItems(orderHistory);
        historyTable.setPlaceholder(new Label("Loading history..."));

        loadOrderHistory();

        if (titleLabel != null) {
             titleLabel.setText("Your Past Orders (" +
                                currentUser.getFirstName() + ")");
        }
    }

    private void setupTableColumns() {
        TableColumn<Order, Integer> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderID"));
        idCol.setPrefWidth(80);

        TableColumn<Order, LocalDateTime> dateTimeCol =
            new TableColumn<>("Date/Time");
        dateTimeCol.setCellValueFactory(
            new PropertyValueFactory<>("orderTimestamp"));
        dateTimeCol.setPrefWidth(130);
        dateTimeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.format(DATE_TIME_FORMATTER));
            }
        });

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
        itemsCol.setPrefWidth(200);

        TableColumn<Order, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setPrefWidth(80);
        totalCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        totalCol.setCellFactory(tc -> new TableCell<>() {
             @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null :
                        CURRENCY_FORMATTER.format(price));
            }
        });

        TableColumn<Order, OrderStatus> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(110);

        historyTable.getColumns().setAll(
            idCol, dateTimeCol, typeCol, itemsCol, totalCol, statusCol);

        dateTimeCol.setSortType(TableColumn.SortType.DESCENDING);
        historyTable.getSortOrder().add(dateTimeCol);
    }

    void loadOrderHistory() {
        if (orderService == null || currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Required service or context not available.");
            historyTable.setPlaceholder(
                new Label("Error loading order history.")
            );
            return;
        }
        try {
            int customerId = currentUser.getUserID();
            System.out.println("Loading order history for customer ID: " +
                               customerId);
            List<Order> history =
                orderService.getCustomerOrderHistory(customerId);

            orderHistory.setAll(history);
            historyTable.setPlaceholder(new Label("You have no past orders."));
            historyTable.sort();
            System.out.println("Loaded " + history.size() + " orders.");

        } catch (NoSuchElementException e) {
             showAlert(Alert.AlertType.ERROR, "Error",
                       "Could not find customer data.");
             historyTable.setPlaceholder(new Label("Error loading history."));
        } catch (Exception e) {
            System.err.println("Error loading order history: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load your order history.");
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