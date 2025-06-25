package com.example.vote.controller;

import com.example.vote.util.EmailService;
import com.example.vote.util.PasswordHash;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ResetPasswordController {

    @FXML
    private TextField otpField;
    
    @FXML
    private PasswordField newPasswordField;
    
    @FXML
    private PasswordField confirmPasswordField;
    
    @FXML
    private Label messageLabel;
    
    @FXML
    private Button resetPasswordButton;
    
    @FXML
    private Button resendOtpButton;
    
    @FXML
    private Button backToLoginButton;
    
    // User email that was entered in the forgot password screen
    private String userEmail;
    
    /**
     * Initialize data from the forgot password controller
     * 
     * @param email The user's email
     */
    public void initData(String email) {
        this.userEmail = email;
    }
    
    @FXML
    private void handleResetPassword(ActionEvent event) {
        // Clear previous message
        messageLabel.setText("");
        
        // Get input values
        String otp = otpField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate inputs
        if (otp.isEmpty()) {
            messageLabel.setText("Please enter the OTP sent to your email.");
            return;
        }
        
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Please enter and confirm your new password.");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match!");
            return;
        }
        
        // Validate password strength
        if (newPassword.length() < 6) {
            messageLabel.setText("Password must be at least 6 characters long.");
            return;
        }
        
        // Hash the new password
        String hashedPassword = PasswordHash.hashPassword(newPassword);
        
        // Try to update the password
        boolean success = EmailService.updatePassword(userEmail, hashedPassword, otp);
        
        if (success) {
            // Show success alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Your password has been reset successfully!");
            alert.showAndWait();
            
            // Navigate back to login
            switchToLogin(event);
        } else {
            messageLabel.setText("Failed to reset password. Please check that the OTP is correct and try again.");
        }
    }
    
    @FXML
    private void handleResendOTP(ActionEvent event) {
        // Clear previous message
        messageLabel.setText("");
        
        if (userEmail == null || userEmail.isEmpty()) {
            messageLabel.setText("Email address is missing. Please go back and try again.");
            return;
        }
        
        // Try to send a new OTP
        String otp = EmailService.sendAndStoreOTP(userEmail);
        
        if (otp != null) {
            messageLabel.setText("A new OTP has been sent to your email.");
        } else {
            messageLabel.setText("Failed to send OTP. Please try again later.");
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
}
