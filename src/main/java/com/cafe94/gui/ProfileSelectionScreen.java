package com.cafe94.gui;

import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;
import com.cafe94.util.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ProfileSelectionScreen implements Main.NeedsMainApp,
Main.NeedsSessionManager {

    @FXML private ListView<User> profileListView;
    @FXML private Button selectButton;
    @FXML private Label titleLabel;
    

    private final ObservableList<User> availableProfiles =
        FXCollections.observableArrayList();

    private SessionManager sessionManager;
    private Main mainApp;
    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    @Override public void setSessionManager(SessionManager s) {
        this.sessionManager = s;
    }
    public void setProfiles(List<User> profiles) {
        if (profiles != null) {
            availableProfiles.setAll(profiles);
            profileListView.setPlaceholder(new Label("No profiles provided."));
        } else {
            availableProfiles.clear();
            profileListView.setPlaceholder(new Label("Error loading profiles."));
            showAlert(Alert.AlertType.ERROR, "Error", "No profiles received.");
        }
         System.out.println("Loaded " + availableProfiles.size() +
                            " profiles for selection.");
    }


    @FXML
    public void initialize() {
        System.out.println("Initializing ProfileSelectionScreen...");

        setupListView();
        profileListView.setItems(availableProfiles);
        profileListView.setPlaceholder(new Label("Loading profiles..."));

        if (titleLabel != null) {
            titleLabel.setText("Select Your Profile");
        }
    }

    private void setupListView() {
        profileListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(String.format("%s %s (%s)",
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole().toString()));
                }
            }
        });
    }

    @FXML
    private void handleProfileSelection() {
        User selected = profileListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select a profile from the list.");
            return;
        }

        if (sessionManager == null || mainApp == null) {
             showAlert(Alert.AlertType.ERROR, "Internal Error",
                       "Required context not available.");
            return;
        }

        System.out.println("Selected Profile: " + selected.getEmail() +
                           " Role: " + selected.getRole());

        sessionManager.setCurrentUser(selected);

        showAlert(Alert.AlertType.INFORMATION, "Profile Selected",
            "Proceeding as: " + selected.getFirstName() +
            " (" + selected.getRole() + ")");
        navigateToDashboard(selected);
    }

    private void navigateToDashboard(User user) {
        Stage currentStage = (Stage) selectButton.getScene().getWindow();
        String screenTitle = "Cafe94";
        String fxmlPath = null;
        UserRole role = user.getRole();

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
                showAlert(Alert.AlertType.ERROR, "Navigation Error",
                          "Unknown or invalid role: " + role);
                return;
        }

        try {
            mainApp.loadScene(fxmlPath);
            currentStage.setTitle(screenTitle);
        } catch (IOException ex) {
             System.err.println("Error navigating from ProfileSelect: " + ex);
             showAlert(Alert.AlertType.ERROR, "Navigation Error",
                       "Failed to open dashboard for role " + role + ".");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label content = new Label(msg);
        content.setWrapText(true);
        content.setMaxWidth(300);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
}