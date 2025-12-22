package com.esports.arena;

import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.model.Player;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PlayerLoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Button cancelBtn;
    @FXML private Label errorLabel;
    @FXML private Label helpLabel;

    private MainApp mainApp;
    private PlayerDAO playerDAO;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        playerDAO = new PlayerDAO();
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String user = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String pass = passwordField.getText() == null ? "" : passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("Username and password are required");
            return;
        }

        loginBtn.setDisable(true);

        // Authenticate player in background
        Task<Player> task = new Task<Player>() {
            @Override
            protected Player call() {
                Player player = playerDAO.getPlayerByUsername(user);
                if (player != null && player.getPassword() != null && player.getPassword().equals(pass)) {
                    return player;
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            loginBtn.setDisable(false);
            Player player = task.getValue();
            if (player != null) {
                if (mainApp != null) {
                    mainApp.setCurrentPlayer(player);
                    mainApp.showPlayerDashboard();
                }
            } else {
                errorLabel.setText("Invalid username or password");
            }
        });

        task.setOnFailed(e -> {
            loginBtn.setDisable(false);
            errorLabel.setText("Login failed: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleCancel() {
        if (mainApp != null) mainApp.showMainMenu();
    }
}
