package com.example.vote.controller;

import com.example.vote.model.Candidate;
import com.example.vote.model.Election;
import com.example.vote.util.DatabaseConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManageCandidatesController implements Initializable {

    @FXML
    private Label headerLabel;
    
    @FXML
    private TableView<Candidate> candidateTable;
    
    @FXML
    private TableColumn<Candidate, Integer> idColumn;
    
    @FXML
    private TableColumn<Candidate, String> nameColumn;
    
    @FXML
    private TableColumn<Candidate, String> positionColumn;
    
    @FXML
    private TableColumn<Candidate, Integer> votesColumn;
    
    @FXML
    private TableColumn<Candidate, Void> actionsColumn;
    
    @FXML
    private TextField searchField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Button addCandidateButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Pagination pagination;
    
    private Election election;
    private final int ROWS_PER_PAGE = 10;
    private ObservableList<Candidate> masterData = FXCollections.observableArrayList();
    private ObservableList<Candidate> filteredData = FXCollections.observableArrayList();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize table columns with property references
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        positionColumn.setCellValueFactory(cellData -> cellData.getValue().positionProperty());
        votesColumn.setCellValueFactory(cellData -> cellData.getValue().votesProperty().asObject());
        
        // Setup action column with buttons
        setupActionsColumn();
        
        // Setup pagination
        setupPagination();
        
        // Add search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
            pagination.setCurrentPageIndex(0);
            updateTable(0);
        });
    }
    
    public void initData(Election election) {
        this.election = election;
        headerLabel.setText("Candidates for Election: " + election.getTitle());
        loadCandidates();
    }
    
    private void setupActionsColumn() {
        Callback<TableColumn<Candidate, Void>, TableCell<Candidate, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Candidate, Void> call(final TableColumn<Candidate, Void> param) {
                return new TableCell<>() {
                    private final Button editButton = new Button("Edit");
                    private final Button viewButton = new Button("View");
                    private final Button deleteButton = new Button("Delete");
                    private final HBox buttonsBox = new HBox(5, editButton, viewButton, deleteButton);
                    
                    {
                        // Setup Edit button
                        editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                        editButton.setOnAction(event -> {
                            Candidate candidate = getTableView().getItems().get(getIndex());
                            handleEditCandidate(candidate);
                        });
                        
                        // Setup View button
                        viewButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                        viewButton.setOnAction(event -> {
                            Candidate candidate = getTableView().getItems().get(getIndex());
                            handleViewCandidate(candidate);
                        });
                        
                        // Setup Delete button
                        deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        deleteButton.setOnAction(event -> {
                            Candidate candidate = getTableView().getItems().get(getIndex());
                            handleDeleteCandidate(candidate);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonsBox);
                        }
                    }
                };
            }
        };
        
        actionsColumn.setCellFactory(cellFactory);
    }
    
    private void loadCandidates() {
        masterData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM candidates WHERE election_id = ? ORDER BY id")) {
            
            stmt.setInt(1, election.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Candidate candidate = new Candidate();
                candidate.setId(rs.getInt("id"));
                candidate.setElectionId(rs.getInt("election_id"));
                candidate.setName(rs.getString("name"));
                candidate.setProfile(rs.getString("profile"));
                candidate.setPhotoUrl(rs.getString("photo_url"));
                candidate.setPosition(rs.getString("position"));
                candidate.setVotes(rs.getInt("votes"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    candidate.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                candidate.setElection(election);
                masterData.add(candidate);
            }
            
            // Initialize filtered data with all data
            filteredData.setAll(masterData);
            int totalPages = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
            pagination.setPageCount(totalPages == 0 ? 1 : totalPages);
            updateTable(0);
            
            statusLabel.setText(String.format("%d candidates found", masterData.size()));
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading candidates: " + e.getMessage());
        }
    }
    
    private void filterData(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredData.setAll(masterData);
        } else {
            filteredData.clear();
            String lowerCaseFilter = searchText.toLowerCase();
            
            for (Candidate candidate : masterData) {
                if (candidate.getName().toLowerCase().contains(lowerCaseFilter) ||
                    candidate.getPosition().toLowerCase().contains(lowerCaseFilter) ||
                    candidate.getProfile().toLowerCase().contains(lowerCaseFilter)) {
                    filteredData.add(candidate);
                }
            }
        }
        
        int totalPages = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        pagination.setPageCount(totalPages == 0 ? 1 : totalPages);
    }
    
    private void setupPagination() {
        pagination.setPageFactory(this::createPage);
    }
    
    private TableView<Candidate> createPage(int pageIndex) {
        updateTable(pageIndex);
        return candidateTable;
    }
    
    private void updateTable(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredData.size());
        
        ObservableList<Candidate> pageData;
        if (fromIndex > filteredData.size()) {
            pageData = FXCollections.observableArrayList();
        } else {
            pageData = FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex));
        }
        
        candidateTable.setItems(pageData);
    }
    
    @FXML
    private void handleSearch(ActionEvent event) {
        filterData(searchField.getText());
        pagination.setCurrentPageIndex(0);
        updateTable(0);
    }
    
    @FXML
    private void handleRefresh(ActionEvent event) {
        searchField.clear();
        loadCandidates();
    }
    
    @FXML
    private void handleAddCandidate(ActionEvent event) {
        showCandidateDialog(null);
    }
    
    private void handleEditCandidate(Candidate candidate) {
        showCandidateDialog(candidate);
    }
    
    private void handleViewCandidate(Candidate candidate) {
        // Could implement a read-only view of the candidate
        showCandidateDialog(candidate, true);
    }
    
    private void handleDeleteCandidate(Candidate candidate) {
        // Check if the candidate has votes
        if (candidate.getVotes() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Delete Candidate");
            alert.setHeaderText(null);
            alert.setContentText("This candidate has votes and cannot be deleted to preserve election integrity.");
            alert.showAndWait();
            return;
        }
        
        // Confirm delete
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Candidate");
        confirmation.setContentText("Are you sure you want to delete candidate: " + candidate.getName() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM candidates WHERE id = ?")) {
                
                stmt.setInt(1, candidate.getId());
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    statusLabel.setText("Candidate deleted successfully!");
                    loadCandidates(); // Refresh the table
                } else {
                    statusLabel.setText("Failed to delete candidate.");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                statusLabel.setText("Error deleting candidate: " + e.getMessage());
                
                // Show more detailed error in alert
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Database Error");
                error.setHeaderText("Failed to Delete Candidate");
                error.setContentText("An error occurred while deleting the candidate: " + e.getMessage());
                error.showAndWait();
            }
        }
    }
    
    private void showCandidateDialog(Candidate existingCandidate) {
        showCandidateDialog(existingCandidate, false);
    }
    
    private void showCandidateDialog(Candidate existingCandidate, boolean readOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/candidate-form-view.fxml"));
            Parent candidateForm = loader.load();
            
            CandidateFormController controller = loader.getController();
            controller.initData(existingCandidate, election, this, readOnly);
            
            Stage dialogStage = new Stage();
            String title = existingCandidate == null ? "Add New Candidate" : 
                           readOnly ? "View Candidate" : "Edit Candidate";
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addCandidateButton.getScene().getWindow());
            
            Scene scene = new Scene(candidateForm);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Form Loading Error");
            alert.setContentText("Could not load the candidate form: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleBackToElections(ActionEvent event) {
        // Close the current stage
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }
    
    // Public method to refresh the table after add/edit operations
    public void refreshTable() {
        loadCandidates();
    }
}
