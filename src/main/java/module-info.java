module com.esports.arena.esportsarena {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;

    // HTTP/Firebase
    requires java.net.http;
    

    // JSON processing
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.annotation;

    // Export packages to JavaFX
    opens com.esports.arena to javafx.fxml;
    opens com.esports.arena.tabs to javafx.fxml;
    opens com.esports.arena.model to javafx.base, com.fasterxml.jackson.databind;

    exports com.esports.arena;
    exports com.esports.arena.tabs;
    exports com.esports.arena.model;
    exports com.esports.arena.dao;
    exports com.esports.arena.service;
}