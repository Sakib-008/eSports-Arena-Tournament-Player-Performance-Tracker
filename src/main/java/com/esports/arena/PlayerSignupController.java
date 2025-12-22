package com.esports.arena;

import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.model.Player;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.time.LocalDate;

public class PlayerSignupController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField realNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Button signupBtn;
    @FXML private Button cancelBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private MainApp mainApp;
    private PlayerDAO playerDAO;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        playerDAO = new PlayerDAO();
        errorLabel.setText("");
        successLabel.setText("");

        // Initialize role combo box
        roleCombo.setItems(FXCollections.observableArrayList(
                "Rifler", "Support", "Sniper", "IGL", "Lurker", "Entry Fragger"
        ));
        roleCombo.setValue("Rifler");
    }

    @FXML
    private void handleSignup() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();
        String confirmPassword = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        String realName = realNameField.getText() == null ? "" : realNameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String role = roleCombo.getValue();

        errorLabel.setText("");
        successLabel.setText("");

        // Validation
        if (username.isEmpty() || password.isEmpty() || realName.isEmpty() || email.isEmpty()) {
            errorLabel.setText("All fields are required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            return;
        }

        if (password.length() < 6) {
            errorLabel.setText("Password must be at least 6 characters");
            return;
        }

        if (!email.contains("@")) {
            errorLabel.setText("Please enter a valid email address");
            return;
        }

        signupBtn.setDisable(true);

        // Create player in background
        Task<Integer> task = new Task<Integer>() {
            @Override
            protected Integer call() {
                try {
                    // Check if username already exists
                    System.out.println("Checking if username '" + username + "' exists...");
                    Player existing = playerDAO.getPlayerByUsername(username);
                    if (existing != null) {
                        System.out.println("Username '" + username + "' already exists (ID: " + existing.getId() + ")");
                        return -1; // Username taken
                    }
                    System.out.println("Username '" + username + "' is available, creating player...");

                    // Create new player (store plain password)
                    Player player = new Player(username, realName, email, role);
                    player.setPassword(password);
                    player.setJoinDate(LocalDate.now());
                    player.setAvailable(true);

                    int playerId = playerDAO.createPlayer(player);
                    System.out.println("Player created with ID: " + playerId);
                    return playerId;
                } catch (Exception ex) {
                    System.err.println("Signup error: " + ex.getMessage());
                    return -2; // Error during creation
                }
            }
        };

        task.setOnSucceeded(e -> {
            signupBtn.setDisable(false);
            int playerId = task.getValue();
            if (playerId == -1) {
                errorLabel.setText("❌ Username already exists. Please choose a different username.");
            } else if (playerId == -2) {
                errorLabel.setText("❌ Account creation failed. Please try again or check console for details.");
            } else if (playerId > 0) {
                successLabel.setText("✓ Account created successfully! Redirecting to dashboard...");
                // Load the created player and show dashboard
                Task<Player> loadTask = new Task<Player>() {
                    @Override
                    protected Player call() {
                        return playerDAO.getPlayerById(playerId);
                    }
                };

                loadTask.setOnSucceeded(e2 -> {
                    Player player = loadTask.getValue();
                    if (player != null && mainApp != null) {
                        mainApp.setCurrentPlayer(player);
                        mainApp.showPlayerDashboard();
                    }
                });

                new Thread(loadTask).start();
            } else {
                errorLabel.setText("❌ Failed to create account. Please try again.");
            }
        });

        task.setOnFailed(e -> {
            signupBtn.setDisable(false);
            errorLabel.setText("Signup failed: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    @FXML
    private void handleCancel() {
        if (mainApp != null) mainApp.showMainMenu();
    }
}
