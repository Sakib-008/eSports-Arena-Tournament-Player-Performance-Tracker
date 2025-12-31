package com.esports.arena.tabs;

import com.esports.arena.MainApp;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.model.Player;
import com.esports.arena.model.Team;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;

public class PlayerProfileTabController {
    @FXML private Label playerNameLabel;
    @FXML private Label playerTeamLabel;
    @FXML private Label playerRoleLabel;
    @FXML private Label playerKDLabel;
    @FXML private Label playerWinRateLabel;
    @FXML private ToggleButton availabilityToggle;
    @FXML private TextArea availabilityReasonField;
    @FXML private Button updateAvailabilityBtn;

    private MainApp mainApp;
    private PlayerDAO playerDAO;
    private TeamDAO teamDAO;
    private Player currentPlayer;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    @FXML
    private void initialize() {
        playerDAO = new PlayerDAO();
        teamDAO = new TeamDAO();
        setupAvailabilityToggle();
    }

    private void setupAvailabilityToggle() {
        availabilityToggle.selectedProperty().addListener((obs, wasAvailable, isAvailable) -> {
            availabilityReasonField.setDisable(isAvailable);
            if (isAvailable) {
                availabilityReasonField.clear();
            }
        });
    }

    public void loadPlayer(Player player) {
        if (player == null) return;
        this.currentPlayer = player;

        playerNameLabel.setText(player.getUsername());
        playerRoleLabel.setText(player.getRole());
        playerKDLabel.setText(String.format("%.2f", player.getKdRatio()));
        playerWinRateLabel.setText(String.format("%.1f%%", player.getWinRate()));

        availabilityToggle.setSelected(player.isAvailable());
        availabilityReasonField.setText(player.getAvailabilityReason() != null ? player.getAvailabilityReason() : "");

        // Load team name
        if (player.getTeamId() == null || player.getTeamId() == 0) {
            playerTeamLabel.setText("No Team");
        } else {
            Task<Team> loadTeamTask = new Task<>() {
                @Override
                protected Team call() {
                    return teamDAO.getTeamById(player.getTeamId());
                }
            };
            
            loadTeamTask.setOnSucceeded(e -> {
                Team team = loadTeamTask.getValue();
                if (team != null) {
                    playerTeamLabel.setText(team.getName() + " [" + team.getTag() + "]");
                } else {
                    playerTeamLabel.setText("No Team");
                }
            });
            
            loadTeamTask.setOnFailed(e -> playerTeamLabel.setText("Error loading team"));
            
            new Thread(loadTeamTask).start();
        }
    }

    @FXML
    private void handleUpdateAvailability() {
        if (currentPlayer == null) {
            MainApp.showError("Error", "No player selected");
            return;
        }

        boolean isAvailable = availabilityToggle.isSelected();
        String reason = availabilityReasonField.getText().trim();

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return playerDAO.updateAvailability(currentPlayer.getId(), isAvailable, reason);
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                MainApp.showInfo("Success", "Availability updated successfully");
                currentPlayer.setAvailable(isAvailable);
                currentPlayer.setAvailabilityReason(reason);
            } else {
                MainApp.showError("Error", "Failed to update availability");
            }
        });

        task.setOnFailed(e -> MainApp.showError("Error", "Failed to update availability"));

        new Thread(task).start();
    }
}
