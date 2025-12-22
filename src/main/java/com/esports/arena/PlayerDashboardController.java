package com.esports.arena;

import java.util.List;
import java.util.Map;

import com.esports.arena.dao.LeaderVoteDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.dao.TournamentDAO;
import com.esports.arena.model.Player;
import com.esports.arena.model.Team;
import com.esports.arena.model.Tournament;
import com.esports.arena.service.TournamentStatsService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
@SuppressWarnings("unused")
public class PlayerDashboardController {
    // Tab Pane
    @FXML private TabPane playerTabPane;

    // Header
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
    @FXML private javafx.scene.control.ComboBox<Tournament> tournamentCombo;
    @FXML private Label tournamentKillsLabel;
    @FXML private Label tournamentDeathsLabel;
    @FXML private Label tournamentAssistsLabel;
    @FXML private Label tournamentMatchesPlayedLabel;
    @FXML private Label tournamentMatchesWonLabel;
    @FXML private Label tournamentKDLabel;
    @FXML private Label tournamentWinRateLabel;
    @FXML private BarChart<String, Number> performanceChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Spinner<Integer> killsSpinner;
    @FXML private Spinner<Integer> deathsSpinner;
    @FXML private Spinner<Integer> assistsSpinner;
    @FXML private CheckBox matchWonCheckBox;

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
    private TournamentDAO tournamentDAO;
    private TournamentStatsService tournamentStatsService;
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

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    @FXML
    private void initialize() {
        playerDAO = new PlayerDAO();
        teamDAO = new TeamDAO();
        voteDAO = new LeaderVoteDAO();
        tournamentDAO = new TournamentDAO();
        tournamentStatsService = new TournamentStatsService();

        allPlayers = FXCollections.observableArrayList();
        teamMembers = FXCollections.observableArrayList();
        voteCandidates = FXCollections.observableArrayList();

        setupAvailabilityToggle();
        setupListViews();
        setupStatsInputs();
        setupTournamentCombo();

        loadAllPlayers();
        loadTournaments();

        // If currentPlayer was set from login, display it
        if (currentPlayer != null) {
            loadPlayerData(currentPlayer);
        }

        // Refresh player data when tab is selected to get latest team assignment
        if (playerTabPane != null) {
            playerTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (currentPlayer != null && newTab != null) {
                    refreshPlayerData();
                }
            });
        }
    }

    private void setupTournamentCombo() {
        if (tournamentCombo != null) {
            tournamentCombo.setCellFactory(param -> new ListCell<Tournament>() {
                @Override
                protected void updateItem(Tournament item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Overall Stats" : item.getName());
                }
            });

            tournamentCombo.setButtonCell(new ListCell<Tournament>() {
                @Override
                protected void updateItem(Tournament item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Overall Stats" : item.getName());
                }
            });

            tournamentCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (currentPlayer != null) {
                    loadTournamentStats(newVal);
                }
            });
        }
    }

    private void loadTournaments() {
        Task<List<Tournament>> task = new Task<>() {
            @Override
            protected List<Tournament> call() {
                return tournamentDAO.getAllTournaments();
            }
        };

        task.setOnSucceeded(e -> {
            if (tournamentCombo != null) {
                List<Tournament> tournaments = task.getValue();
                tournamentCombo.getItems().clear();
                tournamentCombo.getItems().add(null); // Add null for "Overall Stats"
                tournamentCombo.getItems().addAll(tournaments);
                tournamentCombo.getSelectionModel().select(0); // Select "Overall Stats"
            }
        });

        task.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load tournaments"));

        new Thread(task).start();
    }

    private void loadTournamentStats(Tournament tournament) {
        if (currentPlayer == null) return;

        if (tournament == null) {
            // Show overall stats
            if (currentPlayer != null) {
                tournamentKillsLabel.setText(String.valueOf(currentPlayer.getTotalKills()));
                tournamentDeathsLabel.setText(String.valueOf(currentPlayer.getTotalDeaths()));
                tournamentAssistsLabel.setText(String.valueOf(currentPlayer.getTotalAssists()));
                tournamentMatchesPlayedLabel.setText(String.valueOf(currentPlayer.getMatchesPlayed()));
                tournamentMatchesWonLabel.setText(String.valueOf(currentPlayer.getMatchesWon()));
                tournamentKDLabel.setText(String.format("%.2f", currentPlayer.getKdRatio()));
                tournamentWinRateLabel.setText(String.format("%.1f%%", currentPlayer.getWinRate()));
            }
            return;
        }

        Task<TournamentStatsService.TournamentPlayerStats> task = new Task<>() {
            @Override
            protected TournamentStatsService.TournamentPlayerStats call() {
                return tournamentStatsService.getPlayerTournamentStats(currentPlayer.getId(), tournament.getId());
            }
        };

        task.setOnSucceeded(e -> {
            TournamentStatsService.TournamentPlayerStats stats = task.getValue();
            tournamentKillsLabel.setText(String.valueOf(stats.kills));
            tournamentDeathsLabel.setText(String.valueOf(stats.deaths));
            tournamentAssistsLabel.setText(String.valueOf(stats.assists));
            tournamentMatchesPlayedLabel.setText(String.valueOf(stats.matchesPlayed));
            tournamentMatchesWonLabel.setText(String.valueOf(stats.matchesWon));
            tournamentKDLabel.setText(String.format("%.2f", stats.getKdRatio()));
            tournamentWinRateLabel.setText(String.format("%.1f%%", stats.getWinRate()));
        });

        task.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load tournament stats"));

        new Thread(task).start();
    }

    public void refreshPlayerData() {
        if (currentPlayer != null) {
            Task<Player> refreshTask = new Task<>() {
                @Override
                protected Player call() {
                    return playerDAO.getPlayerById(currentPlayer.getId());
                }
            };

            refreshTask.setOnSucceeded(e -> {
                Player refreshedPlayer = refreshTask.getValue();
                if (refreshedPlayer != null) {
                    loadPlayerData(refreshedPlayer);
                }
            });

            refreshTask.setOnFailed(e ->
                    MainApp.showError("Error", "Failed to refresh player data"));

            new Thread(refreshTask).start();
        }
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

        // Load tournament stats (will show overall if no tournament selected)
        if (tournamentCombo != null && tournamentCombo.getSelectionModel().getSelectedItem() != null) {
            loadTournamentStats(tournamentCombo.getSelectionModel().getSelectedItem());
        } else {
            loadTournamentStats(null); // Show overall stats
        }

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

        resultsTable.getColumns().add(nameCol);
        resultsTable.getColumns().add(votesCol);

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

    private void setupStatsInputs() {
        if (killsSpinner != null) {
            killsSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 0));
        }
        if (deathsSpinner != null) {
            deathsSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 0));
        }
        if (assistsSpinner != null) {
            assistsSpinner.setValueFactory(new javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1000, 0));
        }
        if (matchWonCheckBox != null) {
            matchWonCheckBox.setSelected(false);
        }
    }

    @FXML
    private void handleRecordMatch() {
        if (currentPlayer == null) {
            MainApp.showError("Error", "No player selected");
            return;
        }

        Integer kVal = killsSpinner != null ? killsSpinner.getValue() : null;
        Integer dVal = deathsSpinner != null ? deathsSpinner.getValue() : null;
        Integer aVal = assistsSpinner != null ? assistsSpinner.getValue() : null;
        int kills = kVal == null ? 0 : kVal;
        int deaths = dVal == null ? 0 : dVal;
        int assists = aVal == null ? 0 : aVal;
        boolean won = matchWonCheckBox != null && matchWonCheckBox.isSelected();

        Task<Boolean> updateTask = new Task<>() {
            @Override
            protected Boolean call() {
                return playerDAO.updatePlayerStats(currentPlayer.getId(), kills, deaths, assists, won);
            }
        };

        updateTask.setOnSucceeded(e -> {
            if (updateTask.getValue()) {
                MainApp.showInfo("Success", "Match stats recorded successfully!");
                if (killsSpinner != null) killsSpinner.getValueFactory().setValue(0);
                if (deathsSpinner != null) deathsSpinner.getValueFactory().setValue(0);
                if (assistsSpinner != null) assistsSpinner.getValueFactory().setValue(0);
                if (matchWonCheckBox != null) matchWonCheckBox.setSelected(false);

                Player updatedPlayer = playerDAO.getPlayerById(currentPlayer.getId());
                if (updatedPlayer != null) {
                    loadPlayerData(updatedPlayer);
                }
            } else {
                MainApp.showError("Error", "Failed to record match stats");
            }
        });

        updateTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to record match stats: " + updateTask.getException().getMessage()));

        new Thread(updateTask).start();
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