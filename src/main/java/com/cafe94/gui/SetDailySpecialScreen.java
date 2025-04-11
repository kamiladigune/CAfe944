package com.cafe94.gui;

import com.cafe94.domain.Item;
import com.cafe94.domain.User;
import com.cafe94.enums.UserRole;
import com.cafe94.services.IMenuService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class SetDailySpecialScreen implements Main.NeedsMainApp {

    @FXML private RadioButton selectExistingRadio;
    @FXML private RadioButton createNewRadio;
    @FXML private ToggleGroup choiceToggleGroup;
    @FXML private ComboBox<Item> existingItemsComboBox;
    @FXML private TextField newNameField;
    @FXML private TextField newDescriptionField;
    @FXML private TextField newPriceField;
    @FXML private Button setSpecialButton;
    @FXML private GridPane newSpecialPane;
    @FXML private HBox existingBox;
    @FXML private Label titleLabel;

    private final ObservableList<Item> existingMenuItems =
        FXCollections.observableArrayList();

    private IMenuService menuService;
    private User currentUser;
    private Main mainApp;

    private static final NumberFormat CURRENCY_FORMATTER =
        NumberFormat.getCurrencyInstance(Locale.UK);

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setMenuService(IMenuService menuService) {
        this.menuService = Objects.requireNonNull(menuService);
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
        if (this.currentUser != null &&
             this.currentUser.getRole() != UserRole.CHEF) {
             System.err.println("WARN: Non-Chef accessing SetSpecial: " +
                                this.currentUser.getEmail());
         }
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(menuService, "MenuService is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing SetDailySpecialScreen for " +
                           currentUser.getEmail());

        configureComboBox();
        existingItemsComboBox.setItems(existingMenuItems);
        loadMenuItems();
        if (choiceToggleGroup != null) {
            setupControlToggling(choiceToggleGroup);
            toggleControls(selectExistingRadio.isSelected());
        } else {
             System.err.println("ToggleGroup not injected!");
        }

        if (titleLabel != null) {
            titleLabel.setText("Set Today's Daily Special");
        }
    }

    private void configureComboBox() {
        existingItemsComboBox.setPromptText("Choose an item...");
        existingItemsComboBox.setPrefWidth(300);
        existingItemsComboBox.setConverter(new StringConverter<Item>() {
            @Override public String toString(Item item) {
                return item == null ? null :
                       item.getName() + " (" +
                       CURRENCY_FORMATTER.format(item.getPrice()) + ")";
            }
            @Override public Item fromString(String string) { return null; }
        });
    }

    private void loadMenuItems() {
        if (menuService == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Menu service unavailable.");
            return;
        }
        try {
            List<Item> items = menuService.getAllItems();
            List<Item> nonSpecials = items.stream()
                .filter(item -> !item.isDailySpecial())
                .collect(Collectors.toList());
            existingMenuItems.setAll(nonSpecials);
            System.out.println("Loaded " + nonSpecials.size() + " items.");
        } catch (Exception e) {
             System.err.println("Error loading menu items: " + e);
             e.printStackTrace();
             showAlert(Alert.AlertType.ERROR, "Loading Error",
                       "Failed to load menu items for selection.");
        }
    }

    private void setupControlToggling(ToggleGroup group) {
         group.selectedToggleProperty().addListener(
             (observable, oldToggle, newToggle) -> {
                 toggleControls(newToggle == selectExistingRadio);
             });
    }

    private void toggleControls(boolean showExisting) {
        if (existingBox != null && newSpecialPane != null) {
             existingBox.setManaged(showExisting);
             existingBox.setVisible(showExisting);
             newSpecialPane.setManaged(!showExisting);
             newSpecialPane.setVisible(!showExisting);
        } else {
            System.err.println("Containers for toggling not injected!");
        }
    }

    @FXML
    private void handleSetSpecial() {
        if (currentUser == null || menuService == null) {
            showAlert(Alert.AlertType.ERROR, "Internal Error",
                      "Context or service not available.");
            return;
        }

        Item resultingSpecial = null;
        String successMessage = "";

        try {
            if (selectExistingRadio.isSelected()) {
                Item selected = existingItemsComboBox.getValue();
                if (selected == null) {
                    showAlert(Alert.AlertType.WARNING, "Selection Error",
                              "Please select an existing menu item.");
                    return;
                }
                System.out.println("Calling setDailySpecial service...");
                resultingSpecial = menuService.setDailySpecial(
                    selected.getItemID(), currentUser);
                successMessage = "Daily special updated to: " +
                                 resultingSpecial.getName();

            } else if (createNewRadio.isSelected()) {
                String name = newNameField.getText();
                String desc = newDescriptionField.getText();
                String priceStr = newPriceField.getText();

                if (name == null || name.trim().isEmpty() ||
                    priceStr == null || priceStr.trim().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Input Error",
                              "Name and Price are required for new special.");
                    return;
                }
                desc = (desc == null) ? "" : desc.trim();

                double price;
                try {
                    price = Double.parseDouble(priceStr);
                    if (price < 0) {
                        showAlert(Alert.AlertType.WARNING, "Input Error",
                                  "Price cannot be negative.");
                        return;
                    }
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.WARNING, "Input Error",
                        "Invalid price format. Use numbers (e.g., 10.50).");
                    return;
                }

                System.out.println("Calling setNewCreationAsSpecial...");
                resultingSpecial = menuService.setNewCreationAsSpecial(
                    name, desc, price, currentUser);
                successMessage = "New creation '" + resultingSpecial.getName() +
                                 "' set as transient daily special.";
            } else {
                 showAlert(Alert.AlertType.ERROR, "Logic Error",
                           "No selection mode active.");
                 return;
            }

            showAlert(Alert.AlertType.INFORMATION, "Success",
            successMessage);
            clearInputFields();
        } catch (Exception e) {
             System.err.println("Error setting daily special: " + e);
             e.printStackTrace();
             showAlert(Alert.AlertType.ERROR, "Operation Failed",
                       "Could not set daily special:\n" + e.getMessage());
        }
    }

    private void clearInputFields() {
         existingItemsComboBox.getSelectionModel().clearSelection();
         if (newNameField != null) newNameField.clear();
         if (newDescriptionField != null) newDescriptionField.clear();
         if (newPriceField != null) newPriceField.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label content = new Label(msg);
        content.setWrapText(true);
        content.setMaxWidth(400);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
}