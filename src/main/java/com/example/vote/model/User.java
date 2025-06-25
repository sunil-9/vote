package com.example.vote.model;

import javafx.beans.property.*;
import java.sql.Timestamp;

/**
 * User model representing a user in the system
 */
public class User {
    private final IntegerProperty id = new SimpleIntegerProperty(this, "id", 0);
    private final StringProperty username = new SimpleStringProperty(this, "username", "");
    private final StringProperty password = new SimpleStringProperty(this, "password", "");
    private final StringProperty role = new SimpleStringProperty(this, "role", "");
    private final StringProperty fullname = new SimpleStringProperty(this, "fullname", "");
    private final StringProperty email = new SimpleStringProperty(this, "email", "");
    private Timestamp createdAt;

    // Default constructor
    public User() {
    }

    // Constructor with essential fields
    public User(String username, String password, String role) {
        setUsername(username);
        setPassword(password);
        setRole(role);
    }

    // Constructor with all fields except id and createdAt
    public User(String username, String password, String role, String fullname, String email) {
        setUsername(username);
        setPassword(password);
        setRole(role);
        setFullname(fullname);
        setEmail(email);
    }

    // Full constructor
    public User(int id, String username, String password, String role, String fullname, String email, Timestamp createdAt) {
        setId(id);
        setUsername(username);
        setPassword(password);
        setRole(role);
        setFullname(fullname);
        setEmail(email);
        this.createdAt = createdAt;
    }

    // JavaFX Properties
    public IntegerProperty idProperty() {
        return id;
    }
    
    public StringProperty usernameProperty() {
        return username;
    }
    
    public StringProperty passwordProperty() {
        return password;
    }
    
    public StringProperty roleProperty() {
        return role;
    }
    
    public StringProperty fullnameProperty() {
        return fullname;
    }
    
    public StringProperty emailProperty() {
        return email;
    }
    
    // Getters and Setters
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public String getPassword() {
        return password.get();
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public String getRole() {
        return role.get();
    }

    public void setRole(String role) {
        this.role.set(role);
    }

    public String getFullname() {
        return fullname.get();
    }

    public void setFullname(String fullname) {
        this.fullname.set(fullname);
    }

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(getRole());
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + getId() +
                ", username='" + getUsername() + '\'' +
                ", role='" + getRole() + '\'' +
                ", fullname='" + getFullname() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
