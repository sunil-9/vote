package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import javafx.beans.property.SimpleDoubleProperty;
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
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

public class ElectionResultsController implements Initializable {

    @FXML
    private Label electionTitleLabel;

    @FXML
    private TableView<CandidateResult> candidatesTableView;

    @FXML
    private TableColumn<CandidateResult, Integer> rankColumn;

    @FXML
    private TableColumn<CandidateResult, String> nameColumn;

    @FXML
    private TableColumn<CandidateResult, Integer> voteCountColumn;

    @FXML
    private TableColumn<CandidateResult, String> percentageColumn;

    @FXML
    private PieChart votesPieChart;

    @FXML
    private Label totalVotesLabel;

    @FXML
    private Label turnoutLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Button refreshButton;

    @FXML
    private Button backButton;

    private User currentUser;
    private int electionId;
    private String electionTitle;
    private String returnView = "/com/example/vote/user-elections-view.fxml";
    private ObservableList<CandidateResult> candidateResults = FXCollections.observableArrayList();
    private int totalVotes = 0;
    private int totalEligibleVoters = 0;

    /**
     * Inner class to represent candidate results
     */
    public static class CandidateResult {
        private final SimpleIntegerProperty rank;
        private final SimpleIntegerProperty candidateId;
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty voteCount;
        private final SimpleDoubleProperty percentage;

        public CandidateResult(int rank, int candidateId, String name, int voteCount, double percentage) {
            this.rank = new SimpleIntegerProperty(rank);
            this.candidateId = new SimpleIntegerProperty(candidateId);
            this.name = new SimpleStringProperty(name);
            this.voteCount = new SimpleIntegerProperty(voteCount);
            this.percentage = new SimpleDoubleProperty(percentage);
        }

        public int getRank() {
            return rank.get();
        }

        public int getCandidateId() {
            return candidateId.get();
        }

        public String getName() {
            return name.get();
        }

        public int getVoteCount() {
            return voteCount.get();
        }

        public double getPercentage() {
            return percentage.get();
        }

        public String getPercentageString() {
            DecimalFormat df = new DecimalFormat("0.0%");
            return df.format(percentage.get() / 100);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Configure table columns
        rankColumn.setCellValueFactory(data -> data.getValue().rank.asObject());
        nameColumn.setCellValueFactory(data -> data.getValue().name);
        voteCountColumn.setCellValueFactory(data -> data.getValue().voteCount.asObject());
        percentageColumn.setCellValueFactory(data -> {
            DecimalFormat df = new DecimalFormat("0.0%");
            return new SimpleStringProperty(df.format(data.getValue().getPercentage() / 100));
        });
    }

    /**
     * Initialize data for the controller
     * @param user The current user
     * @param electionId The ID of the election to show results for
     */
    public void initData(User user, int electionId) {
        this.currentUser = user;
        this.electionId = electionId;
        
        // Load election and results data
        loadElectionData();
    }
    
    /**
     * Set the return view (can be used when coming from different screens)
     */
    public void setReturnView(String returnView) {
        this.returnView = returnView;
    }

    /**
     * Load election data and results
     */
    private void loadElectionData() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Get election details
            String electionSql = "SELECT title, status, start_date, end_date FROM elections WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(electionSql)) {
                stmt.setInt(1, electionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        electionTitle = rs.getString("title");
                        String status = rs.getString("status");
                        electionTitleLabel.setText(electionTitle);
                        statusLabel.setText(status);
                    } else {
                        statusLabel.setText("Election not found");
                        return;
                    }
                }
            }
            
            // Get candidate results
            loadCandidateResults(conn);
            
            // Calculate turnout
            calculateTurnout(conn);
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading election data: " + e.getMessage());
        }
    }

    /**
     * Load candidate results from the database
     */
    private void loadCandidateResults(Connection conn) throws SQLException {
        candidateResults.clear();
        
        // Query to get candidates with their vote counts, ordered by votes DESC
        String sql = "SELECT c.id, c.name, COALESCE(COUNT(v.id), 0) AS vote_count " +
                     "FROM candidates c " +
                     "LEFT JOIN votes v ON c.id = v.candidate_id " +
                     "WHERE c.election_id = ? " +
                     "GROUP BY c.id " +
                     "ORDER BY vote_count DESC";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, electionId);
            try (ResultSet rs = stmt.executeQuery()) {
                // First, sum up total votes
                totalVotes = 0;
                
                // Create a temporary list to store results before calculating percentages
                ObservableList<CandidateResult> tempResults = FXCollections.observableArrayList();
                
                int rank = 1;
                while (rs.next()) {
                    int candidateId = rs.getInt("id");
                    String name = rs.getString("name");
                    int voteCount = rs.getInt("vote_count");
                    
                    // Accumulate total votes
                    totalVotes += voteCount;
                    
                    // Store the result (without percentage for now)
                    tempResults.add(new CandidateResult(rank++, candidateId, name, voteCount, 0));
                }
                
                // Update total votes label
                totalVotesLabel.setText(String.valueOf(totalVotes));
                
                // Now calculate percentages and add to actual results list
                ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
                
                for (CandidateResult result : tempResults) {
                    // Calculate percentage
                    double percentage = totalVotes == 0 ? 0 : (result.getVoteCount() * 100.0 / totalVotes);
                    
                    // Add to results with percentage
                    candidateResults.add(new CandidateResult(
                            result.getRank(),
                            result.getCandidateId(),
                            result.getName(),
                            result.getVoteCount(),
                            percentage
                    ));
                    
                    // Add to pie chart data if votes > 0
                    if (result.getVoteCount() > 0) {
                        pieChartData.add(new PieChart.Data(result.getName() + " (" + result.getVoteCount() + ")", result.getVoteCount()));
                    }
                }
                
                // Update table view
                candidatesTableView.setItems(candidateResults);
                
                // Update pie chart
                votesPieChart.setData(pieChartData);
                votesPieChart.setTitle("Vote Distribution");
                
                // Add visible labels to pie chart slices
                pieChartData.forEach(data -> {
                    data.nameProperty().bind(
                            new SimpleStringProperty(data.getName() + " (" + 
                            new DecimalFormat("0.0%").format(data.getPieValue() / totalVotes) + ")")
                    );
                });
            }
        }
    }

    /**
     * Calculate voter turnout
     */
    private void calculateTurnout(Connection conn) throws SQLException {
        // Get total eligible voters (all users with role 'user')
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'user'";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                totalEligibleVoters = rs.getInt(1);
            }
        }
        
        // Calculate turnout percentage
        double turnout = totalEligibleVoters == 0 ? 0 : (totalVotes * 100.0 / totalEligibleVoters);
        DecimalFormat df = new DecimalFormat("0.0%");
        turnoutLabel.setText(df.format(turnout / 100));
    }

    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        loadElectionData();
    }

    /**
     * Handle back button click
     */
    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(returnView));
            Parent parent = loader.load();
            
            if (returnView.endsWith("user-elections-view.fxml")) {
                UserElectionsController controller = loader.getController();
                controller.initData(currentUser);
            } else if (returnView.endsWith("user-dashboard.fxml")) {
                UserDashboardController controller = loader.getController();
                controller.initData(currentUser);
            }
            
            Scene scene = new Scene(parent);
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(scene);
            window.setTitle(returnView.contains("dashboard") ? "User Dashboard" : "Elections");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to previous view: " + e.getMessage());
        }
    }
}
