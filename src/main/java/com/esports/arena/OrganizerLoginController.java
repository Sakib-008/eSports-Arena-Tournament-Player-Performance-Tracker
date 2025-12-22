package com.esports.arena;

import com.esports.arena.dao.OrganizerDAO;
import com.esports.arena.model.Organizer;

import javafx.concurrent.Task;
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
    private OrganizerDAO organizerDAO;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
        this.organizerDAO = new OrganizerDAO();
    }

    @FXML
    private void initialize() {
        helpLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            helpLabel.setText("Please enter both username and password.");
            return;
        }

        // Disable login button to prevent multiple clicks
        loginBtn.setDisable(true);
        helpLabel.setText("Authenticating...");

        Task<Organizer> loginTask = new Task<>() {
            @Override
            protected Organizer call() {
                return organizerDAO.authenticateOrganizer(user, pass);
            }
        };

        loginTask.setOnSucceeded(e -> {
            Organizer organizer = loginTask.getValue();
            if (organizer != null) {
                if (mainApp != null) {
                    mainApp.showOrganizerDashboard();
                }
            } else {
                helpLabel.setText("Invalid username or password.");
                passwordField.clear();
                loginBtn.setDisable(false);
            }
        });

        loginTask.setOnFailed(e -> {
            helpLabel.setText("Authentication error. Please try again.");
            loginBtn.setDisable(false);
        });

        new Thread(loginTask).start();
    }

    @FXML
    private void handleCancel() {
        if (mainApp != null) mainApp.showMainMenu();
    }
}
