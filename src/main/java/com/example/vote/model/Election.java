package com.example.vote.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * Election model representing an election in the system
 */
public class Election {
    private final IntegerProperty id = new SimpleIntegerProperty(this, "id", 0);
    private final StringProperty title = new SimpleStringProperty(this, "title", "");
    private final StringProperty description = new SimpleStringProperty(this, "description", "");
    private final ObjectProperty<LocalDateTime> startDate = new SimpleObjectProperty<>(this, "startDate");
    private final ObjectProperty<LocalDateTime> endDate = new SimpleObjectProperty<>(this, "endDate");
    private final StringProperty status = new SimpleStringProperty(this, "status", "pending");
    private final IntegerProperty createdBy = new SimpleIntegerProperty(this, "createdBy", 0);
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>(this, "createdAt");
    private final ObjectProperty<LocalDateTime> updatedAt = new SimpleObjectProperty<>(this, "updatedAt");
    
    // Default constructor
    public Election() {
    }
    
    // Constructor with essential fields
    public Election(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        setTitle(title);
        setDescription(description);
        setStartDate(startDate);
        setEndDate(endDate);
    }
    
    // Full constructor
    public Election(int id, String title, String description, LocalDateTime startDate, 
                    LocalDateTime endDate, String status, int createdBy, 
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        setId(id);
        setTitle(title);
        setDescription(description);
        setStartDate(startDate);
        setEndDate(endDate);
        setStatus(status);
        setCreatedBy(createdBy);
        setCreatedAt(createdAt);
        setUpdatedAt(updatedAt);
    }
    
    // JavaFX Properties
    public IntegerProperty idProperty() {
        return id;
    }
    
    public StringProperty titleProperty() {
        return title;
    }
    
    public StringProperty descriptionProperty() {
        return description;
    }
    
    public ObjectProperty<LocalDateTime> startDateProperty() {
        return startDate;
    }
    
    public ObjectProperty<LocalDateTime> endDateProperty() {
        return endDate;
    }
    
    public StringProperty statusProperty() {
        return status;
    }
    
    public IntegerProperty createdByProperty() {
        return createdBy;
    }
    
    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }
    
    public ObjectProperty<LocalDateTime> updatedAtProperty() {
        return updatedAt;
    }
    
    // Getters and Setters
    public int getId() {
        return id.get();
    }
    
    public void setId(int id) {
        this.id.set(id);
    }
    
    public String getTitle() {
        return title.get();
    }
    
    public void setTitle(String title) {
        this.title.set(title);
    }
    
    public String getDescription() {
        return description.get();
    }
    
    public void setDescription(String description) {
        this.description.set(description);
    }
    
    public LocalDateTime getStartDate() {
        return startDate.get();
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate.set(startDate);
    }
    
    public LocalDateTime getEndDate() {
        return endDate.get();
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate.set(endDate);
    }
    
    public String getStatus() {
        return status.get();
    }
    
    public void setStatus(String status) {
        this.status.set(status);
    }
    
    public int getCreatedBy() {
        return createdBy.get();
    }
    
    public void setCreatedBy(int createdBy) {
        this.createdBy.set(createdBy);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt.get();
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt.set(updatedAt);
    }
    
    public boolean isPending() {
        return "pending".equalsIgnoreCase(getStatus());
    }
    
    public boolean isActive() {
        return "active".equalsIgnoreCase(getStatus());
    }
    
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(getStatus());
    }
    
    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(getStatus());
    }
    
    @Override
    public String toString() {
        return "Election{" +
                "id=" + getId() +
                ", title='" + getTitle() + '\'' +
                ", startDate=" + getStartDate() +
                ", endDate=" + getEndDate() +
                ", status='" + getStatus() + '\'' +
                '}';
    }
}
