package com.example.vote.controller;

import com.example.vote.model.Election;
import com.example.vote.model.Candidate;
import com.example.vote.util.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ReportsController implements Initializable {

    @FXML
    private ComboBox<Election> electionComboBox;

    @FXML
    private Button generateReportButton;

    @FXML
    private Button exportPdfButton;

    @FXML
    private Button backButton;

    @FXML
    private Text totalVotesText;

    @FXML
    private Text voterTurnoutText;

    @FXML
    private Text electionStatusText;

    @FXML
    private Label electionTitleLabel;

    @FXML
    private Label electionDescriptionLabel;

    @FXML
    private Label electionDatesLabel;

    @FXML
    private Label electionCreatedByLabel;

    @FXML
    private PieChart voteDistributionPieChart;

    @FXML
    private BarChart<String, Number> candidateVotesBarChart;

    @FXML
    private CategoryAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private TableView<CandidateResult> resultsTableView;

    @FXML
    private TableColumn<CandidateResult, Integer> rankColumn;

    @FXML
    private TableColumn<CandidateResult, String> candidateNameColumn;

    @FXML
    private TableColumn<CandidateResult, String> candidatePositionColumn;

    @FXML
    private TableColumn<CandidateResult, Integer> votesColumn;

    @FXML
    private TableColumn<CandidateResult, String> percentageColumn;

    @FXML
    private TextArea demographicsNote;

    @FXML
    private Label statusLabel;

    private ObservableList<Election> elections;
    private ObservableList<CandidateResult> candidateResults;
    private int totalUsersCount;
    
    // Custom class to hold candidate result data
    public static class CandidateResult {
        private final Integer rank;
        private final String name;
        private final String position;
        private final Integer votes;
        private final String percentage;

        public CandidateResult(Integer rank, String name, String position, Integer votes, String percentage) {
            this.rank = rank;
            this.name = name;
            this.position = position;
            this.votes = votes;
            this.percentage = percentage;
        }

        public Integer getRank() { return rank; }
        public String getName() { return name; }
        public String getPosition() { return position; }
        public Integer getVotes() { return votes; }
        public String getPercentage() { return percentage; }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set up table columns
        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        candidateNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        candidatePositionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        votesColumn.setCellValueFactory(new PropertyValueFactory<>("votes"));
        percentageColumn.setCellValueFactory(new PropertyValueFactory<>("percentage"));

        // Load elections into combo box
        loadElections();
        
        // Count total users for potential turnout calculation
        countTotalUsers();
        
        // Set export button disabled initially
        exportPdfButton.setDisable(true);
        
        // Set up election selection handler
        electionComboBox.setOnAction(event -> {
            if (electionComboBox.getValue() != null) {
                generateReportButton.setDisable(false);
            }
        });
    }

    private void loadElections() {
        elections = FXCollections.observableArrayList();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, title, description, start_date, end_date, status, created_by FROM elections")) {
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Election election = new Election();
                election.setId(rs.getInt("id"));
                election.setTitle(rs.getString("title"));
                election.setDescription(rs.getString("description"));
                election.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                election.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                election.setStatus(rs.getString("status"));
                election.setCreatedBy(rs.getInt("created_by"));
                
                elections.add(election);
            }
            
            electionComboBox.setItems(elections);
            
            // If there are no elections, disable the generate button
            if (elections.isEmpty()) {
                generateReportButton.setDisable(true);
                statusLabel.setText("No elections available");
            } else {
                generateReportButton.setDisable(false);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading elections: " + e.getMessage());
        }
    }
    
    private void countTotalUsers() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users")) {
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                totalUsersCount = rs.getInt(1);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error counting users: " + e.getMessage());
        }
    }

    @FXML
    private void handleGenerateReport(ActionEvent event) {
        Election selectedElection = electionComboBox.getValue();
        
        if (selectedElection == null) {
            statusLabel.setText("Please select an election first");
            return;
        }
        
        try {
            // Load election details
            loadElectionDetails(selectedElection);
            
            // Load candidate results
            loadCandidateResults(selectedElection);
            
            // Generate charts
            generateCharts(selectedElection);
            
            // Enable export button
            exportPdfButton.setDisable(false);
            
            statusLabel.setText("Report generated successfully");
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error generating report: " + e.getMessage());
        }
    }
    
    private void loadElectionDetails(Election election) throws SQLException {
        // Set election information
        electionTitleLabel.setText(election.getTitle());
        electionDescriptionLabel.setText(election.getDescription());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String dateRange = election.getStartDate().format(formatter) + " to " + 
                          election.getEndDate().format(formatter);
        electionDatesLabel.setText(dateRange);
        
        electionStatusText.setText(election.getStatus());
        
        // Get created by user name
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT fullname FROM users WHERE id = ?")) {
            
            stmt.setInt(1, election.getCreatedBy());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                electionCreatedByLabel.setText(rs.getString("fullname"));
            } else {
                electionCreatedByLabel.setText("Unknown");
            }
        }
    }
    
    private void loadCandidateResults(Election election) throws SQLException {
        candidateResults = FXCollections.observableArrayList();
        
        int totalVotes = 0;
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT c.id, c.name, c.position, c.votes " +
                     "FROM candidates c " +
                     "WHERE c.election_id = ? " +
                     "ORDER BY c.votes DESC")) {
            
            stmt.setInt(1, election.getId());
            ResultSet rs = stmt.executeQuery();
            
            int rank = 1;
            while (rs.next()) {
                String name = rs.getString("name");
                String position = rs.getString("position");
                int votes = rs.getInt("votes");
                
                totalVotes += votes;
                
                candidateResults.add(new CandidateResult(rank, name, position, votes, "0%"));
                rank++;
            }
            
            // Calculate percentages
            if (totalVotes > 0) {
                for (CandidateResult result : candidateResults) {
                    double percentage = (double) result.getVotes() / totalVotes * 100;
                    String percentageStr = String.format("%.1f%%", percentage);
                    
                    // Update the percentage (we're creating a new object because the fields are final)
                    int index = candidateResults.indexOf(result);
                    candidateResults.set(index, new CandidateResult(
                            result.getRank(), 
                            result.getName(),
                            result.getPosition(),
                            result.getVotes(),
                            percentageStr
                    ));
                }
            }
            
            // Update table
            resultsTableView.setItems(candidateResults);
            
            // Update summary stats
            totalVotesText.setText(String.valueOf(totalVotes));
            
            // Calculate turnout
            if (totalUsersCount > 0) {
                double turnoutPercentage = 0;
                
                // Count distinct voters for this election
                try (PreparedStatement votersStmt = conn.prepareStatement(
                        "SELECT COUNT(DISTINCT user_id) FROM votes WHERE election_id = ?")) {
                    
                    votersStmt.setInt(1, election.getId());
                    ResultSet votersRs = votersStmt.executeQuery();
                    
                    if (votersRs.next()) {
                        int distinctVoters = votersRs.getInt(1);
                        turnoutPercentage = (double) distinctVoters / totalUsersCount * 100;
                    }
                }
                
                voterTurnoutText.setText(String.format("%.1f%%", turnoutPercentage));
            } else {
                voterTurnoutText.setText("N/A");
            }
            
        }
    }
    
    private void generateCharts(Election election) {
        // Clear previous chart data
        voteDistributionPieChart.getData().clear();
        candidateVotesBarChart.getData().clear();
        
        if (candidateResults.isEmpty()) {
            return;
        }
        
        // Generate pie chart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (CandidateResult result : candidateResults) {
            pieChartData.add(new PieChart.Data(result.getName(), result.getVotes()));
        }
        voteDistributionPieChart.setData(pieChartData);
        voteDistributionPieChart.setTitle("Vote Distribution");
        
        // Generate bar chart
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Votes");
        
        for (CandidateResult result : candidateResults) {
            series.getData().add(new XYChart.Data<>(result.getName(), result.getVotes()));
        }
        
        candidateVotesBarChart.getData().add(series);
    }

    @FXML
    private void handleExportPdf(ActionEvent event) {
        // This would implement PDF export functionality
        // For now, just show a message
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export to PDF");
        alert.setHeaderText("Export Feature");
        alert.setContentText("PDF export functionality will be implemented in a future version.");
        alert.showAndWait();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent adminDashboardParent = FXMLLoader.load(getClass().getResource("/com/example/vote/admin-dashboard.fxml"));
            Scene adminDashboardScene = new Scene(adminDashboardParent);
            
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(adminDashboardScene);
            window.setTitle("Admin Dashboard");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to dashboard: " + e.getMessage());
        }
    }
}
