package com.cafe94.gui;

import com.cafe94.domain.Item;
import com.cafe94.domain.Takeaway;
import com.cafe94.domain.User;
import com.cafe94.services.IMenuService;
import com.cafe94.services.IOrderService;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TakeawayOrderScreen implements Main.NeedsMainApp {
    public static class OrderItem {
        private final ObjectProperty<Item> domainItem;
        private final IntegerProperty quantity;
        private static final NumberFormat CURRENCY_FORMATTER =
            NumberFormat.getCurrencyInstance(Locale.UK);

        public OrderItem(Item item, int qty) {
            this.domainItem = new SimpleObjectProperty<>(
                Objects.requireNonNull(item));
            this.quantity = new SimpleIntegerProperty(qty);
        }
        public Item getDomainItem() { return domainItem.get(); }
        public String getName() { return getDomainItem().getName(); }
        public BigDecimal getPrice() {
            return BigDecimal.valueOf(getDomainItem().getPrice());
        }
        public String getFormattedPrice() {
            return CURRENCY_FORMATTER.format(getPrice());
        }
        public int getQuantity() { return quantity.get(); }
        public void setQuantity(int qty) { this.quantity.set(qty); }
        public BigDecimal getTotalPrice() {
            return getPrice().multiply(BigDecimal.valueOf(getQuantity()));
        }
        public String getFormattedTotalPrice() {
            return CURRENCY_FORMATTER.format(getTotalPrice());
        }
        @Override public String toString() {
            return getQuantity() + " x " + getName() + " (" +
                   getFormattedTotalPrice() + ")";
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderItem oi = (OrderItem) o;
            return getDomainItem().getItemID() ==
                   oi.getDomainItem().getItemID();
        }
        @Override public int hashCode() {
            return Objects.hash(getDomainItem().getItemID());
        }
    }

   
    @FXML private TableView<Item> menuTable;
    @FXML private ListView<OrderItem> orderSummaryList;
    @FXML private TextField pickupTimeField;
    @FXML private Label totalOrderPriceLabel;
    @FXML private Label specialDetailsLabel;
    @FXML private VBox specialDisplayBox;
    @FXML private Button placeOrderButton;


    private final ObservableList<Item> availableMenuItems =
        FXCollections.observableArrayList();
    private final ObservableList<OrderItem> currentOrderItems =
        FXCollections.observableArrayList();


    private IMenuService menuService;
    private IOrderService orderService;
    private User currentUser;
    private Main mainApp;


    private static final NumberFormat CURRENCY_FORMATTER =
        NumberFormat.getCurrencyInstance(Locale.UK);
    private static final DateTimeFormatter TIME_PARSER =
        DateTimeFormatter.ofPattern("HH:mm");

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setMenuService(IMenuService menuService) {
        this.menuService = Objects.requireNonNull(menuService);
    }
    public void setOrderService(IOrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(menuService, "MenuService is null");
        Objects.requireNonNull(orderService, "OrderService is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing TakeawayOrderScreen for " +
                           currentUser.getEmail());

        configureSpecialDisplayBox();
        menuTable.setItems(availableMenuItems);
        menuTable.setPlaceholder(new Label("Menu loading..."));

        orderSummaryList.setItems(currentOrderItems);
        totalOrderPriceLabel.setText("Total: £0.00");
        pickupTimeField.setPromptText("e.g., 18:30");

        loadMenuItems();
        loadAndDisplaySpecial();
        setupOrderTotalListener();
    }

    private void configureSpecialDisplayBox() {
        if (specialDisplayBox != null) {
            specialDisplayBox.setVisible(false);
            specialDisplayBox.setManaged(false);
        }
        if (specialDetailsLabel != null) {
            specialDetailsLabel.setWrapText(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void setupMenuTable() {
        TableColumn<Item, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(120);
        TableColumn<Item, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);
        TableColumn<Item, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(80);
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        priceCol.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Double p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty||p==null ? null : CURRENCY_FORMATTER.format(p));
            }});
        TableColumn<Item, Void> addCol = new TableColumn<>("Action");
        addCol.setPrefWidth(70);
        addCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Add");
            { btn.setOnAction(e -> {
                 Item item = getTableView().getItems().get(getIndex());
                 addItemToOrder(item);
              });
            }
            @Override public void updateItem(Void i, boolean empty) {
                super.updateItem(i, empty);
                setGraphic(empty ? null : btn);
            }
        });
        menuTable.getColumns().setAll(nameCol, descCol, priceCol, addCol);
    }

    private void loadMenuItems() {
        if (menuService == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Menu service unavailable.");
            return;
        }
        try {
            List<Item> items = menuService.getAllItems();
            availableMenuItems.setAll(items);
            menuTable.setPlaceholder(new Label("Menu is empty."));
            System.out.println("Loaded " + items.size() + " menu items.");
        } catch (Exception e) {
            handleLoadingError("menu items", e);
        }
    }

    private void loadAndDisplaySpecial() {
        if (menuService == null) {
            displaySpecialError("Menu service unavailable.");
            return;
        }
        try {
            List<Item> specials = menuService.getDailySpecials();
            if (specials.isEmpty()) {
                displaySpecialText("No special today!");
                System.out.println("No daily specials found.");
            } else {
                String text = specials.stream()
                    .map(item -> String.format("%s (%s)", item.getName(),
                             CURRENCY_FORMATTER.format(item.getPrice())))
                    .collect(Collectors.joining("\n"));
                displaySpecialText(text);
                System.out.println("Displayed " + specials.size() +
                " specials.");
            }
        } catch (Exception e) {
            handleLoadingError("daily special(s)", e);
            displaySpecialError("Error loading special.");
        }
    }

    private void displaySpecialText(String text) {
        if (specialDetailsLabel != null && specialDisplayBox != null) {
            specialDetailsLabel.setText(text);
            specialDisplayBox.setVisible(true);
            specialDisplayBox.setManaged(true);
        }
    }

    private void displaySpecialError(String errorText) {
         if (specialDetailsLabel != null && specialDisplayBox != null) {
             specialDetailsLabel.setText("Error: " + errorText);
             specialDisplayBox.setVisible(true);
             specialDisplayBox.setManaged(true);
         }
    }

    private void addItemToOrder(Item itemToAdd) {
        OrderItem checkItem = new OrderItem(itemToAdd, 1);
        Optional<OrderItem> existing = currentOrderItems.stream()
            .filter(oi -> oi.equals(checkItem)).findFirst();
        if (existing.isPresent()) {
            OrderItem oi = existing.get();
            oi.setQuantity(oi.getQuantity() + 1);
            orderSummaryList.refresh();
        } else {
            currentOrderItems.add(new OrderItem(itemToAdd, 1));
        }
        System.out.println("Added to order: " + itemToAdd.getName());
    }

    private void setupOrderTotalListener() {
         currentOrderItems.addListener(
             (ListChangeListener.Change<? extends OrderItem> c) -> {
                 updateTotalPrice();
             });
    }

    private void updateTotalPrice() {
         BigDecimal total = BigDecimal.ZERO;
         for (OrderItem item : currentOrderItems) {
             total = total.add(item.getTotalPrice());
         }
         totalOrderPriceLabel.setText(
             "Total: " + CURRENCY_FORMATTER.format(total));
    }

    @FXML
    private void handlePlaceOrder() {
        if (currentUser == null || orderService == null) {
             showAlert(Alert.AlertType.ERROR, "Internal Error",
                       "Context or service not available.");
             return;
        }
        if (currentOrderItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Order",
                      "Please add items to your order.");
            return;
        }
        LocalTime pickupTime;
        try {
            pickupTime = LocalTime.parse(pickupTimeField.getText(),
            TIME_PARSER);
            if (pickupTime.isBefore(LocalTime.now().plusMinutes(5))) {
                showAlert(Alert.AlertType.WARNING, "Invalid Time",
                          "Pickup must be at least 5 mins in the future.");
                return;
            }
        } catch (DateTimeParseException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid Time",
                      "Enter time in HH:MM format (e.g., 18:30).");
            return;
        }
        List<Item> itemsToOrder = currentOrderItems.stream()
            .flatMap(oi -> {
                List<Item> rpt = new ArrayList<>();
                for (int i = 0; i < oi.getQuantity(); i++) {
                    rpt.add(oi.getDomainItem());
                }
                return rpt.stream();
            }).collect(Collectors.toList());
        int customerId = currentUser.getUserID();
        try {
            System.out.println("Placing takeaway order via service...");
            Takeaway order = orderService.placeTakeawayOrder(
                itemsToOrder, customerId, pickupTime);
            if (order != null) {
                showAlert(Alert.AlertType.INFORMATION, "Order Placed",
                    "Takeaway order (ID: " + order.getOrderID() +
                    ") placed for pick-up at " +
                    pickupTime.format(TIME_PARSER) + ".");
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Order Failed",
                          "Could not place takeaway order (service error).");
            }
        } catch (Exception ex) {
            handleLoadingError("place takeaway order", ex);
        }
    }

    private void clearForm() {
        currentOrderItems.clear();
        pickupTimeField.clear();
    }

    private void handleLoadingError(String action, Exception e) {
        System.err.println("Error during " + action + ": " + e.getMessage());
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Error",
                  "Failed to " + action + ".\nPlease check connection.");
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