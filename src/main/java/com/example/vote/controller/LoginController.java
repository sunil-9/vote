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

public class LoginController {

    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Button signupLinkButton;
    
    @FXML
    private Hyperlink forgotPasswordLink;
    
    @FXML
    private Label messageLabel;
    
    @FXML
    private void handleLogin(ActionEvent event) {
        // Clear previous error message
        messageLabel.setText("");
        
        // Get username and password
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Username and password are required!");
            return;
        }
        
        // Try to authenticate user
        try {
            User user = authenticateUser(username, password);
            
            if (user != null) {
                // Successful login
                if (user.isAdmin()) {
                    // Load admin dashboard
                    loadAdminDashboard(event, user);
                } else {
                    // Load user dashboard
                    loadUserDashboard(event, user);
                }
            } else {
                // Failed login
                messageLabel.setText("Invalid username or password!");
            }
        } catch (SQLException e) {
            messageLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            messageLabel.setText("Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Authenticate a user by checking their credentials
     * @param username The username to check
     * @param password The password to check
     * @return User object if authentication successful, null otherwise
     */
    /**
     * Updates the last_login timestamp for the given user ID
     * @param userId The ID of the user to update
     */
    private void updateLastLoginTimestamp(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Failed to update last login timestamp: " + e.getMessage());
            // Non-critical error, we'll just log it and continue
        }
    }
    
    private User authenticateUser(String username, String password) throws SQLException {
        // First, retrieve the user by username only
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Get the stored hashed password
                    String storedHashedPassword = rs.getString("password");
                    
                    // Verify the password
                    if (PasswordHash.verifyPassword(password, storedHashedPassword)) {
                        // Password is correct, read all user data first before calling other methods
                        int userId = rs.getInt("id");
                        String role = rs.getString("role");
                        String fullname = rs.getString("fullname");
                        String email = rs.getString("email");
                        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                        
                        // Now that we've read everything we need, we can close the ResultSet and update the login timestamp
                        updateLastLoginTimestamp(userId);
                        
                        // Create and return user object with the data we read earlier
                        User user = new User();
                        user.setId(userId);
                        user.setUsername(username);
                        user.setPassword(""); // Don't store the password in memory
                        user.setRole(role);
                        user.setFullname(fullname);
                        user.setEmail(email);
                        user.setCreatedAt(createdAt);
                        
                        return user;
                    }
                }
            }
        }
        
        return null; // Authentication failed
    }
    
    /**
     * Load the admin dashboard screen
     */
    private void loadAdminDashboard(ActionEvent event, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/admin-dashboard.fxml"));
        Parent dashboardParent = loader.load();
        
        // Get the controller and pass the user object
        AdminDashboardController controller = loader.getController();
        controller.initData(user);
        
        Scene dashboardScene = new Scene(dashboardParent);
        
        // Get the current stage
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(dashboardScene);
        window.setTitle("Admin Dashboard");
        window.show();
    }
    
    /**
     * Load the user dashboard screen
     */
    private void loadUserDashboard(ActionEvent event, User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/user-dashboard.fxml"));
        Parent dashboardParent = loader.load();
        
        // Get the controller and pass the user object
        UserDashboardController controller = loader.getController();
        controller.initData(user);
        
        Scene dashboardScene = new Scene(dashboardParent);
        
        // Get the current stage
        Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        window.setScene(dashboardScene);
        window.setTitle("User Dashboard");
        window.show();
    }
    
    @FXML
    private void switchToSignup(ActionEvent event) {
        try {
            Parent signupParent = FXMLLoader.load(getClass().getResource("/com/example/vote/signup-view.fxml"));
            Scene signupScene = new Scene(signupParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(signupScene);
            window.setTitle("Sign Up");
            window.show();
            
        } catch (IOException e) {
            messageLabel.setText("Error loading signup page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void switchToForgotPassword(ActionEvent event) {
        try {
            Parent forgotPasswordParent = FXMLLoader.load(getClass().getResource("/com/example/vote/forgot-password-view.fxml"));
            Scene forgotPasswordScene = new Scene(forgotPasswordParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(forgotPasswordScene);
            window.setTitle("Forgot Password");
            window.show();
            
        } catch (IOException e) {
            messageLabel.setText("Error loading forgot password page: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
