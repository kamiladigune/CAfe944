package com.cafe94.gui;

import com.cafe94.domain.Item;
import com.cafe94.services.IMenuService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class MenuViewScreen implements Main.NeedsMainApp {

    @FXML private TableView<Item> menuTable;
    @FXML private Label specialDetailsLabel;
    @FXML private VBox specialDisplayBox;
    @FXML private Label titleLabel;
    private final ObservableList<Item> menuItems =
        FXCollections.observableArrayList();

    private IMenuService menuService;
    private Main mainApp;

    private static final NumberFormat CURRENCY_FORMATTER =
        NumberFormat.getCurrencyInstance(Locale.UK);

    @Override public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
    }
    public void setMenuService(IMenuService menuService) {
        this.menuService = Objects.requireNonNull(menuService);
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(menuService, "MenuService is null");

        System.out.println("Initializing MenuViewScreen...");

        configureSpecialDisplayBox();
        setupTableColumns();
        menuTable.setItems(menuItems);
        menuTable.setPlaceholder(new Label("Menu loading..."));

        loadMenuAndSpecial();

        if (titleLabel != null) {
            titleLabel.setText("Menu");
        }
    }

    private void configureSpecialDisplayBox() {
        if (specialDisplayBox != null) {
            specialDisplayBox.setVisible(false);
            specialDisplayBox.setManaged(false);
        }
        if (specialDetailsLabel != null) {
            specialDetailsLabel.setWrapText(true);
        }
    }


    private void setupTableColumns() {
        TableColumn<Item, String> nameCol = new TableColumn<>("Item");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);

        TableColumn<Item, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(300);

        TableColumn<Item, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        priceCol.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null :
                        CURRENCY_FORMATTER.format(price));
            }
        });

        menuTable.getColumns().setAll(nameCol, descCol, priceCol);
    }

    private void loadMenuAndSpecial() {
        if (menuService == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Menu service not available.");
            menuTable.setPlaceholder(new Label("Error loading menu."));
            displaySpecialError("Could not load special.");
            return;
        }

        try {
            List<Item> items = menuService.getAllItems();
            List<Item> regular = items.stream()
                                      .filter(item -> !item.isDailySpecial())
                                      .collect(Collectors.toList());
            menuItems.setAll(regular);
            menuTable.setPlaceholder(new Label("Menu is currently empty."));
            System.out.println("Loaded " + regular.size() + " menu items.");
        } catch (Exception e) {
            System.err.println("Error loading menu items: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load menu items.");
            menuTable.setPlaceholder(new Label("Error loading menu."));
        }

        try {
            List<Item> specials = menuService.getDailySpecials();
            if (specials.isEmpty()) {
                displaySpecialText("No daily special set today.");
                System.out.println("No daily specials found.");
            } else {
                Item special = specials.get(0);
                String text = String.format("%s - %s", special.getName(),
                    CURRENCY_FORMATTER.format(special.getPrice()));
                displaySpecialText(text);
                System.out.println("Loaded special: " + special.getName());
                if (specials.size() > 1) {
                     System.out.println("WARN: Multiple specials found.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading daily special: " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error",
                      "Failed to load daily special.");
            displaySpecialError("Error loading special.");
        }
    }

    private void displaySpecialText(String text) {
        if (specialDetailsLabel != null && specialDisplayBox != null) {
            specialDetailsLabel.setText(text);
            specialDisplayBox.setVisible(true);
            specialDisplayBox.setManaged(true);
        }
    }

     private void displaySpecialError(String errorText) {
          if (specialDetailsLabel != null && specialDisplayBox != null) {
              specialDetailsLabel.setText("Error: " + errorText);
              specialDisplayBox.setVisible(true);
              specialDisplayBox.setManaged(true);
          }
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