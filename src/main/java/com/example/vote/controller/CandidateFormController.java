package com.example.vote.controller;

import com.example.vote.model.Candidate;
import com.example.vote.model.Election;
import com.example.vote.util.DatabaseConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class CandidateFormController implements Initializable {

    @FXML
    private TextField nameField;

    @FXML
    private TextField positionField;

    @FXML
    private TextField photoUrlField;

    @FXML
    private Button browseButton;

    @FXML
    private TextArea profileArea;

    @FXML
    private TextField votesField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private Candidate candidate;
    private Election election;
    private ManageCandidatesController parentController;
    private boolean isEditMode = false;
    private boolean readOnly = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize default values or setup UI elements if needed
    }

    public void initData(Candidate candidate, Election election, 
                         ManageCandidatesController parentController, boolean readOnly) {
        this.election = election;
        this.parentController = parentController;
        this.readOnly = readOnly;
        
        if (candidate != null) {
            // Edit mode
            this.candidate = candidate;
            this.isEditMode = true;
            populateFormWithCandidateData();
        } else {
            // Add mode
            this.candidate = new Candidate();
            this.candidate.setElectionId(election.getId());
            this.isEditMode = false;
            votesField.setText("0");
        }
        
        // If read-only mode, disable all fields
        if (readOnly) {
            nameField.setEditable(false);
            positionField.setEditable(false);
            photoUrlField.setEditable(false);
            profileArea.setEditable(false);
            browseButton.setDisable(true);
            saveButton.setText("Close");
            saveButton.setOnAction(this::handleCancel);
        }
    }
    
    private void populateFormWithCandidateData() {
        nameField.setText(candidate.getName());
        positionField.setText(candidate.getPosition());
        photoUrlField.setText(candidate.getPhotoUrl());
        profileArea.setText(candidate.getProfile());
        votesField.setText(String.valueOf(candidate.getVotes()));
    }
    
    @FXML
    private void handleBrowsePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Candidate Photo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File selectedFile = fileChooser.showOpenDialog(browseButton.getScene().getWindow());
        if (selectedFile != null) {
            photoUrlField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        if (validateInputs()) {
            // Get values from form
            String name = nameField.getText().trim();
            String position = positionField.getText().trim();
            String photoUrl = photoUrlField.getText().trim();
            String profile = profileArea.getText().trim();
            
            // Update candidate object
            candidate.setName(name);
            candidate.setPosition(position);
            candidate.setPhotoUrl(photoUrl);
            candidate.setProfile(profile);
            
            // Save to database
            if (isEditMode) {
                updateCandidate();
            } else {
                createCandidate();
            }
        }
    }
    
    private boolean validateInputs() {
        errorLabel.setText("");
        
        if (nameField.getText().trim().isEmpty()) {
            errorLabel.setText("Name cannot be empty");
            return false;
        }
        
        if (positionField.getText().trim().isEmpty()) {
            errorLabel.setText("Position cannot be empty");
            return false;
        }
        
        return true;
    }
    
    private void createCandidate() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO candidates (election_id, name, profile, photo_url, position) VALUES (?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, election.getId());
            stmt.setString(2, candidate.getName());
            stmt.setString(3, candidate.getProfile());
            stmt.setString(4, candidate.getPhotoUrl());
            stmt.setString(5, candidate.getPosition());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 1) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    candidate.setId(generatedKeys.getInt(1));
                    
                    // Close the form and refresh parent table
                    closeForm(true);
                }
            } else {
                errorLabel.setText("Failed to create candidate. Please try again.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }
    
    private void updateCandidate() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE candidates SET name = ?, profile = ?, photo_url = ?, position = ? WHERE id = ?")) {
            
            stmt.setString(1, candidate.getName());
            stmt.setString(2, candidate.getProfile());
            stmt.setString(3, candidate.getPhotoUrl());
            stmt.setString(4, candidate.getPosition());
            stmt.setInt(5, candidate.getId());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 1) {
                // Close the form and refresh parent table
                closeForm(true);
            } else {
                errorLabel.setText("Failed to update candidate. Please try again.");
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
