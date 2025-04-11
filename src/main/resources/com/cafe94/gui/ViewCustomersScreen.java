package com.cafe94.gui;

import com.cafe94.domain.Customer;
import com.cafe94.domain.User;
import com.cafe94.enums.UserRole; // Added for role check
import com.cafe94.persistence.IUserRepository;
import com.cafe94.services.IOrderService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException; // Added for navigation exception
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ViewCustomersScreen implements Main.NeedsMainApp {

    @FXML private TableView<Customer> customerTable;
    @FXML private Button viewHistoryButton;
    @FXML private Label titleLabel;

    private final ObservableList<Customer> customerList =
        FXCollections.observableArrayList();

    private IUserRepository userRepository;
    private IOrderService orderService; // Needed to pass to History screen
    private User currentUser; // Manager viewing this screen
    private Main mainApp;

    @Override public void setMainApp(Main mainApp) { this.mainApp = mainApp; }
    public void setUserRepository(IUserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }
    public void setOrderService(IOrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }
    // Add setter for current user if needed for authorization checks
    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        if (this.currentUser != null &&
            this.currentUser.getRole() != UserRole.MANAGER) {
            System.err.println("WARN: Non-Manager accessing ViewCustomers: " +
                               this.currentUser.getEmail());
            // Disable button?
            if (viewHistoryButton != null) viewHistoryButton.setDisable(true);
        }
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(userRepository, "UserRepo is null");
        Objects.requireNonNull(orderService, "OrderService is null");
        // CurrentUser might be null if not passed, handle defensively

        System.out.println("Initializing ViewCustomersScreen...");

        setupTableColumns();
        customerTable.setItems(customerList);
        customerTable.setPlaceholder(new Label("Loading customers..."));

        loadCustomers();

        if (titleLabel != null) {
            titleLabel.setText("Customer List");
        }
        // Button action linked via FXML onAction="#handleViewHistory"
    }

    private void setupTableColumns() {
        TableColumn<Customer, Integer> idCol = new TableColumn<>("Cust ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("userID"));
        idCol.setPrefWidth(100);

        TableColumn<Customer, String> fNameCol = new TableColumn<>("First");
        fNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        fNameCol.setPrefWidth(120);

        TableColumn<Customer, String> lNameCol = new TableColumn<>("Last");
        lNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lNameCol.setPrefWidth(120);

        TableColumn<Customer, String> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(new PropertyValueFactory<>("address"));
        addrCol.setPrefWidth(250);

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        phoneCol.setPrefWidth(120);

        customerTable.getColumns().setAll(
            idCol, fNameCol, lNameCol, addrCol, phoneCol);
    }

    private void loadCustomers() {
        if (userRepository == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "User repo unavailable.");
            customerTable.setPlaceholder(new Label("Error loading data."));
            return;
        }
        try {
            List<User> allUsers = userRepository.findAll();
            List<Customer> customers = allUsers.stream()
                .filter(Customer.class::isInstance)
                .map(Customer.class::cast)
                .collect(Collectors.toList());

            customerList.setAll(customers);
            customerTable.setPlaceholder(new Label("No customers found."));
            System.out.println("Loaded " + customers.size() + " customers.");

        } catch (Exception e) {
            System.err.println("Error loading customers: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load customer data.");
            customerTable.setPlaceholder(new Label("Error loading data."));
        }
    }

    @FXML
    private void handleViewHistory() {
        Customer selected = customerTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select a customer from the table.");
            return;
        }
        if (orderService == null || mainApp == null) {
             showAlert(Alert.AlertType.ERROR, "Internal Error",
                       "Required service or context missing.");
            return;
        }

        System.out.println("Viewing history for customer: " + selected.getUserID());

        try {
            // Use mainApp to open OrderHistoryScreen in a new window
            String fxmlPath = "/com/cafe94/gui/OrderHistoryScreen.fxml";
            String title = "Order History - " + selected.getFirstName();

            // Open as a modal window (blocks interaction with main window)
            // Pass false for modal if non-blocking needed
            Object controller = mainApp.openWindow(fxmlPath, title, true);

            // Pass the selected customer to the history controller
            // Requires OrderHistoryScreen to have a setCurrentCustomer method
            if (controller instanceof OrderHistoryScreen) {
                // IMPORTANT: Modify OrderHistoryScreen to accept the *selected*
                // customer instead of relying only on the logged-in user.
                // Or adjust Main.injectDependencies for this specific case.
                 ((OrderHistoryScreen) controller).setCurrentUser(selected);
                 // Ensure OrderHistoryScreen re-loads data if needed after
                 // setCurrentUser is called post-initialize.
                 ((OrderHistoryScreen) controller).loadOrderHistory();
            } else {
                System.err.println("Loaded controller is not OrderHistoryScreen!");
            }

        } catch (IOException ex) {
            System.err.println("Error launching Order History: " + ex);
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                      "Could not open order history window.");
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