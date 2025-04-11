package com.cafe94.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class CustomerWelcomeScreen implements Main.NeedsMainApp {

    @FXML private Button signUpButton;
    @FXML private Button loginButton;

    private Main mainApp;

    @Override
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        System.out.println("CustomerWelcomeScreen initialized.");
    }

    @FXML
    private void handleSignUp() {
        System.out.println("Sign Up button clicked.");
        if (mainApp == null) {
            showAlert("Nav Error");
            return;
        }
        try {
            mainApp.loadScene("/com/cafe94/gui/CustomerSignUpScreen.fxml");
            Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
            stage.setTitle("Cafe94 - Customer Sign Up");
        } catch (IOException e) {
            showAlert("Could not open sign up screen.");
        }
    }

    @FXML
    private void handleLogin() {
        System.out.println("Login button clicked.");
        if (mainApp == null) {
            showAlert("Nav Error");
            return;
        }
        try {
            mainApp.loadScene("/com/cafe94/gui/CustomerLoginScreen.fxml");
            Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
            stage.setTitle("Cafe94 - Customer Login");
        } catch (IOException e) {
            showAlert("Could not open login screen.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}