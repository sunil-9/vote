package com.example.vote.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * Candidate model representing a candidate in an election
 */
public class Candidate {
    private final IntegerProperty id = new SimpleIntegerProperty(this, "id", 0);
    private final IntegerProperty electionId = new SimpleIntegerProperty(this, "electionId", 0);
    private final StringProperty name = new SimpleStringProperty(this, "name", "");
    private final StringProperty profile = new SimpleStringProperty(this, "profile", "");
    private final StringProperty photoUrl = new SimpleStringProperty(this, "photoUrl", "");
    private final StringProperty position = new SimpleStringProperty(this, "position", "");
    private final IntegerProperty votes = new SimpleIntegerProperty(this, "votes", 0);
    private final ObjectProperty<LocalDateTime> createdAt = new SimpleObjectProperty<>(this, "createdAt");
    
    // For UI purposes, we can also keep a reference to the related election
    private Election election;
    
    // Default constructor
    public Candidate() {
    }
    
    // Constructor with essential fields
    public Candidate(int electionId, String name, String position) {
        setElectionId(electionId);
        setName(name);
        setPosition(position);
    }
    
    // Constructor with more fields
    public Candidate(int electionId, String name, String profile, String position, String photoUrl) {
        setElectionId(electionId);
        setName(name);
        setProfile(profile);
        setPosition(position);
        setPhotoUrl(photoUrl);
    }
    
    // Full constructor
    public Candidate(int id, int electionId, String name, String profile, String photoUrl, 
                     String position, int votes, LocalDateTime createdAt) {
        setId(id);
        setElectionId(electionId);
        setName(name);
        setProfile(profile);
        setPhotoUrl(photoUrl);
        setPosition(position);
        setVotes(votes);
        setCreatedAt(createdAt);
    }
    
    // JavaFX Properties
    public IntegerProperty idProperty() {
        return id;
    }
    
    public IntegerProperty electionIdProperty() {
        return electionId;
    }
    
    public StringProperty nameProperty() {
        return name;
    }
    
    public StringProperty profileProperty() {
        return profile;
    }
    
    public StringProperty photoUrlProperty() {
        return photoUrl;
    }
    
    public StringProperty positionProperty() {
        return position;
    }
    
    public IntegerProperty votesProperty() {
        return votes;
    }
    
    public ObjectProperty<LocalDateTime> createdAtProperty() {
        return createdAt;
    }
    
    // Getters and Setters
    public int getId() {
        return id.get();
    }
    
    public void setId(int id) {
        this.id.set(id);
    }
    
    public int getElectionId() {
        return electionId.get();
    }
    
    public void setElectionId(int electionId) {
        this.electionId.set(electionId);
    }
    
    public String getName() {
        return name.get();
    }
    
    public void setName(String name) {
        this.name.set(name);
    }
    
    public String getProfile() {
        return profile.get();
    }
    
    public void setProfile(String profile) {
        this.profile.set(profile);
    }
    
    public String getPhotoUrl() {
        return photoUrl.get();
    }
    
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl.set(photoUrl);
    }
    
    public String getPosition() {
        return position.get();
    }
    
    public void setPosition(String position) {
        this.position.set(position);
    }
    
    public int getVotes() {
        return votes.get();
    }
    
    public void setVotes(int votes) {
        this.votes.set(votes);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt.get();
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt.set(createdAt);
    }
    
    // Election reference handling
    public Election getElection() {
        return election;
    }
    
    public void setElection(Election election) {
        this.election = election;
        if (election != null) {
            setElectionId(election.getId());
        }
    }
    
    @Override
    public String toString() {
        return "Candidate{" +
                "id=" + getId() +
                ", electionId=" + getElectionId() +
                ", name='" + getName() + '\'' +
                ", position='" + getPosition() + '\'' +
                ", votes=" + getVotes() +
                '}';
    }
}
