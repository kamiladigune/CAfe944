package com.cafe94.gui;

import java.io.IOException;

import com.cafe94.domain.User;
import com.cafe94.services.IUserService;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Objects;

public class CustomerSignUpScreen implements Main.NeedsMainApp {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signUpButton;
    @FXML private Label errorLabel;

    private IUserService userService;
    private Main mainApp;

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setUserService(IUserService userService) {
        this.userService = Objects.requireNonNull(userService);
    }

    @FXML
    public void initialize() {
        errorLabel.setText("");
        System.out.println("CustomerSignUpScreen Initialized");
    }

    @FXML
    private void handleSignUpAction() {
        if (userService == null || mainApp == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Service or navigation context not available.");
            return;
        }

        String fname = firstNameField.getText();
        String lname = lastNameField.getText();
        String email = emailField.getText();
        String pass = passwordField.getText();
        String confirmPass = confirmPasswordField.getText();
        String phone = phoneField.getText();
        String address = addressField.getText();

        if (fname == null || fname.isBlank() || lname == null ||
        lname.isBlank() || email == null || email.isBlank() || pass == null
        || pass.isBlank()) {
            showError(
                "First name, last name, email, and password required.");
            return;
        }
        if (!pass.equals(confirmPass)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            System.out.println("Attempting customer sign up via service...");
            User newUser = userService.registerCustomer(fname, lname, email,
            pass, phone, address);

            showAlert(Alert.AlertType.INFORMATION, "Sign Up Successful",
                      "Registration complete! Welcome, " + fname + ".");

            
            mainApp.loadScene(
                "/com/cafe94/gui/CustomerActionScreen.fxml");
            Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
            stage.setTitle("Cafe94 - Welcome");
        } catch (IllegalArgumentException e) {
             showError("Sign up failed: " + e.getMessage());
        } catch (IOException e) {
             System.err.println("Sign up error: " + e);
             showError("An unexpected error occurred during sign up.");
        }
    }

    private void showError(String message) {
        if(errorLabel != null) errorLabel.setText(message);
        else showAlert(Alert.AlertType.ERROR, "Validation Error",
        message);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}