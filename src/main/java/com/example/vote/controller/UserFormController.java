package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import com.example.vote.util.PasswordHash;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class UserFormController implements Initializable {

    @FXML
    private Label titleLabel;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField fullnameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private ComboBox<String> roleComboBox;
    
    @FXML
    private Label passwordNoteLabel;
    
    @FXML
    private Label messageLabel;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    private User user;
    private boolean isEditMode = false;
    private ManageUsersController parentController;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize the role ComboBox with values
        roleComboBox.setItems(FXCollections.observableArrayList("admin", "user"));
        roleComboBox.getSelectionModel().select("user"); // Default role
    }
    
    public void initData(User user, ManageUsersController parentController) {
        this.parentController = parentController;
        
        if (user != null) {
            this.user = user;
            isEditMode = true;
            titleLabel.setText("Edit User");
            loadUserData();
            passwordNoteLabel.setVisible(true); // Show password note for edits
        } else {
            this.user = new User();
            isEditMode = false;
            titleLabel.setText("Add New User");
            passwordNoteLabel.setVisible(false);
        }
    }
    
    private void loadUserData() {
        usernameField.setText(user.getUsername());
        fullnameField.setText(user.getFullname());
        emailField.setText(user.getEmail());
        roleComboBox.getSelectionModel().select(user.getRole());
    }
    
    @FXML
    private void handleSave(ActionEvent event) {
        // Clear previous message
        messageLabel.setText("");
        
        // Get form values
        String username = usernameField.getText().trim();
        String fullname = fullnameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleComboBox.getSelectionModel().getSelectedItem();
        
        // Validate inputs
        if (username.isEmpty() || fullname.isEmpty() || email.isEmpty() || role == null) {
            messageLabel.setText("Please fill in all required fields.");
            return;
        }
        
        if (!isEditMode && (password.isEmpty() || confirmPassword.isEmpty())) {
            messageLabel.setText("Please enter and confirm password.");
            return;
        }
        
        if (!password.isEmpty() && !password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match!");
            return;
        }
        
        if (!isValidEmail(email)) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }
        
        // Check if username or email already exists (for new users or when changing these fields)
        if ((!isEditMode || !username.equals(user.getUsername())) && isUsernameExists(username)) {
            messageLabel.setText("Username already exists. Please choose another one.");
            return;
        }
        
        if ((!isEditMode || !email.equals(user.getEmail())) && isEmailExists(email)) {
            messageLabel.setText("Email already exists. Please use another email address.");
            return;
        }
        
        // Save or update user
        if (isEditMode) {
            updateUser(username, fullname, email, password, role);
        } else {
            createUser(username, fullname, email, password, role);
        }
    }
    
    private boolean isValidEmail(String email) {
        // Simple email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }
    
    private boolean isUsernameExists(String username) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // If we're in edit mode, exclude the current user from the check
                    if (isEditMode && username.equals(user.getUsername())) {
                        return false;
                    }
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database error: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean isEmailExists(String email) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ?")) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // If we're in edit mode, exclude the current user from the check
                    if (isEditMode && email.equals(user.getEmail())) {
                        return false;
                    }
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database error: " + e.getMessage());
        }
        
        return false;
    }
    
    private void createUser(String username, String fullname, String email, String password, String role) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (username, password, role, fullname, email) VALUES (?, ?, ?, ?, ?)")) {
            
            // Hash the password
            String hashedPassword = PasswordHash.hashPassword(password);
            
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, role);
            stmt.setString(4, fullname);
            stmt.setString(5, email);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                showSuccessAlert("User created successfully!");
                parentController.refreshTable();
                closeForm(null);
            } else {
                messageLabel.setText("Failed to create user.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Error creating user: " + e.getMessage());
        }
    }
    
    private void updateUser(String username, String fullname, String email, String password, String role) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt;
            
            if (password.isEmpty()) {
                // Don't update password if not provided
                stmt = conn.prepareStatement(
                        "UPDATE users SET username = ?, role = ?, fullname = ?, email = ? WHERE id = ?");
                stmt.setString(1, username);
                stmt.setString(2, role);
                stmt.setString(3, fullname);
                stmt.setString(4, email);
                stmt.setInt(5, user.getId());
            } else {
                // Update with new password
                stmt = conn.prepareStatement(
                        "UPDATE users SET username = ?, password = ?, role = ?, fullname = ?, email = ? WHERE id = ?");
                stmt.setString(1, username);
                stmt.setString(2, PasswordHash.hashPassword(password));
                stmt.setString(3, role);
                stmt.setString(4, fullname);
                stmt.setString(5, email);
                stmt.setInt(6, user.getId());
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                showSuccessAlert("User updated successfully!");
                parentController.refreshTable();
                closeForm(null);
            } else {
                messageLabel.setText("Failed to update user.");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Error updating user: " + e.getMessage());
        }
    }
    
    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        closeForm(event);
    }
    
    private void closeForm(ActionEvent event) {
        // Get the current stage and close it
        Node source = (Node) (event != null ? event.getSource() : saveButton);
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
