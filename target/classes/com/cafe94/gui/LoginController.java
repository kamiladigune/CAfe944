package com.cafe94.gui;

import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;
import com.cafe94.persistence.*; // Keep persistence if needed directly
import com.cafe94.services.*;
import com.cafe94.util.SessionManager;
import com.cafe94.util.ValidationUtils;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException; // For navigation exceptions
import java.util.List;
import java.util.Objects;

/**
 * Controller for the Login screen (LoginScreen.fxml).
 * Handles user authentication and navigation to role dashboards.
 */
// Implement interfaces to receive dependencies via Main.injectDependencies
public class LoginController implements Main.NeedsMainApp, Main.NeedsSessionManager {

    // --- FXML Bindings ---
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    // --- Injected dependencies ---
    private IUserService userService;
    private SessionManager sessionManager;
    private IOrderService orderService; // Keep if dashboard needs it
    private IMenuService menuService; // Keep if dashboard needs it
    private IBookingService bookingService; // Keep if dashboard needs it
    private IReportingService reportingService; // Keep if dashboard needs it
    private IUserRepository userRepository; // Keep if dashboard needs it
    private ITableRepository tableRepository; // Keep if dashboard needs it
    private IBookingRepository bookingRepository; // Keep if dashboard needs it
    private Main mainApp; // Reference to Main for navigation

    // --- Setters for Dependency Injection (called by Main) ---
    @Override public void setMainApp(Main mainApp) { this.mainApp = mainApp; }
    @Override public void setSessionManager(SessionManager m) { this.sessionManager = m; }
    public void setUserService(IUserService s){ this.userService = s; }
    public void setOrderService(IOrderService s){ this.orderService = s; }
    public void setMenuService(IMenuService s){ this.menuService = s; }
    public void setBookingService(IBookingService s){ this.bookingService = s; }
    public void setReportingService(IReportingService s){ this.reportingService = s; }
    public void setUserRepository(IUserRepository r){ this.userRepository = r; }
    public void setTableRepository(ITableRepository r){ this.tableRepository = r; }
    public void setBookingRepository(IBookingRepository r){ this.bookingRepository = r; }

    /** Called by FXMLLoader after FXML load */
    @FXML
    public void initialize() {
        clearError();
        // Add listeners to clear error on input change
        emailField.textProperty().addListener((o,ov,nv) -> clearError());
        passwordField.textProperty().addListener((o,ov,nv)->clearError());
    }

    /** Handles the login button click */
    @FXML
    private void handleLoginButtonAction() {
        clearError();
        // Check required services are injected
        if(userService == null || sessionManager == null || mainApp == null){
            showError("Internal Error: Services missing.");
            return;
        }
        String email = emailField.getText();
        String password = passwordField.getText();
        try {
            // Basic validation
            ValidationUtils.requireNonBlank(email,"Email");
            ValidationUtils.requireNonBlank(password,"Password");
        } catch (IllegalArgumentException e){
            showError(e.getMessage()); // Show validation errors
            return;
        }
        try {
            // Attempt login via service
            List<User> profiles = userService.login(email, password);

            if (profiles == null || profiles.isEmpty()){
                showError("Login failed. Check credentials.");
            } else if (profiles.size() == 1) {
                // Single profile found, log in directly
                User user = profiles.get(0);
                sessionManager.setCurrentUser(user); // Set user in session
                System.out.println("Login OK: User=" + user.getEmail() +
                                   " Role=" + user.getRole());
                navigateToDashboard(user); // Navigate using Main app
            } else {
                // Multiple profiles found (e.g., Staff+Customer)
                System.out.println("Multiple profiles found for email.");
                // Navigate to profile selection screen
                navigateToProfileSelection(profiles);
            }
        } catch (Exception e){
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
            showError("Unexpected login error occurred.");
        }
    }

