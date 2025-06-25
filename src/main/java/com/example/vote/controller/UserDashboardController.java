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
import javafx.scene.control.ListView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class UserDashboardController implements Initializable {

    @FXML
    private Label welcomeLabel;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private Button voteButton;
    
    @FXML
    private Button electionsButton;
    
    @FXML
    private Button profileButton;
    
    @FXML
    private ListView<String> electionsListView;
    
    @FXML
    private Text totalVotesText;
    
    @FXML
    private Text activeElectionsText;
    
    @FXML
    private Label statusLabel;
    
    private User currentUser;
    private Timer refreshTimer;
    private Map<String, Integer> electionMap; // Maps election display names to their IDs
    
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
        updateDashboardData();
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
            // Count user's total votes
            int totalVotes = 0;
            if (currentUser != null) {
                totalVotes = countUserVotes(conn, currentUser.getId());
            }
            totalVotesText.setText(String.valueOf(totalVotes));
            
            // Count active elections
            int activeElections = countActiveElections(conn);
            activeElectionsText.setText(String.valueOf(activeElections));
            
            // Load active elections into list view
            loadActiveElections(conn);
            
            statusLabel.setText("Ready");
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Database error: " + e.getMessage());
        }
    }
    
    /**
     * Count the number of votes cast by a specific user
     */
    private int countUserVotes(Connection conn, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
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
     * Load active elections into the list view
     */
    private void loadActiveElections(Connection conn) throws SQLException {
        electionsListView.getItems().clear();
        
        String sql = "SELECT id, title FROM elections WHERE status = 'active' AND CURRENT_TIMESTAMP BETWEEN start_date AND end_date";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            Map<String, Integer> electionMap = new HashMap<>();
            boolean hasElections = false;
            
            while (rs.next()) {
                hasElections = true;
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String displayText = title;
                
                // Store the election ID mapped to its display text for later retrieval
                electionMap.put(displayText, id);
                electionsListView.getItems().add(displayText);
            }
            
            if (!hasElections) {
                electionsListView.getItems().add("No active elections found");
            }
            
            // Store the election map as a property of the controller
            this.electionMap = electionMap;
        }
    }
    
    /**
     * Check if user has already voted in an election
     */
    private boolean hasUserVotedInElection(Connection conn, int userId, int electionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM votes WHERE user_id = ? AND election_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, electionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    /**
     * Handle vote button click
     */
    @FXML
    private void handleVote(ActionEvent event) {
        String selected = electionsListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("No active elections found")) {
            statusLabel.setText("No election selected");
            return;
        }
        
        // Get the election ID from the map
        Integer electionId = electionMap.get(selected);
        if (electionId == null) {
            statusLabel.setText("Invalid election selection");
            return;
        }
        
        try {
            // Open the voting screen for this election
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/vote-view.fxml"));
            Parent voteViewParent = loader.load();
            
            // Pass election ID and user to the voting controller
            VoteController voteController = loader.getController();
            voteController.initData(currentUser, electionId);
            
            Scene voteScene = new Scene(voteViewParent);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(voteScene);
            window.setTitle("Vote: " + selected);
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading voting screen: " + e.getMessage());
        }
    }
    
    /**
     * Handle elections button click
     */
    @FXML
    private void handleElections(ActionEvent event) {
        try {
            // Navigate to the elections view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/user-elections-view.fxml"));
            Parent electionsViewParent = loader.load();
            
            // Pass the user to the elections controller
            UserElectionsController controller = loader.getController();
            controller.initData(currentUser);
            
            // Set the new scene
            Scene electionsScene = new Scene(electionsViewParent);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(electionsScene);
            window.setTitle("Elections");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error opening elections view: " + e.getMessage());
        }
    }
    

    
    /**
     * Handle profile button click
     */
    @FXML
    private void handleProfile(ActionEvent event) {
        try {
            // Navigate to user profile view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/user-profile-view.fxml"));
            Parent profileViewParent = loader.load();
            
            // Pass the user to the profile controller
            UserProfileController controller = loader.getController();
            controller.initData(currentUser);
            
            Scene profileScene = new Scene(profileViewParent);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(profileScene);
            window.setTitle("My Profile");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error opening profile view: " + e.getMessage());
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
