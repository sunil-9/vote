module com.example.vote {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    requires java.mail;

    // Open packages to JavaFX FXML
    opens com.example.vote to javafx.fxml;
    opens com.example.vote.controller to javafx.fxml;
    opens com.example.vote.model to javafx.fxml;

    // Export packages
    exports com.example.vote;
    exports com.example.vote.controller;
    exports com.example.vote.model;
    exports com.example.vote.util;
}