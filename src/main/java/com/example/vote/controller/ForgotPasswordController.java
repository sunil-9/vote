package com.example.vote.controller;

import com.example.vote.util.EmailService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ForgotPasswordController {
    
    @FXML
    private TextField emailField;
    
    @FXML
    private Label messageLabel;
    
    @FXML
    private Button sendOtpButton;
    
    @FXML
    private Button backToLoginButton;
    
    @FXML
    private void handleSendOTP(ActionEvent event) {
        // Clear previous message
        messageLabel.setText("");
        
        // Get email address
        String email = emailField.getText().trim();
        
        // Validate email
        if (email.isEmpty()) {
            messageLabel.setText("Please enter your email address.");
            return;
        }
        
        if (!isValidEmail(email)) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }
        
        // Try to send OTP
        String otp = EmailService.sendAndStoreOTP(email);
        
        if (otp != null) {
            try {
                // Load reset password screen
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/reset-password-view.fxml"));
                Parent resetPasswordParent = loader.load();
                
                // Pass email to reset password controller
                ResetPasswordController controller = loader.getController();
                controller.initData(email);
                
                Scene resetPasswordScene = new Scene(resetPasswordParent);
                
                // Get the current stage
                Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
                window.setScene(resetPasswordScene);
                window.setTitle("Reset Password");
                window.show();
                
            } catch (IOException e) {
                messageLabel.setText("Error loading reset password page: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            messageLabel.setText("Failed to send OTP. Please check if the email is registered.");
        }
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
    
    /**
     * Basic email validation
     * 
     * @param email The email to validate
     * @return true if email is valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        // Simple email validation
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }
}
