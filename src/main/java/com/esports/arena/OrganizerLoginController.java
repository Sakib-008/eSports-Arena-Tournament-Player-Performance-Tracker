package com.esports.arena;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class OrganizerLoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button cancelBtn;
    @FXML private Label helpLabel;

    private MainApp mainApp;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        helpLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        // Simple credential check for organizers.
        if (user.equals("organizer") && pass.equals("123")) {
            if (mainApp != null) mainApp.showOrganizerDashboard();
        } else {
            helpLabel.setText("Invalid credentials.");
        }
    }

    @FXML
    private void handleCancel() {
        // Return to main menu
        if (mainApp != null) mainApp.showMainMenu();
    }
}
