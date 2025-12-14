package com.esports.arena;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class MainMenuController {
    @FXML private VBox mainMenuContainer;
    @FXML private Button organizerButton;
    @FXML private Button playerButton;
    @FXML private Button exitButton;

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {

    }

    @FXML
    private void handleOrganizerMode() {
        mainApp.showOrganizerDashboard();
    }

    @FXML
    private void handlePlayerMode() {
        mainApp.showPlayerDashboard();
    }

    @FXML
    private void handleExit() {
        System.exit(0);
    }
}