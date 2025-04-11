package com.cafe94.gui;

import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;
import com.cafe94.persistence.IUserRepository;
import com.cafe94.services.IUserService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StaffManagementScreen implements Main.NeedsMainApp {

    @FXML private TableView<Staff> staffTable;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button removeButton;
    @FXML private Label titleLabel;

    private final ObservableList<Staff> staffList =
        FXCollections.observableArrayList();
    private IUserService userService;
    private IUserRepository userRepository;
    private User currentUser;
    private Main mainApp;

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setUserService(IUserService s) {
        this.userService = s;
    }
    public void setUserRepository(IUserRepository r){
        this.userRepository = r;}
    public void setCurrentUser(User u) {
        this.currentUser = u;
    }

    @FXML
    public void initialize() {
        System.out.println("Initializing StaffManagementScreen...");
        Objects.requireNonNull(userService, "User Service is null");
        Objects.requireNonNull(userRepository, "User Repo is null");
        Objects.requireNonNull(currentUser, "Current User is null");

        setupTableColumns();
        staffTable.setItems(staffList);
        staffTable.setPlaceholder(new Label("Loading staff..."));

        loadStaffList();
    }

    private void setupTableColumns() {
        TableColumn<Staff, String> idCol = new TableColumn<>("Staff ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("staffId"));
        idCol.setPrefWidth(80);

        TableColumn<Staff, String> fNameCol = new TableColumn<>("First");
        fNameCol.setCellValueFactory(new PropertyValueFactory<>("firstName"));
        fNameCol.setPrefWidth(120);

        TableColumn<Staff, String> lNameCol = new TableColumn<>("Last");
        lNameCol.setCellValueFactory(new PropertyValueFactory<>("lastName"));
        lNameCol.setPrefWidth(120);

        TableColumn<Staff, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(180);

        TableColumn<Staff, UserRole> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setPrefWidth(100);

        staffTable.getColumns().setAll(idCol, fNameCol, lNameCol,
                                       emailCol, roleCol);
    }

    private void loadStaffList() {
        try {
            List<Staff> allStaff = userRepository.findAllStaff();
            staffList.setAll(allStaff);
            staffTable.setPlaceholder(new Label("No staff members."));
            System.out.println("Loaded " + allStaff.size() + " staff.");
        } catch (Exception e) {
            System.err.println("Error load staff: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load staff list.");
            staffTable.setPlaceholder(new Label("Error loading list."));
        }
    }

    @FXML
    private void handleAddStaff() {
        System.out.println("Add Staff clicked");
        AddEditStaffDialog dialog = new AddEditStaffDialog(
            userService, currentUser);

        Optional<Staff> result = dialog.showAndWaitAndReturn();

        result.ifPresent(newStaff -> {
             System.out.println("Dialog OK, new staff: " + newStaff);
             loadStaffList();
        });
    }

    @FXML
    private void handleEditStaff() {
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select staff to edit.");
            return;
        }
        System.out.println("Edit Staff clicked for: " + selected);

        AddEditStaffDialog dialog = new AddEditStaffDialog(
            selected, userService, currentUser);

        Optional<Staff> result = dialog.showAndWaitAndReturn();

        result.ifPresent(updatedStaff -> {
            System.out.println("Dialog OK, updated: " + updatedStaff);
            staffTable.refresh();
        });
    }

    @FXML
    private void handleRemoveStaff() {
        Staff selected = staffTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                      "Please select staff to remove.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Removal");
        confirm.setHeaderText("Remove " + selected.getFirstName() + "?");
        confirm.setContentText("Are you sure? This cannot be undone.");

        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isPresent() && choice.get() == ButtonType.OK) {
            System.out.println("Attempting remove: " + selected);
            try {
                boolean deleted = userService.removeStaff(
                    selected.getUserID(), currentUser);
                if (deleted) {
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                              "Staff member removed.");
                    loadStaffList();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Failed",
                              "Could not remove staff (service failed).");
                }
            } catch (Exception e) {
                 System.err.println("Error removing staff: " + e);
                 showAlert(Alert.AlertType.ERROR, "Error",
                           "Could not remove staff:\n" + e.getMessage());
            }
        } else {
            System.out.println("Removal cancelled.");
        }
    }

    private void showAlert(Alert.AlertType t, String title, String msg) {
        Alert alert = new Alert(t);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label content = new Label(msg);
        content.setWrapText(true);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
}