    /**
     * Navigates to the appropriate dashboard based on user role.
     * Uses the Main application's navigation methods.
     * @param user The authenticated user.
     */
    private void navigateToDashboard(User user) {
        Stage currentStage = (Stage) loginButton.getScene().getWindow();
        String screenTitle = "Cafe94";
        String fxmlPath = null; // FXML path for the role's dashboard
        UserRole role = user.getRole();

        // Determine FXML path and title based on role
        switch (role) {
            case MANAGER:
                screenTitle += " - Manager Dashboard";
                // Example: Load StaffManagementScreen or a dashboard
                fxmlPath = "/com/cafe94/gui/StaffManagementScreen.fxml";
                break;
            case CHEF:
                screenTitle += " - Chef Dashboard";
                fxmlPath = "/com/cafe94/gui/OutstandingOrdersScreen.fxml";
                break;
            case WAITER:
                screenTitle += " - Waiter Dashboard";
                fxmlPath = "/com/cafe94/gui/BookingApproverScreen.fxml";
                break;
            case DRIVER:
                screenTitle += " - Driver Dashboard";
                fxmlPath = "/com/cafe94/gui/DriverDeliveriesScreen.fxml";
                break;
            case CUSTOMER: // Customer logged in via Staff Login? Handle this case
                screenTitle += " - Customer Portal";
                 // Might need a dedicated Customer Dashboard FXML
                fxmlPath = "/com/cafe94/gui/BookingRequestScreen.fxml";
                break;
            default:
                showError("Cannot navigate: Unknown role " + role);
                return;
        }

        try {
            // Use mainApp to load the scene into the primary stage
            mainApp.loadScene(fxmlPath);
            currentStage.setTitle(screenTitle); // Update window title
        } catch (Exception ex) {
             handleNavigationError(role, ex); // Use existing error handler
        }
    }

    /**
     * Navigates to the Profile Selection screen using Main app.
     * @param profiles The list of authenticated profiles.
     */
    private void navigateToProfileSelection(List<User> profiles) {
        Stage currentStage = (Stage) loginButton.getScene().getWindow();
        System.out.println("Navigating to Profile Selection screen.");

        try {
            // Load FXML via Main, get controller, pass profiles
            String fxmlPath = "/com/cafe94/gui/ProfileSelectionScreen.fxml";
            FXMLLoader loader = mainApp.createLoader(fxmlPath);
            Parent root = loader.load();
            Object controller = loader.getController();

            // Inject standard dependencies (like mainApp, sessionMgr)
            mainApp.injectDependencies(controller);

            // Pass the profiles list to the specific controller
            if (controller instanceof ProfileSelectionScreen) {
                ((ProfileSelectionScreen) controller).setProfiles(profiles);
            } else {
                 throw new IllegalStateException("Wrong controller type!");
            }

            // Set the new root in the primary scene
            currentStage.getScene().setRoot(root);
            currentStage.setTitle("Cafe94 - Select Profile");

        } catch (Exception ex) {
             System.err.println("Err launch Profile Select: " + ex);
             showError("Could not open profile selection window.");
        }
    }

    // --- Helper Methods (Mostly Unchanged) ---
    private void showError(String msg){
        if(errorLabel != null){
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        } else {
            // Fallback if label is somehow null
            showAlert(Alert.AlertType.ERROR,"Login Error", msg);
        }
    }
    private void clearError(){
        if(errorLabel != null){
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }
    private void handleNavigationError(UserRole role, Exception ex){
        System.err.println("Err launch screen "+role+": "+ex.getMessage());
        ex.printStackTrace(); // Print stack trace for debug
        showAlert(Alert.AlertType.ERROR, "Navigation Error",
                  "Failed open window for role " + role + ".");
    }
    private void showAlert(Alert.AlertType t, String title, String msg){
        Alert a=new Alert(t); a.setTitle(title);
        a.setHeaderText(null);
        Label c=new Label(msg); c.setWrapText(true); c.setMaxWidth(350);
        a.getDialogPane().setContent(c); a.showAndWait();
    }

    // REMOVED: isAnyServiceNull() check (DI handles this now)
}