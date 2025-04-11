package com.cafe94.gui;

import com.cafe94.domain.Item;
import com.cafe94.domain.Table;
import com.cafe94.domain.User;
import com.cafe94.domain.EatIn;
import com.cafe94.persistence.ITableRepository;
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
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class EatInOrderScreen implements Main.NeedsMainApp {


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
        public Item getDomainItem() {
            return domainItem.get();
        }
        public String getName() {
            return getDomainItem().getName();
        }
        public BigDecimal getPrice() {
            return BigDecimal.valueOf(getDomainItem().getPrice());
        }
        public String getFormattedPrice() {
            return CURRENCY_FORMATTER.format(getPrice());
        }
        public int getQuantity() {
            return quantity.get();
        }
        public void setQuantity(int qty) {
            this.quantity.set(qty);
        }
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

    @FXML private ComboBox<Table> tableComboBox;
    @FXML private TableView<Item> menuTable;
    @FXML private ListView<OrderItem> currentOrderList;
    @FXML private Label totalOrderPriceLabel;
    @FXML private Button submitOrderButton;
    @FXML private Label menuTitleLabel;
    @FXML private Label orderTitleLabel;

    
    private final ObservableList<Item> availableMenuItems =
        FXCollections.observableArrayList();
    private final ObservableList<OrderItem> currentOrderItems =
        FXCollections.observableArrayList();
    private final ObservableList<Table> availableTables =
        FXCollections.observableArrayList();


    private IMenuService menuService;
    private IOrderService orderService;
    private ITableRepository tableRepository;
    private User currentUser;
    private Main mainApp;
    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setMenuService(IMenuService s) {
        this.menuService = s;
    }
    public void setOrderService(IOrderService s) {
        this.orderService = s;
    }
    public void setTableRepository(ITableRepository r) {
        this.tableRepository = r;
    }
    public void setCurrentUser(User u) {
        this.currentUser = u;
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(menuService, "MenuService is null");
        Objects.requireNonNull(orderService, "OrderService is null");
        Objects.requireNonNull(tableRepository, "TableRepository is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing EatInOrderScreen for " +
                           currentUser.getEmail());

        configureTableComboBox();
        setupMenuTable();
        menuTable.setItems(availableMenuItems);
        menuTable.setPlaceholder(new Label("Menu loading..."));

        currentOrderList.setItems(currentOrderItems);
        totalOrderPriceLabel.setText("Total: £0.00");

        loadAvailableTables();
        loadMenuItems();
        setupOrderTotalListener();

        tableComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                clearCurrentOrder();
                System.out.println("Selected table T" + newVal.getTableNumber());
            } else if (newVal == null) {
                clearCurrentOrder();
            }
        });
    }

    private void configureTableComboBox() {
        tableComboBox.setItems(availableTables);
        tableComboBox.setPromptText("Choose table...");
        tableComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Table table) {
                return table == null ? null :
                       String.format("T%d (%d Seats, %s)",
                           table.getTableNumber(), table.getCapacity(),
                           table.getStatus());
            }
            @Override public Table fromString(String string) {
                return null;
            }
        });
    }

    private void loadAvailableTables() {
        try {
            List<Table> tables = tableRepository.findAll();
            availableTables.setAll(tables);
             System.out.println("Loaded " + tables.size() + " tables.");
        } catch (Exception e) {
             System.err.println("Error loading tables: " + e);
             showAlert(Alert.AlertType.ERROR, "Error", "Failed load tables.");
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
            final NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.UK);
            @Override protected void updateItem(Double p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : fmt.format(p));
            }});

        TableColumn<Item, Void> addCol = new TableColumn<>("Action");
        addCol.setPrefWidth(70);
        addCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Add");
            { btn.setOnAction(e -> {
                 if (tableComboBox.getValue() == null) {
                     showAlert(Alert.AlertType.WARNING, "No Table",
                               "Select table before adding items.");
                     return;
                 }
                 Item item = getTableView().getItems().get(getIndex());
                 addItemToOrder(item);
              });
            }
            @Override public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        menuTable.getColumns().setAll(nameCol, descCol, priceCol, addCol);
    }

    private void loadMenuItems() {
        try {
            availableMenuItems.setAll(menuService.getAllItems());
            menuTable.setPlaceholder(new Label("Menu is empty."));
            System.out.println("Loaded " + availableMenuItems.size() +
            " items.");
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error",
            "Failed load menu.");
            menuTable.setPlaceholder(new Label("Error loading menu."));
        }
    }

    private void addItemToOrder(Item itemToAdd) {
        OrderItem checkItem = new OrderItem(itemToAdd, 1);
        Optional<OrderItem> existing = currentOrderItems.stream()
            .filter(oi -> oi.equals(checkItem)).findFirst();

        if (existing.isPresent()) {
            OrderItem oi = existing.get();
            oi.setQuantity(oi.getQuantity() + 1);
            currentOrderList.refresh();
        } else {
            currentOrderItems.add(new OrderItem(itemToAdd, 1));
        }
        System.out.println("Added " + itemToAdd.getName() + " to order.");
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
             "Total: " + NumberFormat.getCurrencyInstance(Locale.UK)
                                     .format(total));
    }

    private void clearCurrentOrder() {
        currentOrderItems.clear();
    }

    @FXML
    private void handleSubmitOrder() {
        Table selectedTable = tableComboBox.getValue();
        if (selectedTable == null) {
            showAlert(Alert.AlertType.WARNING, "No Table Selected",
                      "Please select the table for this order.");
            return;
        }
        if (currentOrderItems.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Order",
                      "Please add items to the order.");
            return;
        }
        if (orderService == null || currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Internal Error",
                      "Service or user context missing.");
            return;
        }

        List<Item> itemsToOrder = currentOrderItems.stream()
            .flatMap(oi -> {
                List<Item> repeated = new ArrayList<>();
                for(int i=0; i<oi.getQuantity(); i++) {
                    repeated.add(oi.getDomainItem());
                }
                return repeated.stream();
            }).collect(Collectors.toList());

        int tableNumber = selectedTable.getTableNumber();
        int waiterId = currentUser.getUserID();

        try {
            System.out.println("Submitting eat-in order via service...");
            EatIn order = orderService.createEatInOrder(
                itemsToOrder, waiterId, tableNumber, null);

            if (order != null) {
                 showAlert(Alert.AlertType.INFORMATION, "Order Submitted",
                           "Order (ID: " + order.getOrderID() +
                           ") for table T" + tableNumber +
                           " sent to kitchen.");
                 clearForm();
            } else {
                 showAlert(Alert.AlertType.ERROR, "Submission Failed",
                           "Could not submit order (service error).");
            }
        } catch (Exception e) {
             System.err.println("Error submitting eat-in order: " + e);
             e.printStackTrace();
             showAlert(Alert.AlertType.ERROR, "Submission Error",
                       "Could not submit order:\n" + e.getMessage());
        }
    }

    private void clearForm() {
        clearCurrentOrder();
        tableComboBox.getSelectionModel().clearSelection();
        tableComboBox.setPromptText("Choose table...");
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