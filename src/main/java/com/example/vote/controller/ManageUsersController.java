package com.example.vote.controller;

import com.example.vote.model.User;
import com.example.vote.util.DatabaseConnection;
import com.example.vote.util.PasswordHash;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class ManageUsersController implements Initializable {

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, Integer> idColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> fullnameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, Void> actionsColumn;

    @FXML
    private TextField searchField;

    @FXML
    private Button searchButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button addUserButton;

    @FXML
    private Button backButton;

    @FXML
    private Label statusLabel;

    @FXML
    private Pagination pagination;

    private final int ROWS_PER_PAGE = 10;
    private ObservableList<User> masterData = FXCollections.observableArrayList();
    private ObservableList<User> filteredData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize table columns with property references
        idColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        usernameColumn.setCellValueFactory(cellData -> cellData.getValue().usernameProperty());
        fullnameColumn.setCellValueFactory(cellData -> cellData.getValue().fullnameProperty());
        emailColumn.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        roleColumn.setCellValueFactory(cellData -> cellData.getValue().roleProperty());
        
        // Setup action column with buttons
        setupActionsColumn();
        
        // Load initial data
        loadUsers();
        
        // Setup pagination
        setupPagination();
        
        // Search field listener for real-time search
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterData(newValue);
            pagination.setCurrentPageIndex(0);
            updateTable(0);
        });
        
        // For debugging: show the table in a basic scene if nothing is visible
        if (userTable.getItems() == null || userTable.getItems().isEmpty()) {
            statusLabel.setText("No users found or table not updated properly");
        }
    }
    
    private void setupActionsColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final HBox buttonsBox = new HBox(5, editButton, deleteButton);
                    
                    {
                        // Setup Edit button
                        editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
                        editButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            handleEditUser(user);
                        });
                        
                        // Setup Delete button
                        deleteButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
                        deleteButton.setOnAction(event -> {
                            User user = getTableView().getItems().get(getIndex());
                            handleDeleteUser(user);
                        });
                    }
                    
                    @Override
                    public void updateItem(Void item, boolean empty) {
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
    
    private void loadUsers() {
        masterData.clear();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users ORDER BY id");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setRole(rs.getString("role"));
                user.setFullname(rs.getString("fullname"));
                user.setEmail(rs.getString("email"));
                
                masterData.add(user);
            }
            
            // Initialize filtered data with all data
            filteredData.setAll(masterData);
            int totalPages = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
            pagination.setPageCount(totalPages == 0 ? 1 : totalPages);
            updateTable(0);
            
            statusLabel.setText(String.format("%d users found", masterData.size()));
            
        } catch (SQLException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading users: " + e.getMessage());
        }
    }
    
    private void filterData(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredData.setAll(masterData);
        } else {
            filteredData.clear();
            String lowerCaseFilter = searchText.toLowerCase();
            
            for (User user : masterData) {
                // Match by username, fullname, email or role
                if (user.getUsername().toLowerCase().contains(lowerCaseFilter) ||
                    user.getFullname().toLowerCase().contains(lowerCaseFilter) ||
                    user.getEmail().toLowerCase().contains(lowerCaseFilter) ||
                    user.getRole().toLowerCase().contains(lowerCaseFilter)) {
                    filteredData.add(user);
                }
            }
        }
        
        int totalPages = (filteredData.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE;
        pagination.setPageCount(totalPages == 0 ? 1 : totalPages);
        updateTable(pagination.getCurrentPageIndex());
    }
    
    private void setupPagination() {
        pagination.setPageFactory(this::createPage);
    }
    
    private Node createPage(int pageIndex) {
        updateTable(pageIndex);
        return userTable;
    }
    
    private void updateTable(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredData.size());
        
        ObservableList<User> pageData;
        if (fromIndex > filteredData.size()) {
            pageData = FXCollections.observableArrayList();
        } else {
            pageData = FXCollections.observableArrayList(filteredData.subList(fromIndex, toIndex));
        }
        
        userTable.setItems(pageData);
    }
    
    @FXML
    private void handleSearch(ActionEvent event) {
        String searchText = searchField.getText().trim();
        filterData(searchText);
        
        int count = filteredData.size();
        statusLabel.setText(String.format("%d users found", count));
    }
    
    @FXML
    private void handleRefresh(ActionEvent event) {
        searchField.clear();
        loadUsers();
    }
    
    @FXML
    private void handleAddUser(ActionEvent event) {
        showUserDialog(null);
    }
    
    private void handleEditUser(User user) {
        showUserDialog(user);
    }
    
    private void handleDeleteUser(User user) {
        // Don't allow deleting yourself
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT username FROM users WHERE id = ?")) {
            stmt.setInt(1, user.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getString("username").equals("admin")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Cannot Delete Admin");
                    alert.setHeaderText(null);
                    alert.setContentText("You cannot delete the main admin account.");
                    alert.showAndWait();
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // Confirm delete
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Deletion");
        confirmation.setHeaderText("Delete User");
        confirmation.setContentText("Are you sure you want to delete user: " + user.getUsername() + "?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                
                stmt.setInt(1, user.getId());
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    statusLabel.setText("User deleted successfully!");
                    loadUsers(); // Refresh the table
                } else {
                    statusLabel.setText("Failed to delete user.");
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                statusLabel.setText("Error deleting user: " + e.getMessage());
                
                // Show more detailed error in alert
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Database Error");
                error.setHeaderText("Failed to Delete User");
                error.setContentText("An error occurred while deleting the user: " + e.getMessage());
                error.showAndWait();
            }
        }
    }
    
    private void showUserDialog(User existingUser) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/vote/user-form-view.fxml"));
            Parent userForm = loader.load();
            
            UserFormController controller = loader.getController();
            controller.initData(existingUser, this);
            
            Stage dialogStage = new Stage();
            dialogStage.setTitle(existingUser == null ? "Add New User" : "Edit User");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addUserButton.getScene().getWindow());
            
            Scene scene = new Scene(userForm);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Form Loading Error");
            alert.setContentText("Could not load the user form: " + e.getMessage());
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
        loadUsers();
    }
}
