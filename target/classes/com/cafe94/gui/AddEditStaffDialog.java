package com.cafe94.gui;

import com.cafe94.domain.Staff;
import com.cafe94.domain.User;
import com.cafe94.dto.StaffDetailsDto;
import com.cafe94.enums.UserRole;
import com.cafe94.services.IUserService;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class AddEditStaffDialog extends Dialog<Optional<Staff>> {

    private final TextField staffIdField = new TextField();
    private final TextField firstNameField = new TextField();
    private final TextField lastNameField = new TextField();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final ComboBox<UserRole> roleComboBox = new ComboBox<>();

    private final IUserService userService;
    private final User currentUser;
    private final Staff editingStaff; // Null if in Add mode

    public AddEditStaffDialog(IUserService userService, User currentUser) {
        this(null, userService, currentUser); // Call edit constructor
    }

    public AddEditStaffDialog(Staff staffToEdit, IUserService userService,
                              User currentUser) {
        this.editingStaff = staffToEdit; // May be null for Add mode
        this.userService = Objects.requireNonNull(userService,
            "UserService cannot be null in AddEditStaffDialog");
        this.currentUser = Objects.requireNonNull(currentUser,
            "Current user cannot be null in AddEditStaffDialog");

        setTitle(editingStaff == null ? "Add New Staff" : "Edit Staff");
        initModality(Modality.APPLICATION_MODAL);

        GridPane grid = createGridPane();
        setupControls(grid);
        populateFieldsForEdit();

        getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save",
                                        ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveButtonType,
                                               ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return createOrUpdateStaff(); // Returns Optional<Staff>
            }
            return Optional.empty(); // Cancel or close returns empty
        });
    }

    private GridPane createGridPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        return grid;
    }

    private void setupControls(GridPane grid) {
        roleComboBox.setItems(FXCollections.observableArrayList(
            Stream.of(UserRole.values())
                  .filter(UserRole::isStaffRole)
                  .toArray(UserRole[]::new)
        ));

        int rowIndex = 0;
        grid.add(new Label("Staff ID:"), 0, rowIndex);
        grid.add(staffIdField, 1, rowIndex++);

        grid.add(new Label("First Name:"), 0, rowIndex);
        grid.add(firstNameField, 1, rowIndex++);

        grid.add(new Label("Last Name:"), 0, rowIndex);
        grid.add(lastNameField, 1, rowIndex++);

        grid.add(new Label("Email:"), 0, rowIndex);
        grid.add(emailField, 1, rowIndex++);

        grid.add(new Label("Role:"), 0, rowIndex);
        grid.add(roleComboBox, 1, rowIndex++);

        if (editingStaff == null) { // Only show password for new staff
            grid.add(new Label("Password:"), 0, rowIndex);
            grid.add(passwordField, 1, rowIndex++);
            staffIdField.setPromptText("Enter unique Staff ID");
        } else {
            staffIdField.setEditable(false); // Don't allow editing ID
        }
    }

    private void populateFieldsForEdit() {
        if (editingStaff != null) {
            staffIdField.setText(editingStaff.getStaffId());
            firstNameField.setText(editingStaff.getFirstName());
            lastNameField.setText(editingStaff.getLastName());
            emailField.setText(editingStaff.getEmail());
            roleComboBox.setValue(editingStaff.getRole());
        }
    }

    private Optional<Staff> createOrUpdateStaff() {
        String staffId = staffIdField.getText();
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        UserRole role = roleComboBox.getValue();
        String password =
            (editingStaff == null) ? passwordField.getText() : null;

        if (Stream.of(staffId, firstName, lastName, email)
                  .anyMatch(s -> s == null || s.trim().isEmpty())
            || role == null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                "Staff ID, First/Last Name, Email, Role required.");
            return Optional.empty(); // Indicate validation failure
        }

        if (editingStaff == null && (password == null || password.isEmpty())) {
            showAlert(Alert.AlertType.ERROR, "Validation Error",
                      "Password is required for new staff members.");
            return Optional.empty(); // Indicate validation failure
        }

        try {
            if (editingStaff == null) {
                System.out.println("Attempting hireStaff via service...");
                Staff newStaff = userService.hireStaff(firstName, lastName,
                    email, password, role, staffId, 0.0, currentUser);
                showAlert(Alert.AlertType.INFORMATION, "Success",
                          "New staff member hired successfully.");
                return Optional.ofNullable(newStaff);
            } else {
                System.out.println("Attempting updateStaffDetails (ID: " +
                                   editingStaff.getUserID() + ")");
                StaffDetailsDto dto = new StaffDetailsDto(firstName,
                                                          lastName, email);
                // Role change handling is noted as not supported by service
                if (role != editingStaff.getRole()) {
                    System.out.println("WARN: Role change not handled.");
                    showAlert(Alert.AlertType.WARNING,
                        "Role Change Not Implemented",
                        "Changing roles is not currently supported.");
                }
                Staff updated = userService.updateStaffDetails(
                    editingStaff.getUserID(), dto, currentUser);
                showAlert(Alert.AlertType.INFORMATION, "Success",
                          "Staff details updated successfully.");
                return Optional.ofNullable(updated);
            }
        } catch (RuntimeException ex) {
             System.err.println("Error during staff operation: " + ex);
             showAlert(Alert.AlertType.ERROR, "Operation Failed",
                       "Could not save staff:\n" + ex.getMessage());
             return Optional.empty(); // Indicate failure
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // Added helper method for StaffManagementScreen
    public Optional<Staff> showAndWaitAndReturn() {
        Optional<Optional<Staff>> result = showAndWait();
        // Unwrap the outer Optional from the dialog itself
        return result.orElse(Optional.empty());
    }
}