package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class AdminDashboardController implements Initializable {

    @FXML
    private Label welcomeLabel;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private Button usersButton;
    
    @FXML
    private Button electionButton;
    
    @FXML
    private Button reportButton;
    
    @FXML
    private Button settingsButton;
    
    @FXML
    private Text totalUsersText;
    
    @FXML
    private Text activeElectionsText;
    
    @FXML
    private Text completedElectionsText;
    
    @FXML
    private Text totalVotesText;
    
    @FXML
    private Label lastUpdateLabel;
    
    @FXML
    private ProgressBar systemHealthProgress;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    private Timer refreshTimer;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initially update the dashboard with data
        updateDashboardData();
        
        // Set up a timer to refresh data periodically (every 30 seconds)
        setupRefreshTimer();
    }
    
    /**
     * Initialize data for the controller from login
     * @param user The logged in user
     */
    public void initData(User user) {
        this.currentUser = user;
        welcomeLabel.setText("Welcome, " + user.getFullname());
    }
    
    /**
     * Set up a timer to refresh dashboard data
     */
    private void setupRefreshTimer() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Update UI on the JavaFX Application Thread
                Platform.runLater(() -> updateDashboardData());
            }
        }, 30000, 30000); // Update every 30 seconds
    }
    
    /**
     * Update dashboard data from database
     */
    private void updateDashboardData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Count total users
            int totalUsers = countUsers(conn);
            totalUsersText.setText(String.valueOf(totalUsers));
            
            // Count active elections
            int activeElections = countActiveElections(conn);
            activeElectionsText.setText(String.valueOf(activeElections));
            
            // Count completed elections
            int completedElections = countCompletedElections(conn);
            completedElectionsText.setText(String.valueOf(completedElections));
            
            // Count total votes
            int totalVotes = countTotalVotes(conn);
            totalVotesText.setText(String.valueOf(totalVotes));
            
            // Update last update time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            lastUpdateLabel.setText("Last Updated: " + LocalDateTime.now().format(formatter));
            
            // Set system health based on database connectivity
            systemHealthProgress.setProgress(1.0);
            statusLabel.setText("System Ready");
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Database error: " + e.getMessage());
            
            // If database error, set health indicator to warning
            systemHealthProgress.setProgress(0.5);
            systemHealthProgress.getStyleClass().add("progress-red");
        }
    }
    
    /**
     * Count total number of users in the system
     */
    private int countUsers(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Count active elections in the system
     */
    private int countActiveElections(Connection conn) throws SQLException {
        // An election is active if its status is 'active' and current date is between start_date and end_date
        String sql = "SELECT COUNT(*) FROM elections WHERE status = 'active' AND CURRENT_TIMESTAMP BETWEEN start_date AND end_date";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Count completed elections in the system
     */
    private int countCompletedElections(Connection conn) throws SQLException {
        // An election is completed if its status is 'completed' or current date is past end_date
        String sql = "SELECT COUNT(*) FROM elections WHERE status = 'completed' OR end_date < CURRENT_TIMESTAMP";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Count total votes cast in the system
     */
    private int countTotalVotes(Connection conn) throws SQLException {
        // As of now, we're assuming a votes table exists. If not, this will throw an exception
        // which will be caught in the calling method
        try {
            String sql = "SELECT COUNT(*) FROM votes";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            // If votes table doesn't exist yet, return 0 instead of throwing
            if (e.getMessage().contains("doesn't exist") || e.getMessage().contains("unknown table")) {
                return 0;
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Handle button click to manage users
     */
    @FXML
    private void handleUsers(ActionEvent event) {
        try {
            Parent manageUsersParent = FXMLLoader.load(getClass().getResource("/com/example/vote/manage-users-view.fxml"));
            Scene manageUsersScene = new Scene(manageUsersParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(manageUsersScene);
            window.setTitle("Manage Users");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading user management: " + e.getMessage());
        }
    }
    
    /**
     * Handle button click to manage elections
     */
    @FXML
    private void handleElections(ActionEvent event) {
        try {
            Parent manageElectionsParent = FXMLLoader.load(getClass().getResource("/com/example/vote/manage-elections-view.fxml"));
            Scene manageElectionsScene = new Scene(manageElectionsParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(manageElectionsScene);
            window.setTitle("Manage Elections");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading election management: " + e.getMessage());
        }
    }
    
    /**
     * Handle button click to view reports
     */
    @FXML
    private void handleReports(ActionEvent event) {
        try {
            Parent reportsParent = FXMLLoader.load(getClass().getResource("/com/example/vote/reports-view.fxml"));
            Scene reportsScene = new Scene(reportsParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(reportsScene);
            window.setTitle("Election Reports");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading reports: " + e.getMessage());
        }
    }
    
    /**
     * Handle button click to configure settings
     */
    @FXML
    private void handleSettings(ActionEvent event) {
        try {
            Parent settingsParent = FXMLLoader.load(getClass().getResource("/com/example/vote/settings-view.fxml"));
            Scene settingsScene = new Scene(settingsParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(settingsScene);
            window.setTitle("Application Settings");
            window.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading settings: " + e.getMessage());
        }
    }
    
    /**
     * Handle logout button click
     */
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Clean up resources
            if (refreshTimer != null) {
                refreshTimer.cancel();
            }
            
            // Navigate back to login screen
            Parent loginParent = FXMLLoader.load(getClass().getResource("/com/example/vote/login-view.fxml"));
            Scene loginScene = new Scene(loginParent);
            
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(loginScene);
            window.setTitle("Login");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to login: " + e.getMessage());
        }
    }
}
