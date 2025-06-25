package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import com.example.vote.util.PasswordHash;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField roleField;

    @FXML
    private TextField lastLoginField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TableView<VoteHistory> votingHistoryTable;

    @FXML
    private TableColumn<VoteHistory, String> electionColumn;

    @FXML
    private TableColumn<VoteHistory, String> candidateColumn;

    @FXML
    private TableColumn<VoteHistory, String> dateColumn;

    @FXML
    private TableColumn<VoteHistory, String> statusColumn;

    @FXML
    private Label votingHistoryLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button saveInfoButton;

    @FXML
    private Button changePasswordButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    private User currentUser;
    private boolean infoChanged = false;

    /**
     * Inner class to represent a voting history record
     */
    public static class VoteHistory {
        private final SimpleStringProperty election;
        private final SimpleStringProperty candidate;
        private final SimpleStringProperty date;
        private final SimpleStringProperty status;

        public VoteHistory(String election, String candidate, String date, String status) {
            this.election = new SimpleStringProperty(election);
            this.candidate = new SimpleStringProperty(candidate);
            this.date = new SimpleStringProperty(date);
            this.status = new SimpleStringProperty(status);
        }

        public String getElection() {
            return election.get();
        }

        public String getCandidate() {
            return candidate.get();
        }

        public String getDate() {
            return date.get();
        }

        public String getStatus() {
            return status.get();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Configure table columns
        electionColumn.setCellValueFactory(data -> data.getValue().election);
        candidateColumn.setCellValueFactory(data -> data.getValue().candidate);
        dateColumn.setCellValueFactory(data -> data.getValue().date);
        statusColumn.setCellValueFactory(data -> data.getValue().status);

        // Add listeners to detect changes in editable fields
        fullNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            infoChanged = true;
        });

        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            infoChanged = true;
        });
    }

    /**
     * Initialize data for the controller
     * @param user The current user
     */
    public void initData(User user) {
        this.currentUser = user;
        loadUserData();
        loadVotingHistory();

        // Reset changed flag after loading data
        infoChanged = false;
    }

    /**
     * Load user data from database
     */
    private void loadUserData() {
        if (currentUser == null) {
            statusLabel.setText("Error: User not found");
            return;
        }

        usernameField.setText(currentUser.getUsername());
        fullNameField.setText(currentUser.getFullname());
        emailField.setText(currentUser.getEmail());
        roleField.setText(currentUser.getRole());

        // Get last login time from database
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT last_login FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUser.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getTimestamp("last_login") != null) {
                        Timestamp lastLogin = rs.getTimestamp("last_login");
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        lastLoginField.setText(lastLogin.toLocalDateTime().format(formatter));
                    } else {
                        lastLoginField.setText("Never");
                    }
                }
            }
            statusLabel.setText("User data loaded successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading user data: " + e.getMessage());
        }
    }

    /**
     * Load voting history from database
     */
    private void loadVotingHistory() {
        ObservableList<VoteHistory> historyList = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT e.title, c.name, v.voted_at, e.status " +
                         "FROM votes v " +
                         "JOIN elections e ON v.election_id = e.id " +
                         "JOIN candidates c ON v.candidate_id = c.id " +
                         "WHERE v.user_id = ? " +
                         "ORDER BY v.voted_at DESC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUser.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String election = rs.getString("title");
                        String candidate = rs.getString("name");
                        
                        // Format date
                        Timestamp timestamp = rs.getTimestamp("voted_at");
                        String date = timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                        
                        String status = rs.getString("status");
                        
                        historyList.add(new VoteHistory(election, candidate, date, status));
                    }
                }
            }

            // Update table and label
            votingHistoryTable.setItems(historyList);
            votingHistoryLabel.setText("Total Elections Voted: " + historyList.size());
            
            statusLabel.setText("Voting history loaded successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading voting history: " + e.getMessage());
        }
    }

    /**
     * Save user information changes
     */
    @FXML
    private void handleSaveInfo(ActionEvent event) {
        if (!infoChanged) {
            statusLabel.setText("No changes to save");
            return;
        }

        String fullName = fullNameField.getText();
        String email = emailField.getText();

        // Validate inputs
        if (fullName.isEmpty() || email.isEmpty()) {
            statusLabel.setText("Full name and email cannot be empty");
            return;
        }

        // Email validation
        if (!email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) {
            statusLabel.setText("Please enter a valid email address");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Update user in database
            String sql = "UPDATE users SET fullname = ?, email = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fullName);
                stmt.setString(2, email);
                stmt.setInt(3, currentUser.getId());
                int rowsAffected = stmt.executeUpdate();

                if (rowsAffected > 0) {
                    // Update current user object
                    currentUser.setFullname(fullName);
                    currentUser.setEmail(email);
                    
                    statusLabel.setText("Profile updated successfully");
                    infoChanged = false;
                } else {
                    statusLabel.setText("Failed to update profile");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error updating profile: " + e.getMessage());
        }
    }

    /**
     * Change user password
     */
    @FXML
    private void handleChangePassword(ActionEvent event) {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validate inputs
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            statusLabel.setText("All password fields are required");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            statusLabel.setText("New passwords do not match");
            return;
        }

        if (newPassword.length() < 6) {
            statusLabel.setText("New password must be at least 6 characters long");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Verify current password
            String sql = "SELECT password FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUser.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");
                        
                        if (!PasswordHash.verifyPassword(currentPassword, hashedPassword)) {
                            statusLabel.setText("Current password is incorrect");
                            return;
                        }
                    } else {
                        statusLabel.setText("User not found");
                        return;
                    }
                }
            }

            // Update password
            String newHashedPassword = PasswordHash.hashPassword(newPassword);
            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newHashedPassword);
                updateStmt.setInt(2, currentUser.getId());
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected > 0) {
                    statusLabel.setText("Password changed successfully");
                    
                    // Clear password fields
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                } else {
                    statusLabel.setText("Failed to change password");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error changing password: " + e.getMessage());
        }
    }

    /**
     * Refresh profile data
     */
    @FXML
    private void handleRefresh(ActionEvent event) {
        loadUserData();
        loadVotingHistory();
        infoChanged = false;
    }

    /**
     * Navigate back to user dashboard
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            // Check if there are unsaved changes
            if (infoChanged) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Unsaved Changes");
                alert.setHeaderText("You have unsaved changes");
                alert.setContentText("Do you want to discard your changes?");

                if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                    return;
                }
            }

            // Navigate to user dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/user-dashboard.fxml"));
            Parent dashboardParent = loader.load();
            
            UserDashboardController controller = loader.getController();
            controller.initData(currentUser);
            
            Scene dashboardScene = new Scene(dashboardParent);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(dashboardScene);
            window.setTitle("User Dashboard");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to dashboard: " + e.getMessage());
        }
    }
}
