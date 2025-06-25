package com.example.vote.controller;

import com.example.vote.model.Election;
import com.example.vote.util.DatabaseConnection;
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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManageElectionsController implements Initializable {

    @FXML
    private TableView<Election> electionTable;

    @FXML
    private TableColumn<Election, Integer> idColumn;

    @FXML
    private TableColumn<Election, String> titleColumn;

    @FXML
    private TableColumn<Election, String> startDateColumn;

    @FXML
    private TableColumn<Election, String> endDateColumn;

    @FXML
    private TableColumn<Election, String> statusColumn;

    @FXML
    private TableColumn<Election, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button addElectionButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Pagination pagination;

    private final int ROWS_PER_PAGE = 10;
    private ObservableList<Election> masterData = FXCollections.observableArrayList();
    private ObservableList<Election> filteredData = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize table columns with property references
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        titleColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
        
        // Format dates for display
        startDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getStartDate();
            return new SimpleStringProperty(date != null ? date.format(dateFormatter) : "");
        });
        
        endDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getEndDate();
            return new SimpleStringProperty(date != null ? date.format(dateFormatter) : "");
        });
        
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        
        // Setup action column with buttons
        setupActionsColumn();
        
        // Load initial data
        loadElections();
        
        // Setup pagination
        setupPagination();
        
        // Search field listener for real-time search
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
            pagination.setCurrentPageIndex(0);
            updateTable(0);
        });
    }
    
    private void setupActionsColumn() {
        Callback<TableColumn<Election, Void>, TableCell<Election, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Election, Void> call(final TableColumn<Election, Void> param) {
                return new TableCell<>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final Button candidatesButton = new Button("Candidates");
                    private final HBox buttonsBox = new HBox(5, editButton, candidatesButton, deleteButton);
                    
                    {
                        // Setup Edit button
                        editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                        editButton.setOnAction(event -> {
                            Election election = getTableView().getItems().get(getIndex());
                            handleEditElection(election);
                        });
                        
                        // Setup Candidates button
                        candidatesButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
                        candidatesButton.setOnAction(event -> {
                            Election election = getTableView().getItems().get(getIndex());
                            handleManageCandidates(election);
                        });
                        
                        // Setup Delete button
                        deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        deleteButton.setOnAction(event -> {
                            Election election = getTableView().getItems().get(getIndex());
                            handleDeleteElection(election);
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
    
    private void loadElections() {
        masterData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM elections ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Election election = new Election();
                election.setId(rs.getInt("id"));
                election.setTitle(rs.getString("title"));
                election.setDescription(rs.getString("description"));
                election.setStartDate(rs.getTimestamp("start_date").toLocalDateTime());
                election.setEndDate(rs.getTimestamp("end_date").toLocalDateTime());
                election.setStatus(rs.getString("status"));
                election.setCreatedBy(rs.getInt("created_by"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    election.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    election.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                
                masterData.add(election);
            }
            
            // Initialize filtered data with all data
            filteredData.setAll(masterData);
            int totalPages = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
            pagination.setPageCount(totalPages == 0 ? 1 : totalPages);
            updateTable(0);
            
            statusLabel.setText(String.format("%d elections found", masterData.size()));
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading elections: " + e.getMessage());
        }
    }
    
    private void filterData(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredData.setAll(masterData);
        } else {
            filteredData.clear();
            String lowerCaseFilter = searchText.toLowerCase();
            
            for (Election election : masterData) {
                if (election.getTitle().toLowerCase().contains(lowerCaseFilter) ||
                    election.getDescription().toLowerCase().contains(lowerCaseFilter) ||
                    election.getStatus().toLowerCase().contains(lowerCaseFilter)) {
                    filteredData.add(election);
                }
            }
        }
        
        int totalPages = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        pagination.setPageCount(totalPages == 0 ? 1 : totalPages);
    }
    
    private void setupPagination() {
        pagination.setPageFactory(this::createPage);
    }
    
    private Node createPage(int pageIndex) {
        updateTable(pageIndex);
        return electionTable;
    }
    
    private void updateTable(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredData.size());
        
        ObservableList<Election> pageData;
        if (fromIndex > filteredData.size()) {
            pageData = FXCollections.observableArrayList();
        } else {
            pageData = FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex));
        }
        
        electionTable.setItems(pageData);
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
        loadElections();
    }
    
    @FXML
    private void handleAddElection(ActionEvent event) {
        showElectionDialog(null);
    }
    
    private void handleEditElection(Election election) {
        showElectionDialog(election);
    }
    
    private void handleManageCandidates(Election election) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/manage-candidates-view.fxml"));
            Parent root = loader.load();
            
            ManageCandidatesController controller = loader.getController();
            controller.initData(election);
            
            Stage stage = new Stage();
            stage.setTitle("Manage Candidates - " + election.getTitle());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(electionTable.getScene().getWindow());
            stage.showAndWait();
            
            // Refresh after managing candidates
            loadElections();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error opening candidates view: " + e.getMessage());
        }
    }
    
    private void handleDeleteElection(Election election) {
        // Check if the election has associated votes
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM votes WHERE election_id = ?")) {
            
            stmt.setInt(1, election.getId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cannot Delete Election");
                alert.setHeaderText(null);
                alert.setContentText("This election has associated votes and cannot be deleted.");
                alert.showAndWait();
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Confirm delete
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete Election");
        confirmation.setContentText("Are you sure you want to delete election: " + election.getTitle() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection();
                 // Delete candidates first (due to foreign key constraint)
                 PreparedStatement candidatesStmt = conn.prepareStatement("DELETE FROM candidates WHERE election_id = ?");
                 // Then delete the election
                 PreparedStatement electionStmt = conn.prepareStatement("DELETE FROM elections WHERE id = ?")) {
                
                // First delete associated candidates
                candidatesStmt.setInt(1, election.getId());
                candidatesStmt.executeUpdate();
                
                // Then delete the election
                electionStmt.setInt(1, election.getId());
                int rowsAffected = electionStmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    statusLabel.setText("Election deleted successfully!");
                    loadElections(); // Refresh the table
                } else {
                    statusLabel.setText("Failed to delete election.");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                statusLabel.setText("Error deleting election: " + e.getMessage());
                
                // Show more detailed error in alert
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Database Error");
                error.setHeaderText("Failed to Delete Election");
                error.setContentText("An error occurred while deleting the election: " + e.getMessage());
                error.showAndWait();
            }
        }
    }
    
    private void showElectionDialog(Election existingElection) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/election-form-view.fxml"));
            Parent electionForm = loader.load();
            
            ElectionFormController controller = loader.getController();
            controller.initData(existingElection, this);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle(existingElection == null ? "Add New Election" : "Edit Election");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addElectionButton.getScene().getWindow());
            
            Scene scene = new Scene(electionForm);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Form Loading Error");
            alert.setContentText("Could not load the election form: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handleBackToAdminDashboard(ActionEvent event) {
        try {
            Parent adminDashboardParent = FXMLLoader.load(getClass().getResource("/com/example/vote/admin-dashboard.fxml"));
            Scene adminDashboardScene = new Scene(adminDashboardParent);
            
            // Get the current stage
            Stage window = (Stage) ((Node) event.getSource()).getScene().getWindow();
            window.setScene(adminDashboardScene);
            window.setTitle("Admin Dashboard");
            window.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading admin dashboard: " + e.getMessage());
        }
    }
    
    // Public method to refresh the table after add/edit operations
    public void refreshTable() {
        loadElections();
    }
}
