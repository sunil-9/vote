package com.example.vote.util;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;

/**
 * Utility class for handling email operations
 */
public class EmailService {

    // SMTP Email configuration - Replace with your own SMTP details
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USERNAME = "<your email>";
    private static final String SMTP_PASSWORD = "<Your App Password>"; // Use app-specific password
    
    // OTP configuration
    private static final int OTP_LENGTH = 6;
    
    /**
     * Generate a random OTP of specified length
     * 
     * @return A random OTP
     */
    public static String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }
    
    /**
     * Send OTP via email for password reset
     * 
     * @param recipientEmail The email to send the OTP to
     * @param otp The OTP to send
     * @return true if email was sent successfully, false otherwise
     */
    public static boolean sendOTP(String recipientEmail, String otp) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        
        // Create session with authenticator
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
        
        try {
            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Password Reset OTP");
            
            // Create email content
            String emailContent = "Dear User,\n\n"
                    + "Your OTP for password reset is: " + otp + "\n\n"
                    + "This OTP will expire in 10 minutes.\n\n"
                    + "If you didn't request a password reset, please ignore this email.\n\n"
                    + "Regards,\nVoting System Team";
            
            message.setText(emailContent);
            
            // Send message
            Transport.send(message);
            
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Store OTP in database and send via email
     * 
     * @param email The email address
     * @return The generated OTP if successful, null otherwise
     */
    public static String sendAndStoreOTP(String email) {
        try {
            // First check if email exists
            if (!userEmailExists(email)) {
                return null;
            }
            
            // Deactivate any existing OTPs for this email
            deactivateExistingOTPs(email);
            
            // Generate new OTP
            String otp = generateOTP();
            
            // Store OTP in database
            if (storeOTP(email, otp)) {
                // Send OTP via email
                if (sendOTP(email, otp)) {
                    return otp;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Check if user email exists in the database
     * 
     * @param email The email to check
     * @return true if email exists, false otherwise
     */
    private static boolean userEmailExists(String email) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ?")) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Deactivate all existing OTPs for a specific email
     * 
     * @param email The email address
     */
    private static void deactivateExistingOTPs(String email) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE otp SET status = 'inactive' WHERE email = ?")) {
            
            stmt.setString(1, email);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Store OTP in database
     * 
     * @param email The email address
     * @param otp The OTP to store
     * @return true if stored successfully, false otherwise
     */
    private static boolean storeOTP(String email, String otp) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO otp (email, otp_code, status) VALUES (?, ?, 'active')")) {
            
            stmt.setString(1, email);
            stmt.setString(2, otp);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verify OTP and check if it is valid and active
     * 
     * @param email The email address
     * @param otp The OTP to verify
     * @return true if OTP is valid and active, false otherwise
     */
    public static boolean verifyOTP(String email, String otp) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM otp WHERE email = ? AND otp_code = ? AND status = 'active'")) {
            
            stmt.setString(1, email);
            stmt.setString(2, otp);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Update password for a user after OTP verification
     * 
     * @param email The email address
     * @param newPassword The new password (already hashed)
     * @param otp The OTP that was verified
     * @return true if password was updated successfully, false otherwise
     */
    public static boolean updatePassword(String email, String newPassword, String otp) {
        // First verify OTP
        if (!verifyOTP(email, otp)) {
            return false;
        }
        
        // Update password
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE email = ?")) {
            
            stmt.setString(1, newPassword);
            stmt.setString(2, email);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Deactivate the OTP
                deactivateExistingOTPs(email);
                return true;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
}
