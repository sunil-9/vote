package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import com.example.vote.util.PasswordHash;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SignupController {
    
    @FXML
    private TextField fullNameField;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label messageLabel;
    
    @FXML
    private Button signupButton;
    
    @FXML
    private Button loginLinkButton;
    
    @FXML
    private void handleSignup(ActionEvent event) {
        // Clear previous error message
        messageLabel.setText("");
        
        // Validate input fields
        if (fullNameField.getText().trim().isEmpty() || 
            usernameField.getText().trim().isEmpty() ||
            emailField.getText().trim().isEmpty() ||
            passwordField.getText().isEmpty() ||
            confirmPasswordField.getText().isEmpty()) {
            messageLabel.setText("All fields are required!");
            return;
        }
        
        // Check if passwords match
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            messageLabel.setText("Passwords do not match!");
            return;
        }
        
        // Hash the password before storing
        String hashedPassword = PasswordHash.hashPassword(passwordField.getText());
        
        // Create new user object with hashed password
        User user = new User(
            usernameField.getText().trim(),
            hashedPassword,
            "user",  // Default role is 'user'
            fullNameField.getText().trim(),
            emailField.getText().trim()
        );
        
        // Try to register user in database
        if (registerUser(user)) {
            try {
                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registration Successful");
                alert.setHeaderText(null);
                alert.setContentText("Account created successfully! You can now log in.");
                alert.showAndWait();
                
                // Switch to login screen
                switchToLogin(event);
            } catch (Exception e) {
                messageLabel.setText("Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Register a new user in the database
     * @param user The user to register
     * @return true if registration was successful, false otherwise
     */
    private boolean registerUser(User user) {
        // First check if username already exists in a separate connection
        try {
            if (usernameExists(user.getUsername())) {
                messageLabel.setText("Username already exists! Please choose another one.");
                return false;
            }
        } catch (SQLException e) {
            messageLabel.setText("Database error checking username: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        // Now proceed with user registration in a new connection
        String sql = "INSERT INTO users (username, password, role, fullname, email) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Set parameters and execute query
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getFullname());
            stmt.setString(5, user.getEmail());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            messageLabel.setText("Database error during registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if a username already exists in the database
     * @param username The username to check
     * @return true if the username exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    private boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        boolean exists = false;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    exists = rs.getInt(1) > 0;
                }
            }
        }
        
        return exists;
    }
    
    @FXML
    private void switchToLogin(ActionEvent event) {
        try {
            Parent loginParent = FXMLLoader.load(getClass().getResource("/com/example/vote/login-view.fxml"));
            Scene loginScene = new Scene(loginParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.setTitle("Login");
            window.show();
            
        } catch (IOException e) {
            messageLabel.setText("Error loading login page: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
