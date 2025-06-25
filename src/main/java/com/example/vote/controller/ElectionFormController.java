package com.example.vote.controller;

import com.example.vote.model.Election;
import com.example.vote.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class ElectionFormController implements Initializable {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private ComboBox<String> startHourCombo;

    @FXML
    private ComboBox<String> startMinuteCombo;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> endHourCombo;

    @FXML
    private ComboBox<String> endMinuteCombo;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private Label errorLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Election election;
    private ManageElectionsController parentController;
    private boolean isEditMode = false;
    private int userId; // Current user ID for created_by field

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Populate hour combo boxes (0-23)
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }
        startHourCombo.setItems(hours);
        endHourCombo.setItems(hours);
        
        // Populate minute combo boxes (0-59)
        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format("%02d", i));
        }
        startMinuteCombo.setItems(minutes);
        endMinuteCombo.setItems(minutes);
        
        // Default to current hour and minute
        LocalTime now = LocalTime.now();
        startHourCombo.setValue(String.format("%02d", now.getHour()));
        startMinuteCombo.setValue(String.format("%02d", now.getMinute()));
        endHourCombo.setValue(String.format("%02d", now.getHour()));
        endMinuteCombo.setValue(String.format("%02d", now.getMinute()));
        
        // Populate status combo box
        statusCombo.setItems(FXCollections.observableArrayList("pending", "active", "completed", "cancelled"));
        statusCombo.setValue("pending");
        
        // Default dates to today
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(7)); // Default end date to a week from now
    }

    public void initData(Election election, ManageElectionsController parentController) {
        this.parentController = parentController;
        
        // Get current user ID from session or a utility class
        // For now, we'll use admin ID 1 as default
        this.userId = 1; // Replace with actual session user ID in a real application
        
        if (election != null) {
            // Edit mode
            this.election = election;
            this.isEditMode = true;
            populateFormWithElectionData();
        } else {
            // Add mode
            this.election = new Election();
            this.isEditMode = false;
        }
    }
    
    private void populateFormWithElectionData() {
        titleField.setText(election.getTitle());
        descriptionArea.setText(election.getDescription());
        
        LocalDateTime startDateTime = election.getStartDate();
        if (startDateTime != null) {
            startDatePicker.setValue(startDateTime.toLocalDate());
            startHourCombo.setValue(String.format("%02d", startDateTime.getHour()));
            startMinuteCombo.setValue(String.format("%02d", startDateTime.getMinute()));
        }
        
        LocalDateTime endDateTime = election.getEndDate();
        if (endDateTime != null) {
            endDatePicker.setValue(endDateTime.toLocalDate());
            endHourCombo.setValue(String.format("%02d", endDateTime.getHour()));
            endMinuteCombo.setValue(String.format("%02d", endDateTime.getMinute()));
        }
        
        statusCombo.setValue(election.getStatus());
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        if (validateInputs()) {
            // Get values from form
            String title = titleField.getText().trim();
            String description = descriptionArea.getText().trim();
            
            LocalDate startDate = startDatePicker.getValue();
            int startHour = Integer.parseInt(startHourCombo.getValue());
            int startMinute = Integer.parseInt(startMinuteCombo.getValue());
            LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute));
            
            LocalDate endDate = endDatePicker.getValue();
            int endHour = Integer.parseInt(endHourCombo.getValue());
            int endMinute = Integer.parseInt(endMinuteCombo.getValue());
            LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute));
            
            String status = statusCombo.getValue();
            
            // Update election object
            election.setTitle(title);
            election.setDescription(description);
            election.setStartDate(startDateTime);
            election.setEndDate(endDateTime);
            election.setStatus(status);
            
            // Save to database
            if (isEditMode) {
                updateElection();
            } else {
                election.setCreatedBy(userId);
                createElection();
            }
        }
    }
    
    private boolean validateInputs() {
        errorLabel.setText("");
        
        if (titleField.getText().trim().isEmpty()) {
            errorLabel.setText("Title cannot be empty");
            return false;
        }
        
        if (startDatePicker.getValue() == null) {
            errorLabel.setText("Start date must be selected");
            return false;
        }
        
        if (endDatePicker.getValue() == null) {
            errorLabel.setText("End date must be selected");
            return false;
        }
        
        // Create LocalDateTime objects for comparison
        LocalDate startDate = startDatePicker.getValue();
        int startHour = Integer.parseInt(startHourCombo.getValue());
        int startMinute = Integer.parseInt(startMinuteCombo.getValue());
        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute));
        
        LocalDate endDate = endDatePicker.getValue();
        int endHour = Integer.parseInt(endHourCombo.getValue());
        int endMinute = Integer.parseInt(endMinuteCombo.getValue());
        LocalDateTime endDateTime = LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute));
        
        // Check if end date is after start date
        if (!endDateTime.isAfter(startDateTime)) {
            errorLabel.setText("End date must be after start date");
            return false;
        }
        
        return true;
    }
    
    private void createElection() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO elections (title, description, start_date, end_date, status, created_by) VALUES (?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, election.getTitle());
            stmt.setString(2, election.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(election.getStartDate()));
            stmt.setTimestamp(4, Timestamp.valueOf(election.getEndDate()));
            stmt.setString(5, election.getStatus());
            stmt.setInt(6, election.getCreatedBy());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 1) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    election.setId(generatedKeys.getInt(1));
                    
                    // Close the form and refresh parent table
                    closeForm(true);
                }
            } else {
                errorLabel.setText("Failed to create election. Please try again.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }
    
    private void updateElection() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE elections SET title = ?, description = ?, start_date = ?, end_date = ?, status = ? WHERE id = ?")) {
            
            stmt.setString(1, election.getTitle());
            stmt.setString(2, election.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(election.getStartDate()));
            stmt.setTimestamp(4, Timestamp.valueOf(election.getEndDate()));
            stmt.setString(5, election.getStatus());
            stmt.setInt(6, election.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 1) {
                // Close the form and refresh parent table
                closeForm(true);
            } else {
                errorLabel.setText("Failed to update election. Please try again.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        closeForm(false);
    }
    
    private void closeForm(boolean refreshParent) {
        if (refreshParent && parentController != null) {
            parentController.refreshTable();
        }
        
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
