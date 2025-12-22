package com.esports.arena;

import com.esports.arena.dao.*;
import com.esports.arena.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PlayerDashboardController {
    // Tab Pane
    @FXML private TabPane playerTabPane;

    // Header
    @FXML private ComboBox<Player> playerSelectCombo;
    @FXML private Button backToMenuBtn;

    // Profile Tab
    @FXML private Label playerNameLabel;
    @FXML private Label playerTeamLabel;
    @FXML private Label playerRoleLabel;
    @FXML private Label playerKDLabel;
    @FXML private Label playerWinRateLabel;
    @FXML private ToggleButton availabilityToggle;
    @FXML private TextArea availabilityReasonField;
    @FXML private Button updateAvailabilityBtn;

    // Statistics Tab
    @FXML private Label totalKillsLabel;
    @FXML private Label totalDeathsLabel;
    @FXML private Label totalAssistsLabel;
    @FXML private Label matchesPlayedLabel;
    @FXML private Label matchesWonLabel;
    @FXML private BarChart<String, Number> performanceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    // Team Tab
    @FXML private Label myTeamNameLabel;
    @FXML private Label teamWinsLabel;
    @FXML private Label teamLossesLabel;
    @FXML private Label teamLeaderLabel;
    @FXML private ListView<Player> teamMembersList;

    // Voting Tab
    @FXML private ListView<Player> voteCandidatesList;
    @FXML private Button castVoteBtn;
    @FXML private Label votingStatusLabel;
    @FXML private Button viewVotingResultsBtn;

    private MainApp mainApp;
    private PlayerDAO playerDAO;
    private TeamDAO teamDAO;
    private LeaderVoteDAO voteDAO;

    private Player currentPlayer;
    private Team currentTeam;

    private ObservableList<Player> allPlayers;
    private ObservableList<Player> teamMembers;
    private ObservableList<Player> voteCandidates;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        playerDAO = new PlayerDAO();
        teamDAO = new TeamDAO();
        voteDAO = new LeaderVoteDAO();

        allPlayers = FXCollections.observableArrayList();
        teamMembers = FXCollections.observableArrayList();
        voteCandidates = FXCollections.observableArrayList();

        setupPlayerSelection();
        setupAvailabilityToggle();
        setupListViews();

        loadAllPlayers();
    }

    private void setupPlayerSelection() {
        playerSelectCombo.setItems(allPlayers);
        playerSelectCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Player item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " - " + item.getRealName());
                }
            }
        });

        playerSelectCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Player item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select Player");
                } else {
                    setText(item.getUsername());
                }
            }
        });

        playerSelectCombo.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        loadPlayerData(newVal);
                    }
                });
    }

    private void setupAvailabilityToggle() {
        availabilityToggle.selectedProperty().addListener((obs, wasAvailable, isAvailable) -> {
            availabilityReasonField.setDisable(isAvailable);
            if (isAvailable) {
                availabilityReasonField.clear();
            }
        });
    }

    private void setupListViews() {
        // Team Members List
        teamMembersList.setItems(teamMembers);
        teamMembersList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Player item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String status = item.isAvailable() ? "✓" : "✗";
                    setText(status + " " + item.getUsername() + " - " + item.getRole());
                    if (item.isAvailable()) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #e94560;");
                    }
                }
            }
        });

        // Vote Candidates List
        voteCandidatesList.setItems(voteCandidates);
        voteCandidatesList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Player item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getUsername() + " (" + item.getRole() + ")");
                }
            }
        });
    }

    private void loadAllPlayers() {
        Task<List<Player>> task = new Task<>() {
            @Override
            protected List<Player> call() {
                return playerDAO.getAllPlayers();
            }
        };

        task.setOnSucceeded(e -> {
            allPlayers.setAll(task.getValue());
            if (!allPlayers.isEmpty()) {
                playerSelectCombo.getSelectionModel().selectFirst();
            }
        });

        task.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load players"));

        new Thread(task).start();
    }

    private void loadPlayerData(Player player) {
        currentPlayer = player;

        // Update Profile Tab
        playerNameLabel.setText(player.getUsername());
        playerRoleLabel.setText(player.getRole());
        playerKDLabel.setText(String.format("%.2f", player.getKdRatio()));
        playerWinRateLabel.setText(String.format("%.1f%%", player.getWinRate()));

        // Update Statistics Tab
        totalKillsLabel.setText(String.valueOf(player.getTotalKills()));
        totalDeathsLabel.setText(String.valueOf(player.getTotalDeaths()));
        totalAssistsLabel.setText(String.valueOf(player.getTotalAssists()));
        matchesPlayedLabel.setText(String.valueOf(player.getMatchesPlayed()));
        matchesWonLabel.setText(String.valueOf(player.getMatchesWon()));

        // Update availability
        availabilityToggle.setSelected(player.isAvailable());
        availabilityReasonField.setText(player.getAvailabilityReason() != null ?
                player.getAvailabilityReason() : "");

        // Update performance chart
        updatePerformanceChart(player);

        // Load team data if player has a team
        if (player.getTeamId() != null) {
            loadTeamData(player.getTeamId());
        } else {
            clearTeamData();
        }
    }

    private void updatePerformanceChart(Player player) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Player Statistics");

        series.getData().add(new XYChart.Data<>("Kills", player.getTotalKills()));
        series.getData().add(new XYChart.Data<>("Deaths", player.getTotalDeaths()));
        series.getData().add(new XYChart.Data<>("Assists", player.getTotalAssists()));
        series.getData().add(new XYChart.Data<>("Wins", player.getMatchesWon()));

        performanceChart.getData().clear();
        performanceChart.getData().add(series);
    }

    private void loadTeamData(int teamId) {
        Task<Team> task = new Task<>() {
            @Override
            protected Team call() {
                return teamDAO.getTeamById(teamId);
            }
        };

        task.setOnSucceeded(e -> {
            currentTeam = task.getValue();
            if (currentTeam != null) {
                updateTeamInfo(currentTeam);
                loadTeamMembers(teamId);
                loadVotingData(teamId);
            }
        });

        task.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load team data"));

        new Thread(task).start();
    }

    private void updateTeamInfo(Team team) {
        playerTeamLabel.setText(team.getName());
        myTeamNameLabel.setText(team.getName() + " [" + team.getTag() + "]");
        teamWinsLabel.setText(String.valueOf(team.getWins()));
        teamLossesLabel.setText(String.valueOf(team.getLosses()));

        // Get and display team leader
        Task<Player> leaderTask = new Task<>() {
            @Override
            protected Player call() {
                if (team.getLeaderId() != null) {
                    return playerDAO.getPlayerById(team.getLeaderId());
                }
                return null;
            }
        };

        leaderTask.setOnSucceeded(e -> {
            Player leader = leaderTask.getValue();
            teamLeaderLabel.setText(leader != null ? leader.getUsername() : "No leader");
        });

        new Thread(leaderTask).start();
    }

    private void loadTeamMembers(int teamId) {
        Task<List<Player>> task = new Task<>() {
            @Override
            protected List<Player> call() {
                return playerDAO.getPlayersByTeam(teamId);
            }
        };

        task.setOnSucceeded(e -> {
            List<Player> members = task.getValue();
            teamMembers.setAll(members);
            voteCandidates.setAll(members);
        });

        new Thread(task).start();
    }

    private void loadVotingData(int teamId) {
        Task<Map<Integer, Integer>> task = new Task<>() {
            @Override
            protected Map<Integer, Integer> call() {
                return voteDAO.getVoteCounts(teamId);
            }
        };

        task.setOnSucceeded(e -> {
            boolean hasVoted = voteDAO.hasVoted(teamId, currentPlayer.getId());
            votingStatusLabel.setText(hasVoted ?
                    "✓ You have already voted" : "You haven't voted yet");
            castVoteBtn.setDisable(hasVoted);
        });

        new Thread(task).start();
    }

    private void clearTeamData() {
        playerTeamLabel.setText("No Team");
        myTeamNameLabel.setText("Not in a team");
        teamWinsLabel.setText("0");
        teamLossesLabel.setText("0");
        teamLeaderLabel.setText("N/A");
        teamMembers.clear();
        voteCandidates.clear();
        votingStatusLabel.setText("");
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

                // Refresh team members list if in a team
                if (currentPlayer.getTeamId() != null) {
                    loadTeamMembers(currentPlayer.getTeamId());
                }
            } else {
                MainApp.showError("Error", "Failed to update availability");
            }
        });

        task.setOnFailed(e ->
                MainApp.showError("Error", "Failed to update availability"));

        new Thread(task).start();
    }

    @FXML
    private void handleCastVote() {
        if (currentPlayer == null || currentTeam == null) {
            MainApp.showError("Error", "No player or team selected");
            return;
        }

        Player candidate = voteCandidatesList.getSelectionModel().getSelectedItem();
        if (candidate == null) {
            MainApp.showError("No Selection", "Please select a candidate to vote for");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Vote");
        confirm.setHeaderText("Vote for Team Leader");
        confirm.setContentText("Vote for: " + candidate.getUsername() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                castVote(candidate);
            }
        });
    }

    private void castVote(Player candidate) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return voteDAO.castVote(currentTeam.getId(), currentPlayer.getId(),
                        candidate.getId());
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                MainApp.showInfo("Success", "Vote cast successfully!");
                loadVotingData(currentTeam.getId());
                updateTeamLeaderFromVotes();
            } else {
                MainApp.showError("Error", "Failed to cast vote");
            }
        });

        task.setOnFailed(e ->
                MainApp.showError("Error", "Failed to cast vote"));

        new Thread(task).start();
    }

    private void updateTeamLeaderFromVotes() {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return voteDAO.getCurrentLeader(currentTeam.getId());
            }
        };

        task.setOnSucceeded(e -> {
            Integer leaderId = task.getValue();
            if (leaderId != null) {
                teamDAO.updateTeamLeader(currentTeam.getId(), leaderId);
                loadTeamData(currentTeam.getId());
            }
        });

        new Thread(task).start();
    }

    @FXML
    private void handleViewVotingResults() {
        if (currentTeam == null) {
            MainApp.showError("Error", "No team selected");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Voting Results - " + currentTeam.getName());
        dialog.setHeaderText("Current Leader Election Results");

        TableView<VoteResultDisplay> resultsTable = new TableView<>();
        resultsTable.setPrefWidth(400);
        resultsTable.setPrefHeight(300);

        TableColumn<VoteResultDisplay, String> nameCol = new TableColumn<>("Candidate");
        nameCol.setPrefWidth(250);
        nameCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(() -> cellData.getValue().name));

        TableColumn<VoteResultDisplay, Integer> votesCol = new TableColumn<>("Votes");
        votesCol.setPrefWidth(100);
        votesCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createIntegerBinding(() -> cellData.getValue().votes).asObject());

        resultsTable.getColumns().addAll(nameCol, votesCol);

        Task<Map<Integer, Integer>> task = new Task<>() {
            @Override
            protected Map<Integer, Integer> call() {
                return voteDAO.getVoteCounts(currentTeam.getId());
            }
        };

        task.setOnSucceeded(e -> {
            Map<Integer, Integer> voteCounts = task.getValue();
            ObservableList<VoteResultDisplay> results = FXCollections.observableArrayList();

            for (Player p : teamMembers) {
                int votes = voteCounts.getOrDefault(p.getId(), 0);
                results.add(new VoteResultDisplay(p.getUsername(), votes));
            }

            results.sort((a, b) -> Integer.compare(b.votes, a.votes));
            resultsTable.setItems(results);
        });

        new Thread(task).start();

        dialog.getDialogPane().setContent(resultsTable);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    private void handleBackToMenu() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Return to Main Menu");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("Return to the main menu?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                mainApp.showMainMenu();
            }
        });
    }

    // Helper class for displaying vote results
    private static class VoteResultDisplay {
        String name;
        int votes;

        VoteResultDisplay(String name, int votes) {
            this.name = name;
            this.votes = votes;
        }
    }
}