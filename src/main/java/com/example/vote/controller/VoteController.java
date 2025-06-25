package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import javafx.beans.property.SimpleIntegerProperty;
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
import java.util.ResourceBundle;

public class VoteController implements Initializable {

    @FXML
    private Label electionTitleLabel;

    @FXML
    private Label alreadyVotedLabel;

    @FXML
    private TableView<Candidate> candidatesTableView;

    @FXML
    private TableColumn<Candidate, String> nameColumn;

    @FXML
    private TableColumn<Candidate, String> positionColumn;

    @FXML
    private TableColumn<Candidate, String> infoColumn;

    @FXML
    private Button submitButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label statusLabel;

    private User currentUser;
    private int electionId;
    private String electionTitle;
    private boolean hasVoted = false;

    /**
     * Candidate class to represent election candidates in the TableView
     */
    public static class Candidate {
        private final SimpleIntegerProperty id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty position;
        private final SimpleStringProperty info;

        public Candidate(int id, String name, String position, String info) {
            this.id = new SimpleIntegerProperty(id);
            this.name = new SimpleStringProperty(name);
            this.position = new SimpleStringProperty(position);
            this.info = new SimpleStringProperty(info);
        }

        public int getId() {
            return id.get();
        }

        public String getName() {
            return name.get();
        }

        public String getPosition() {
            return position.get();
        }

        public String getInfo() {
            return info.get();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Configure the table columns
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().name);
        positionColumn.setCellValueFactory(cellData -> cellData.getValue().position);
        infoColumn.setCellValueFactory(cellData -> cellData.getValue().info);
        
        // Add selection listener to update status label when a candidate is selected
        candidatesTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                statusLabel.setText("Selected: " + newSelection.getName());
            }
        });
    }

    /**
     * Initialize data for the controller
     * @param user The current user
     * @param electionId The ID of the election to vote in
     */
    public void initData(User user, int electionId) {
        this.currentUser = user;
        this.electionId = electionId;
        
        // Load election data and candidate list
        loadElectionData();
        
        // Check if user has already voted in this election
        checkIfUserHasVoted();
    }

    /**
     * Load election details and candidate list
     */
    private void loadElectionData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get election details
            String electionSql = "SELECT title FROM elections WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(electionSql)) {
                stmt.setInt(1, electionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        electionTitle = rs.getString("title");
                        electionTitleLabel.setText(electionTitle);
                    }
                }
            }
            
            // Get candidates for this election
            String candidatesSql = "SELECT id, name, position, profile FROM candidates WHERE election_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(candidatesSql)) {
                stmt.setInt(1, electionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    ObservableList<Candidate> candidates = FXCollections.observableArrayList();
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        String position = rs.getString("position") != null ? rs.getString("position") : "";
                        String profile = rs.getString("profile") != null ? rs.getString("profile") : "";
                        
                        candidates.add(new Candidate(id, name, position, profile));
                    }
                    candidatesTableView.setItems(candidates);
                    
                    if (candidates.isEmpty()) {
                        statusLabel.setText("No candidates found for this election");
                        submitButton.setDisable(true);
                    }
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading election data: " + e.getMessage());
        }
    }

    /**
     * Check if the current user has already voted in this election
     */
    private void checkIfUserHasVoted() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM votes WHERE user_id = ? AND election_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentUser.getId());
                stmt.setInt(2, electionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        hasVoted = true;
                        alreadyVotedLabel.setVisible(true);
                        submitButton.setDisable(true);
                        statusLabel.setText("You have already cast a vote in this election");
                        
                        // Try to show who they voted for
                        showPreviousVote(conn);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error checking voting status: " + e.getMessage());
        }
    }
    
    /**
     * Show which candidate the user previously voted for
     */
    private void showPreviousVote(Connection conn) throws SQLException {
        String sql = "SELECT c.name FROM votes v JOIN candidates c ON v.candidate_id = c.id " +
                     "WHERE v.user_id = ? AND v.election_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUser.getId());
            stmt.setInt(2, electionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String candidateName = rs.getString("name");
                    statusLabel.setText("You previously voted for: " + candidateName);
                }
            }
        }
    }

    /**
     * Handle submit vote button click
     */
    @FXML
    private void handleSubmitVote(ActionEvent event) {
        Candidate selectedCandidate = candidatesTableView.getSelectionModel().getSelectedItem();
        
        if (selectedCandidate == null) {
            statusLabel.setText("Please select a candidate first");
            return;
        }
        
        // Confirm vote
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Vote");
        confirmDialog.setHeaderText("Vote for " + selectedCandidate.getName());
        confirmDialog.setContentText("Are you sure you want to vote for " + selectedCandidate.getName() + 
                                     "? This action cannot be undone.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // User confirmed, record the vote
                recordVote(selectedCandidate.getId());
            }
        });
    }
    
    /**
     * Record the user's vote in the database
     */
    private void recordVote(int candidateId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Start a transaction to ensure both operations complete or neither does
            conn.setAutoCommit(false);
            
            try {
                // Insert vote record
                String voteSql = "INSERT INTO votes (election_id, user_id, candidate_id) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(voteSql)) {
                    stmt.setInt(1, electionId);
                    stmt.setInt(2, currentUser.getId());
                    stmt.setInt(3, candidateId);
                    stmt.executeUpdate();
                }
                
                // Update candidate vote count
                String updateCountSql = "UPDATE candidates SET votes = votes + 1 WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateCountSql)) {
                    stmt.setInt(1, candidateId);
                    stmt.executeUpdate();
                }
                
                // Commit the transaction
                conn.commit();
                
                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Vote Recorded");
                successAlert.setHeaderText("Thank you for voting!");
                successAlert.setContentText("Your vote has been successfully recorded.");
                successAlert.showAndWait();
                
                // Return to user dashboard
                returnToDashboard(null);
                
            } catch (SQLException e) {
                // If there's an error, roll back the transaction
                conn.rollback();
                
                // Check if the error is due to unique constraint violation
                if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("unique_vote")) {
                    statusLabel.setText("You have already voted in this election");
                    alreadyVotedLabel.setVisible(true);
                    submitButton.setDisable(true);
                } else {
                    throw e;
                }
            } finally {
                // Reset auto-commit to true
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error recording vote: " + e.getMessage());
        }
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        try {
            returnToDashboard(event);
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Return to user dashboard
     */
    private void returnToDashboard(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/user-dashboard.fxml"));
        Parent dashboardParent = loader.load();
        
        // Pass the user to the dashboard controller
        UserDashboardController controller = loader.getController();
        controller.initData(currentUser);
        
        Scene dashboardScene = new Scene(dashboardParent);
        
        // Get the window from the event or use the current stage
        Stage window;
        if (event != null) {
            window = (Stage) ((Node) event.getSource()).getScene().getWindow();
        } else {
            window = (Stage) submitButton.getScene().getWindow();
        }
        
        window.setScene(dashboardScene);
        window.setTitle("User Dashboard");
        window.show();
    }
}
