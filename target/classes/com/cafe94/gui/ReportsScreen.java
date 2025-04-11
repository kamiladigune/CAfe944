package com.cafe94.gui;

import com.cafe94.domain.Report;
import com.cafe94.domain.User;
import com.cafe94.enums.ReportType; // Kept for potential future use if needed
import com.cafe94.enums.UserRole; // Added for role check
import com.cafe94.services.IReportingService;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReportsScreen implements Main.NeedsMainApp {

    @FXML private TextArea reportDisplayArea;
    @FXML private Button popularItemsButton;
    @FXML private Button busiestPeriodsButton;
    @FXML private Button activeCustomerButton;
    @FXML private Label titleLabel;

    private IReportingService reportingService;
    private User currentUser;
    private Main mainApp;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int TOP_CUSTOMER_LIMIT = 5;

    @Override public void setMainApp(Main mainApp) { this.mainApp = mainApp; }
    public void setReportingService(IReportingService reportingService) {
        this.reportingService = Objects.requireNonNull(reportingService);
    }
    public void setCurrentUser(User currentUser) {
        this.currentUser = Objects.requireNonNull(currentUser);
        // Optional check
         if (this.currentUser != null &&
             this.currentUser.getRole() != UserRole.MANAGER) {
             System.err.println("WARN: Non-Manager accessing Reports: " +
                                this.currentUser.getEmail());
             // Disable buttons or show error if not manager?
         }
    }

    @FXML
    public void initialize() {
        Objects.requireNonNull(reportingService, "ReportingService is null");
        Objects.requireNonNull(currentUser, "CurrentUser is null");

        System.out.println("Initializing ReportsScreen for " +
                           currentUser.getEmail());

        if (reportDisplayArea != null) {
            reportDisplayArea.setEditable(false);
            reportDisplayArea.setWrapText(true);
            reportDisplayArea.setText("Select a report to generate.");
        }
        if (titleLabel != null) {
            titleLabel.setText("Generate System Reports");
        }
        // Button actions linked via FXML onAction properties
    }

    @FXML
    private void handlePopularItems() {
        generateAndDisplayReport(ReportType.POPULAR_ITEMS,
                                 "Most Popular Items");
    }

    @FXML
    private void handleBusiestPeriods() {
        generateAndDisplayReport(ReportType.BUSIEST_PERIODS,
                                 "Busiest Periods");
    }

    @FXML
    private void handleActiveCustomer() {
        generateAndDisplayReport(ReportType.CUSTOMER_ACTIVITY,
                                 "Most Active Customers");
    }

    private void generateAndDisplayReport(ReportType type, String name) {
        if (reportingService == null || currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Error",
                      "Service or context not available.");
            return;
        }
        System.out.println("Requesting report: " + type);
        reportDisplayArea.setText("Generating " + name + "...");

        try {
            Report report = null;
            switch (type) {
                case POPULAR_ITEMS:
                    report = reportingService
                        .generateMostPopularItemsReport(currentUser);
                    break;
                case BUSIEST_PERIODS:
                    report = reportingService
                        .generateBusiestPeriodsReport(currentUser);
                    break;
                case CUSTOMER_ACTIVITY:
                    report = reportingService
                        .generateMostActiveCustomerReport(currentUser,
                                                          TOP_CUSTOMER_LIMIT);
                    break;
                default:
                    showAlert(Alert.AlertType.ERROR, "Error", "Unknown type.");
                    reportDisplayArea.setText("Unknown report type.");
                    return;
            }

            if (report != null) {
                displayReport(report);
                showAlert(Alert.AlertType.INFORMATION, "Report Generated",
                          name + " generated successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Generation failed.");
                reportDisplayArea.setText("Failed to generate report.");
            }
        } catch (Exception e) {
            System.err.println("Error generating report " + type + ": " + e);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Generation Error",
                      "Could not generate report:\n" + e.getMessage());
            reportDisplayArea.setText("Error: " + e.getMessage());
        }
    }

    private void displayReport(Report report) {
        if (report == null) {
            reportDisplayArea.setText("Received null report object.");
            return;
        }
        StringBuilder content = new StringBuilder();
        content.append("--- Report: ").append(report.getTitle())
               .append(" ---\n");
        content.append("Generated: ")
               .append(report.getGeneratedTimestamp()
                             .format(DATE_TIME_FORMATTER))
               .append("\n\n");

        Map<String, Object> data = report.getData();
        if (data == null || data.isEmpty()) {
            content.append("No data available for this report.");
        } else {
            content.append(formatReportData(data));
        }
        reportDisplayArea.setText(content.toString());
    }

    private String formatReportData(Map<String, Object> data) {
        return data.entrySet().stream()
            .map(entry -> {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Map) {
                    String nested = ((Map<?, ?>) value).entrySet().stream()
                        .map(sub -> "  - " + sub.getKey() + ": " + sub.getValue())
                        .collect(Collectors.joining("\n"));
                    return key + ":\n" + nested;
                } else {
                    return "- " + key + ": " + value;
                }
            })
            .collect(Collectors.joining("\n"));
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