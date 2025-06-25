package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class UserElectionsController implements Initializable {

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private TableView<Election> electionsTableView;

    @FXML
    private TableColumn<Election, String> titleColumn;

    @FXML
    private TableColumn<Election, Date> startDateColumn;

    @FXML
    private TableColumn<Election, Date> endDateColumn;

    @FXML
    private TableColumn<Election, String> statusColumn;

    @FXML
    private TableColumn<Election, Void> actionColumn;

    @FXML
    private Button viewDetailsButton;

    @FXML
    private Label statusLabel;

    private User currentUser;
    private ObservableList<Election> electionsList = FXCollections.observableArrayList();

    /**
     * Election class to represent elections in the TableView
     */
    public static class Election {
        private final int id;
        private final String title;
        private final String description;
        private final Date startDate;
        private final Date endDate;
        private final String status;
        private final boolean hasVoted;

        public Election(int id, String title, String description, Date startDate, Date endDate, String status, boolean hasVoted) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.hasVoted = hasVoted;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Date getStartDate() {
            return startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public String getStatus() {
            return status;
        }

        public boolean isHasVoted() {
            return hasVoted;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup filter combo box
        filterComboBox.getItems().addAll("All Elections", "Active", "Upcoming", "Completed", "Voted", "Not Voted");
        filterComboBox.setValue("All Elections");
        filterComboBox.setOnAction(event -> loadElections());

        // Configure the table columns
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        statusColumn.setCellValueFactory(data -> {
            Election election = data.getValue();
            String displayStatus = election.getStatus();
            
            if (election.isHasVoted()) {
                displayStatus += " (Voted)";
            }
            
            return new SimpleStringProperty(displayStatus);
        });

        // Setup action column with vote button
        setupActionColumn();

        // Add selection listener to enable/disable the view details button
        electionsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            viewDetailsButton.setDisable(newSelection == null);
        });
    }

    /**
     * Setup the action column with vote buttons
     */
    private void setupActionColumn() {
        Callback<TableColumn<Election, Void>, TableCell<Election, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Election, Void> call(final TableColumn<Election, Void> param) {
                return new TableCell<>() {
                    private final Button voteButton = new Button("Vote");
                    private final Button resultsButton = new Button("Results");

                    {
                        voteButton.setOnAction((ActionEvent event) -> {
                            Election election = getTableView().getItems().get(getIndex());
                            openVotingScreen(election);
                        });
                        
                        resultsButton.setOnAction((ActionEvent event) -> {
                            Election election = getTableView().getItems().get(getIndex());
                            openResultsScreen(election);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            Election election = getTableView().getItems().get(getIndex());
                            
                            // Show appropriate button based on election status and voting state
                            if ("active".equalsIgnoreCase(election.getStatus())) {
                                if (election.isHasVoted()) {
                                    setGraphic(resultsButton);
                                } else {
                                    setGraphic(voteButton);
                                }
                            } else if ("completed".equalsIgnoreCase(election.getStatus())) {
                                setGraphic(resultsButton);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            }
        };

        actionColumn.setCellFactory(cellFactory);
    }

    /**
     * Initialize data for the controller
     * @param user The current user
     */
    public void initData(User user) {
        this.currentUser = user;
        loadElections();
    }

    /**
     * Load elections based on the selected filter
     */
    private void loadElections() {
        electionsList.clear();
        
        try (Connection conn = DatabaseConnection.getConnection()) {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT e.id, e.title, e.description, e.start_date, e.end_date, e.status, ");
            sql.append("(SELECT COUNT(*) FROM votes v WHERE v.election_id = e.id AND v.user_id = ?) > 0 AS has_voted ");
            sql.append("FROM elections e WHERE 1=1 ");

            // Apply filter
            String filter = filterComboBox.getValue();
            if ("Active".equals(filter)) {
                sql.append("AND e.status = 'active' AND CURRENT_TIMESTAMP BETWEEN e.start_date AND e.end_date ");
            } else if ("Upcoming".equals(filter)) {
                sql.append("AND (e.status = 'active' OR e.status = 'pending') AND CURRENT_TIMESTAMP < e.start_date ");
            } else if ("Completed".equals(filter)) {
                sql.append("AND (e.status = 'completed' OR CURRENT_TIMESTAMP > e.end_date) ");
            } else if ("Voted".equals(filter)) {
                sql.append("AND EXISTS (SELECT 1 FROM votes v WHERE v.election_id = e.id AND v.user_id = ?) ");
            } else if ("Not Voted".equals(filter)) {
                sql.append("AND e.status = 'active' AND CURRENT_TIMESTAMP BETWEEN e.start_date AND e.end_date ");
                sql.append("AND NOT EXISTS (SELECT 1 FROM votes v WHERE v.election_id = e.id AND v.user_id = ?) ");
            }
            
            sql.append("ORDER BY e.start_date DESC");
            
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                stmt.setInt(1, currentUser.getId());
                
                // Set additional parameters for specific filters
                if ("Voted".equals(filter)) {
                    stmt.setInt(2, currentUser.getId());
                } else if ("Not Voted".equals(filter)) {
                    stmt.setInt(2, currentUser.getId());
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String title = rs.getString("title");
                        String description = rs.getString("description");
                        Date startDate = rs.getDate("start_date");
                        Date endDate = rs.getDate("end_date");
                        String status = rs.getString("status");
                        boolean hasVoted = rs.getBoolean("has_voted");
                        
                        Election election = new Election(id, title, description, startDate, endDate, status, hasVoted);
                        electionsList.add(election);
                    }
                }
            }
            
            electionsTableView.setItems(electionsList);
            
            // Update status label
            statusLabel.setText(electionsList.size() + " elections found");
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading elections: " + e.getMessage());
        }
    }

    /**
     * Open the voting screen for an election
     */
    private void openVotingScreen(Election election) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/vote-view.fxml"));
            Parent voteViewParent = loader.load();
            
            // Pass election ID and user to the voting controller
            VoteController voteController = loader.getController();
            voteController.initData(currentUser, election.getId());
            
            Scene voteScene = new Scene(voteViewParent);
            Stage window = (Stage) filterComboBox.getScene().getWindow();
            window.setScene(voteScene);
            window.setTitle("Vote: " + election.getTitle());
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading voting screen: " + e.getMessage());
        }
    }

    /**
     * Open the results screen for an election
     */
    private void openResultsScreen(Election election) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/election-results-view.fxml"));
            Parent resultsViewParent = loader.load();
            
            // Pass election ID and user to the results controller
            ElectionResultsController resultsController = loader.getController();
            resultsController.initData(currentUser, election.getId());
            
            Scene resultsScene = new Scene(resultsViewParent);
            Stage window = (Stage) filterComboBox.getScene().getWindow();
            window.setScene(resultsScene);
            window.setTitle("Results: " + election.getTitle());
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading results screen: " + e.getMessage());
        }
    }

    /**
     * Handle refresh button click
     */
    @FXML
    private void handleRefresh() {
        loadElections();
    }

    /**
     * Handle view details button click
     */
    @FXML
    private void handleViewDetails() {
        Election selectedElection = electionsTableView.getSelectionModel().getSelectedItem();
        if (selectedElection != null) {
            // Show election details in a dialog
            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Election Details");
            detailsDialog.setHeaderText(selectedElection.getTitle());
            
            // Format the content with election details
            StringBuilder content = new StringBuilder();
            content.append("Description: ").append(selectedElection.getDescription()).append("\n\n");
            content.append("Start Date: ").append(selectedElection.getStartDate()).append("\n");
            content.append("End Date: ").append(selectedElection.getEndDate()).append("\n");
            content.append("Status: ").append(selectedElection.getStatus()).append("\n");
            content.append("You have ").append(selectedElection.isHasVoted() ? "voted" : "not voted").append(" in this election.");
            
            detailsDialog.setContentText(content.toString());
            detailsDialog.showAndWait();
        }
    }

    /**
     * Handle back button click
     */
    @FXML
    private void handleBack() {
        try {
            // Navigate back to the user dashboard
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/user-dashboard.fxml"));
            Parent dashboardParent = loader.load();
            
            // Pass the user to the dashboard controller
            UserDashboardController controller = loader.getController();
            controller.initData(currentUser);
            
            // Set the new scene
            Scene dashboardScene = new Scene(dashboardParent);
            Stage window = (Stage) filterComboBox.getScene().getWindow();
            window.setScene(dashboardScene);
            window.setTitle("User Dashboard");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error returning to dashboard: " + e.getMessage());
        }
    }
}
