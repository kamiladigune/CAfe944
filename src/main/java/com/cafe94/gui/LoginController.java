package com.cafe94.gui;

import java.io.IOException;

import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;
import com.cafe94.persistence.*;
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

import java.util.List;

/**
 * Controller for the Login screen
 */

public class LoginController implements Main.NeedsMainApp,
Main.NeedsSessionManager {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;


    private IUserService userService;
    private SessionManager sessionManager;
    private IOrderService orderService;
    private IMenuService menuService;
    private IBookingService bookingService;
    private IReportingService reportingService;
    private IUserRepository userRepository;
    private ITableRepository tableRepository;
    private IBookingRepository bookingRepository;
    private Main mainApp;

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    @Override public void setSessionManager(SessionManager m) {
        this.sessionManager = m;
    }
    public void setUserService(IUserService s){
        this.userService = s;
    }
    public void setOrderService(IOrderService s){
        this.orderService = s;
    }
    public void setMenuService(IMenuService s){
        this.menuService = s;
    }
    public void setBookingService(IBookingService s){
        this.bookingService = s;
    }
    public void setReportingService(IReportingService s){
        this.reportingService = s;
    }
    public void setUserRepository(IUserRepository r){
        this.userRepository = r;
    }
    public void setTableRepository(ITableRepository r){
        this.tableRepository = r;
    }
    public void setBookingRepository(IBookingRepository r){
        this.bookingRepository = r;
    }

    @FXML
    public void initialize() {
        clearError();
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
            showError(e.getMessage());
            return;
        }
        try {
            List<User> profiles = userService.login(email, password);

            if (profiles == null || profiles.isEmpty()){
                showError("Login failed. Check credentials.");
            } else if (profiles.size() == 1) {
                User user = profiles.get(0);
                sessionManager.setCurrentUser(user);
                System.out.println("Login OK: User=" + user.getEmail() +
                                   " Role=" + user.getRole());
                navigateToDashboard(user);
            } else {
                System.out.println("Multiple profiles found for email.");
                navigateToProfileSelection(profiles);
            }
        } catch (Exception e){
            System.err.println("Login error: " + e.getMessage());
            e.printStackTrace();
            showError("Unexpected login error occurred.");
        }
    }

    /**
     * Navigates to the appropriate dashboard based on user role
     * @param user The authenticated user.
     */
    private void navigateToDashboard(User user) {
        Stage currentStage = (Stage) loginButton.getScene().getWindow();
        String screenTitle = "Cafe94";
        String fxmlPath = null;
        UserRole role = user.getRole();

        // Determine FXML path and title based on role
        switch (role) {
            case MANAGER:
                screenTitle += " - Manager Dashboard";
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
            case CUSTOMER:
                screenTitle += " - Customer Portal";
                fxmlPath = "/com/cafe94/gui/BookingRequestScreen.fxml";
                break;
            default:
                showError("Cannot navigate: Unknown role " + role);
                return;
        }

        try {
            mainApp.loadScene(fxmlPath);
            currentStage.setTitle(screenTitle);
        } catch (Exception ex) {
             handleNavigationError(role, ex);
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
            String fxmlPath = "/com/cafe94/gui/ProfileSelectionScreen.fxml";
            FXMLLoader loader = mainApp.createLoader(fxmlPath);
            Parent root = loader.load();
            Object controller = loader.getController();

           
            mainApp.injectDependencies(controller);

            if (controller instanceof ProfileSelectionScreen) {
                ((ProfileSelectionScreen) controller).setProfiles(profiles);
            } else {
                 throw new IllegalStateException("Wrong controller type!");
            }

            
            currentStage.getScene().setRoot(root);
            currentStage.setTitle("Cafe94 - Select Profile");

        } catch (IOException | IllegalStateException ex) {
             System.err.println("Err launch Profile Select: " + ex);
             showError("Could not open profile selection window.");
        }
    }

    private void showError(String msg){
        if(errorLabel != null){
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        } else {
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
        ex.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "Navigation Error",
                  "Failed open window for role " + role + ".");
    }
    private void showAlert(Alert.AlertType t, String title, String msg){
        Alert a=new Alert(t); a.setTitle(title);
        a.setHeaderText(null);
        Label c=new Label(msg); c.setWrapText(true); c.setMaxWidth(350);
        a.getDialogPane().setContent(c); a.showAndWait();
    }

}