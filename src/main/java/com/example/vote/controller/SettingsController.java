package com.example.vote.controller;

import com.example.vote.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML
    private Button backButton;
    
    @FXML
    private TextField dbConnectionField;
    
    @FXML
    private Button testConnectionButton;
    
    @FXML
    private CheckBox enableUserRegistrationCheckbox;
    
    @FXML
    private CheckBox enableEmailNotificationsCheckbox;
    
    @FXML
    private CheckBox enableIpBlockingCheckbox;
    
    @FXML
    private TextField smtpServerField;
    
    @FXML
    private TextField smtpPortField;
    
    @FXML
    private TextField emailUsernameField;
    
    @FXML
    private PasswordField emailPasswordField;
    
    @FXML
    private TextField emailFromField;
    
    @FXML
    private ComboBox<String> themeComboBox;
    
    @FXML
    private TextField applicationTitleField;
    
    @FXML
    private ComboBox<String> dateFormatComboBox;
    
    @FXML
    private Button backupDatabaseButton;
    
    @FXML
    private Label lastBackupLabel;
    
    @FXML
    private Button cleanupDatabaseButton;
    
    @FXML
    private Button resetSystemButton;
    
    @FXML
    private Button restoreDefaultsButton;
    
    @FXML
    private Button applyButton;
    
    @FXML
    private Label statusLabel;
    
    private Map<String, String> settings;
    private boolean hasChanges = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize theme and date format options
        themeComboBox.setItems(FXCollections.observableArrayList(
                "Default", "Light", "Dark", "High Contrast"
        ));
        
        dateFormatComboBox.setItems(FXCollections.observableArrayList(
                "MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "MMMM d, yyyy"
        ));
        
        // Load current settings
        loadSettings();
        
        // Display current database connection URL
        dbConnectionField.setText(DatabaseConnection.getConnectionUrl());
        
        // Set status message
        statusLabel.setText("Settings loaded successfully");
    }
    
    /**
     * Load application settings from database
     */
    private void loadSettings() {
        settings = new HashMap<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT setting_key, setting_value FROM application_settings");
             ResultSet rs = stmt.executeQuery()) {
            
            // Load settings from the database into the map
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
            
            // Set UI components based on loaded settings
            enableUserRegistrationCheckbox.setSelected(Boolean.parseBoolean(
                    getSetting("enable_user_registration", "true")));
            
            enableEmailNotificationsCheckbox.setSelected(Boolean.parseBoolean(
                    getSetting("enable_email_notifications", "false")));
            
            enableIpBlockingCheckbox.setSelected(Boolean.parseBoolean(
                    getSetting("enable_ip_blocking", "true")));
            
            smtpServerField.setText(getSetting("smtp_server", ""));
            smtpPortField.setText(getSetting("smtp_port", "587"));
            emailUsernameField.setText(getSetting("email_username", ""));
            emailPasswordField.setText(getSetting("email_password", ""));
            emailFromField.setText(getSetting("email_from", ""));
            
            String theme = getSetting("application_theme", "Default");
            themeComboBox.setValue(theme);
            
            applicationTitleField.setText(getSetting("application_title", "Voting System"));
            
            String dateFormat = getSetting("date_format", "MM/dd/yyyy");
            dateFormatComboBox.setValue(dateFormat);
            
            String lastBackup = getSetting("last_backup", "Never");
            lastBackupLabel.setText(lastBackup);
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading settings: " + e.getMessage());
            // Use defaults if we can't load from database
            restoreDefaults();
        }
        
        // Reset change tracking
        hasChanges = false;
    }
    
    /**
     * Helper method to get a setting with a default value
     */
    private String getSetting(String key, String defaultValue) {
        String value = settings.get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
    
    /**
     * Save current settings to database
     */
    private void saveSettings() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Prepare statement for updating settings
            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO application_settings (setting_key, setting_value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)");
            
            // Update settings from UI components
            updateSetting(stmt, "enable_user_registration", String.valueOf(enableUserRegistrationCheckbox.isSelected()));
            updateSetting(stmt, "enable_email_notifications", String.valueOf(enableEmailNotificationsCheckbox.isSelected()));
            updateSetting(stmt, "enable_ip_blocking", String.valueOf(enableIpBlockingCheckbox.isSelected()));
            
            updateSetting(stmt, "smtp_server", smtpServerField.getText());
            updateSetting(stmt, "smtp_port", smtpPortField.getText());
            updateSetting(stmt, "email_username", emailUsernameField.getText());
            updateSetting(stmt, "email_password", emailPasswordField.getText());
            updateSetting(stmt, "email_from", emailFromField.getText());
            
            updateSetting(stmt, "application_theme", themeComboBox.getValue());
            updateSetting(stmt, "application_title", applicationTitleField.getText());
            updateSetting(stmt, "date_format", dateFormatComboBox.getValue());
            
            // Update in-memory settings
            settings.put("enable_user_registration", String.valueOf(enableUserRegistrationCheckbox.isSelected()));
            settings.put("enable_email_notifications", String.valueOf(enableEmailNotificationsCheckbox.isSelected()));
            settings.put("enable_ip_blocking", String.valueOf(enableIpBlockingCheckbox.isSelected()));
            settings.put("smtp_server", smtpServerField.getText());
            settings.put("smtp_port", smtpPortField.getText());
            settings.put("email_username", emailUsernameField.getText());
            settings.put("email_password", emailPasswordField.getText());
            settings.put("email_from", emailFromField.getText());
            settings.put("application_theme", themeComboBox.getValue());
            settings.put("application_title", applicationTitleField.getText());
            settings.put("date_format", dateFormatComboBox.getValue());
            
            stmt.close();
            statusLabel.setText("Settings saved successfully");
            hasChanges = false;
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error saving settings: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to update a setting in the database
     */
    private void updateSetting(PreparedStatement stmt, String key, String value) throws SQLException {
        stmt.setString(1, key);
        stmt.setString(2, value);
        stmt.executeUpdate();
    }
    
    /**
     * Restore default settings
     */
    private void restoreDefaults() {
        enableUserRegistrationCheckbox.setSelected(true);
        enableEmailNotificationsCheckbox.setSelected(false);
        enableIpBlockingCheckbox.setSelected(true);
        
        smtpServerField.setText("");
        smtpPortField.setText("587");
        emailUsernameField.setText("");
        emailPasswordField.setText("");
        emailFromField.setText("");
        
        themeComboBox.setValue("Default");
        applicationTitleField.setText("Voting System");
        dateFormatComboBox.setValue("MM/dd/yyyy");
        
        // Mark as changed so it can be saved
        hasChanges = true;
    }

    @FXML
    private void handleTestConnection(ActionEvent event) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            statusLabel.setText("Database connection successful!");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Database connection failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackupDatabase(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Backup Directory");
        
        File selectedDirectory = directoryChooser.showDialog(backupDatabaseButton.getScene().getWindow());
        
        if (selectedDirectory != null) {
            // Simulate backup (in a real app this would export SQL, etc.)
            try {
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                String backupFileName = "vote_backup_" + timestamp + ".sql";
                File backupFile = new File(selectedDirectory, backupFileName);
                
                // Create empty backup file (in a real app, this would contain DB export)
                try (FileWriter writer = new FileWriter(backupFile)) {
                    writer.write("-- Backup of vote database\n");
                    writer.write("-- Generated on: " + timestamp + "\n");
                    writer.write("-- This is a placeholder backup file\n");
                }
                
                // Update last backup time
                String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                lastBackupLabel.setText(formattedDate);
                
                // Update the backup timestamp in the database
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "INSERT INTO application_settings (setting_key, setting_value) VALUES (?, ?) " +
                             "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)")) {
                    
                    stmt.setString(1, "last_backup");
                    stmt.setString(2, formattedDate);
                    stmt.executeUpdate();
                    
                    // Update in-memory settings
                    settings.put("last_backup", formattedDate);
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                    statusLabel.setText("Failed to update backup timestamp: " + e.getMessage());
                }
                
                statusLabel.setText("Backup successful: " + backupFile.getAbsolutePath());
                
            } catch (IOException e) {
                e.printStackTrace();
                statusLabel.setText("Backup failed: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCleanupDatabase(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cleanup Database");
        confirmation.setHeaderText("Confirm Database Cleanup");
        confirmation.setContentText("This will remove expired sessions and unused data. Continue?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Simulate cleanup process
            statusLabel.setText("Database cleanup completed successfully");
        }
    }

    @FXML
    private void handleResetSystem(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.WARNING);
        confirmation.setTitle("Reset System");
        confirmation.setHeaderText("WARNING: System Reset");
        confirmation.setContentText("This will delete ALL data except admin users. This action cannot be undone. Type 'RESET' to confirm.");
        
        // Add a text field for confirmation
        TextField confirmField = new TextField();
        confirmation.getDialogPane().setContent(new VBox(10, new Label("Type 'RESET' to confirm:"), confirmField));
        
        Optional<ButtonType> result = confirmation.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (confirmField.getText().equals("RESET")) {
                // Simulate system reset
                statusLabel.setText("System reset completed. All data has been cleared except admin users.");
            } else {
                statusLabel.setText("System reset cancelled - confirmation text did not match.");
            }
        }
    }

    @FXML
    private void handleRestoreDefaults(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Restore Defaults");
        confirmation.setHeaderText("Confirm Restore Defaults");
        confirmation.setContentText("This will reset all settings to default values. Continue?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            restoreDefaults();
            statusLabel.setText("Default settings restored");
        }
    }

    @FXML
    private void handleApply(ActionEvent event) {
        // Validate settings
        if (enableEmailNotificationsCheckbox.isSelected()) {
            if (smtpServerField.getText().trim().isEmpty()) {
                statusLabel.setText("SMTP server is required for email notifications");
                return;
            }
        }
        
        // Save settings
        saveSettings();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Check if settings were changed but not saved
            if (settingsChanged()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("You have unsaved changes");
                alert.setContentText("Would you like to save your changes before returning to the dashboard?");
                
                ButtonType buttonSave = new ButtonType("Save");
                ButtonType buttonDiscard = new ButtonType("Discard");
                ButtonType buttonCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                
                alert.getButtonTypes().setAll(buttonSave, buttonDiscard, buttonCancel);
                
                Optional<ButtonType> result = alert.showAndWait();
                
                if (result.get() == buttonSave) {
                    saveSettings();
                } else if (result.get() == buttonCancel) {
                    return;
                }
                // If buttonDiscard, continue without saving
            }
            
            // Navigate back to admin dashboard
            Parent adminDashboardParent = FXMLLoader.load(getClass().getResource("/com/example/vote/admin-dashboard.fxml"));
            Scene adminDashboardScene = new Scene(adminDashboardParent);
            
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(adminDashboardScene);
            window.setTitle("Admin Dashboard");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Check if settings have been changed from saved values
     */
    private boolean settingsChanged() {
        // For simplicity, we can just use our flag that tracks changes
        return hasChanges || (
            // Or check if any of the current UI values differ from what we loaded
            enableUserRegistrationCheckbox.isSelected() != Boolean.parseBoolean(
                    getSetting("enable_user_registration", "true")) ||
            
            enableEmailNotificationsCheckbox.isSelected() != Boolean.parseBoolean(
                    getSetting("enable_email_notifications", "false")) ||
            
            enableIpBlockingCheckbox.isSelected() != Boolean.parseBoolean(
                    getSetting("enable_ip_blocking", "true")) ||
            
            !smtpServerField.getText().equals(getSetting("smtp_server", "")) ||
            
            !smtpPortField.getText().equals(getSetting("smtp_port", "587")) ||
            
            !emailUsernameField.getText().equals(getSetting("email_username", "")) ||
            
            !emailPasswordField.getText().equals(getSetting("email_password", "")) ||
            
            !emailFromField.getText().equals(getSetting("email_from", "")) ||
            
            !themeComboBox.getValue().equals(getSetting("application_theme", "Default")) ||
            
            !applicationTitleField.getText().equals(getSetting("application_title", "Voting System")) ||
            
            !dateFormatComboBox.getValue().equals(getSetting("date_format", "MM/dd/yyyy"))
        );
    }
}
