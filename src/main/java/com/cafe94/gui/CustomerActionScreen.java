package com.cafe94.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class CustomerActionScreen implements Main.NeedsMainApp {

    @FXML private Button bookingButton;
    @FXML private Button takeawayButton;

    private Main mainApp;

    @Override
    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    public void initialize() {
        System.out.println("CustomerActionScreen initialized.");
    }

    @FXML
    private void handleBooking() {
        System.out.println("Make Booking button clicked.");
        if (mainApp == null) {
            showAlert("Navigation Error");
            return;
        }
        try {
            mainApp.loadScene("/com/cafe94/gui/BookingRequestScreen.fxml");
            Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
            stage.setTitle("Cafe94 - Request Booking");
        } catch (IOException e) {
            showAlert("Could not open booking screen.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTakeaway() {
        System.out.println("Place Takeaway button clicked.");
        if (mainApp == null) {
            showAlert("Navigation Error");
            return;
        }
        try {
            mainApp.loadScene("/com/cafe94/gui/TakeawayOrderScreen.fxml");
            Stage stage = (Stage) mainApp.primaryStage.getScene().getWindow();
            stage.setTitle("Cafe94 - Place Takeaway Order");
        } catch (IOException e) {
            showAlert("Could not open takeaway order screen.");
            e.printStackTrace();
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