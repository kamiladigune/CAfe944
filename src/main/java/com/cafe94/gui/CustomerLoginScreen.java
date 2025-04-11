package com.cafe94.gui;

import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;
import com.cafe94.services.IUserService;
import com.cafe94.util.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.util.List;
import java.util.Objects;

public class CustomerLoginScreen implements Main.NeedsMainApp,
Main.NeedsSessionManager {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private IUserService userService;
    private SessionManager sessionManager;
    private Main mainApp;

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    @Override public void setSessionManager(SessionManager s) {
        this.sessionManager = s;
    }
    public void setUserService(IUserService userService) {
        this.userService = Objects.requireNonNull(userService);
    }

    @FXML
    public void initialize() {
        clearError();
        emailField.textProperty().addListener((o,ov,nv) -> clearError());
        passwordField.textProperty().addListener((o,ov,nv) -> clearError());
        System.out.println("CustomerLoginScreen Initialized");
    }

    @FXML
    private void handleLoginAction() {
        if (userService == null || sessionManager == null || mainApp == null) {
            showError("Internal error. Services not ready.");
            return;
        }
        clearError();
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email == null || email.isBlank() || password == null ||
        password.isBlank()) {
            showError("Email and password are required.");
            return;
        }

        try {
            List<User> profiles = userService.login(email, password);

            User customerProfile = profiles.stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .findFirst()
                .orElse(null);

            if (customerProfile == null) {
                 showError("Login failed. Check credentials or role.");
                 return;
            }

            // Login successful for customer
            sessionManager.setCurrentUser(customerProfile);
            System.out.println("Customer Login OK: " +
            customerProfile.getEmail());

            mainApp.loadScene("/com/cafe94/gui/TakeawayOrderScreen.fxml");
            Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
            stage.setTitle("Cafe94 - Customer Portal");


        } catch (Exception e) {
            System.err.println("Customer login error: " + e);
            e.printStackTrace();
            showError("An unexpected error occurred during login.");
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Error", message);
        }
    }

    private void clearError() {
         if (errorLabel != null) {
             errorLabel.setText("");
             errorLabel.setVisible(false);
         }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}