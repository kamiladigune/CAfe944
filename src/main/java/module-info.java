module com.cafe94.gui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    
    opens com.cafe94.gui to javafx.fxml;
    exports com.cafe94.gui;
}