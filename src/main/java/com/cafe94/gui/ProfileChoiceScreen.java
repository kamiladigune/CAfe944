package com.cafe94.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

public class ProfileChoiceScreen implements Main.NeedsMainApp {

    private Main mainApp;

    @Override
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        System.out.println("ProfileChoiceScreen initialized.");
    }

    @FXML
    private void navigateToStaffLogin() {
        System.out.println("Staff button clicked. Navigating to Staff Login...");
        if (mainApp == null) {
             showAlert(Alert.AlertType.ERROR, "Error", "Nav error.");
             return;
         }
        try {
            mainApp.loadScene("/com/cafe94/gui/LoginScreen.fxml");
             Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
             stage.setTitle("Cafe94 - Staff Login");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                      "Could not open staff login screen.");
        }
    }


    @FXML
    private void navigateToCustomerLogin() {
        System.out.println("Customer button clicked. Navigating to Customer Welcome...");
         if (mainApp == null) {
             showAlert(Alert.AlertType.ERROR, "Error", "Nav error.");
             return;
         }
        try {
            mainApp.loadScene("/com/cafe94/gui/CustomerWelcomeScreen.fxml");
             Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
             stage.setTitle("Cafe94 - Customer Portal");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                      "Could not open customer welcome screen.");
        }
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert alert = new Alert(t);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}