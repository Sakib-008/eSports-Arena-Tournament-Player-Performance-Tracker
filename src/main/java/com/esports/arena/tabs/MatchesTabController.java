package com.esports.arena.tabs;

import java.util.List;

import com.esports.arena.MainApp;
import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.util.LoadingDialog;
import com.esports.arena.model.Match;
import com.esports.arena.model.Player;
import com.esports.arena.model.PlayerMatchStats;
import com.esports.arena.model.Team;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MatchesTabController {
    @FXML private TableView<Match> matchesTable;
    @FXML private TableColumn<Match, Integer> matchIdCol;
    @FXML private TableColumn<Match, String> matchTeam1Col;
    @FXML private TableColumn<Match, String> matchTeam2Col;
    @FXML private TableColumn<Match, String> matchScoreCol;
    @FXML private TableColumn<Match, String> matchStatusCol;
    @FXML private TableColumn<Match, String> matchRoundCol;
    @FXML private Button viewMatchDetailsBtn;
    @FXML private Button editMatchStatsBtn;
    @FXML private ComboBox<com.esports.arena.model.Tournament> tournamentFilterCombo;

    private MatchDAO matchDAO;
    private com.esports.arena.dao.TournamentDAO tournamentDAO;
    private TeamDAO teamDAO;
    private PlayerDAO playerDAO;
    private ObservableList<Match> matchesData;
    private ObservableList<Team> teamsData;

    private record MatchDetailData(Match match, Team team1, Team team2,
                                   List<Player> playersTeam1, List<Player> playersTeam2,
                                   List<com.esports.arena.model.PlayerMatchStats> stats) { }
    
    private record PlayerStatsInput(int playerId, int kills, int deaths, int assists) { }

    public void initialize(MatchDAO matchDAO, TeamDAO teamDAO, PlayerDAO playerDAO, ObservableList<Team> teamsData) {
        this.matchDAO = matchDAO;
        this.teamDAO = teamDAO;
        this.playerDAO = playerDAO;
        this.teamsData = teamsData;
        this.tournamentDAO = new com.esports.arena.dao.TournamentDAO();
        this.matchesData = FXCollections.observableArrayList();
        setupMatchesTable();
        setupTournamentFilter();
        refreshTournamentFilter();
        loadAllMatches();
    }

    private void setupTournamentFilter() {
        if (tournamentFilterCombo != null) {
            tournamentFilterCombo.setCellFactory(param -> new ListCell<com.esports.arena.model.Tournament>() {
                @Override
                protected void updateItem(com.esports.arena.model.Tournament item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName() + " (" + item.getStatus() + ")");
                }
            });

            tournamentFilterCombo.setButtonCell(new ListCell<com.esports.arena.model.Tournament>() {
                @Override
                protected void updateItem(com.esports.arena.model.Tournament item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "All Tournaments" : item.getName());
                }
            });

            tournamentFilterCombo.setOnAction(e -> {
                com.esports.arena.model.Tournament selected = tournamentFilterCombo.getValue();
                if (selected != null) {
                    loadMatchesForTournament(selected.getId());
                }
            });
        }
    }

    public void refreshTournamentFilter() {
        if (tournamentFilterCombo == null) {
            return;
        }

        Task<List<com.esports.arena.model.Tournament>> task = new Task<>() {
            @Override
            protected List<com.esports.arena.model.Tournament> call() {
                return tournamentDAO.getAllTournaments();
            }
        };

        task.setOnSucceeded(e -> {
            List<com.esports.arena.model.Tournament> tournaments = task.getValue();
            com.esports.arena.model.Tournament previouslySelected = tournamentFilterCombo.getValue();
            tournamentFilterCombo.setItems(FXCollections.observableArrayList(tournaments));
            if (previouslySelected != null) {
                tournamentFilterCombo.getItems().stream()
                        .filter(t -> t.getId() == previouslySelected.getId())
                        .findFirst()
                        .ifPresent(tournamentFilterCombo::setValue);
            }
        });

        task.setOnFailed(e -> MainApp.showError("Error", "Failed to load tournaments for filter: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    private void setupMatchesTable() {
        matchIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        matchTeam1Col.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (match == null) {
                    return "Unknown";
                }
                Team team = findTeamInCache(match.getTeam1Id());
                return team != null ? team.getName() : "Team " + match.getTeam1Id();
            });
        });

        matchTeam2Col.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(() -> {
                if (match == null) {
                    return "Unknown";
                }
                Team team = findTeamInCache(match.getTeam2Id());
                return team != null ? team.getName() : "Team " + match.getTeam2Id();
            });
        });

        matchScoreCol.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> match != null ? match.getScoreDisplay() : "0 - 0");
        });

        matchStatusCol.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> match != null ? match.getStatus().toString() : "UNKNOWN");
        });

        matchRoundCol.setCellValueFactory(new PropertyValueFactory<>("round"));

        matchesTable.setItems(matchesData);

        // Disable edit button for completed matches
        matchesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getStatus() == Match.MatchStatus.COMPLETED) {
                editMatchStatsBtn.setDisable(true);
                editMatchStatsBtn.setStyle("-fx-opacity: 0.5;");
            } else {
                editMatchStatsBtn.setDisable(false);
                editMatchStatsBtn.setStyle("");
            }
        });
    }

    private Team findTeamInCache(int teamId) {
        if (teamsData == null) {
            return null;
        }
        for (Team team : teamsData) {
            if (team.getId() == teamId) {
                return team;
            }
        }
        return null;
    }

    public void loadMatchesForTournament(int tournamentId) {
        LoadingDialog.showLoading("Loading matches...");
        Task<List<Match>> task = new Task<>() {
            @Override
            protected List<Match> call() {
                List<Match> matches = matchDAO.getMatchesByTournament(tournamentId);
                return matches != null ? matches : new java.util.ArrayList<>();
            }
        };

        task.setOnSucceeded(e -> {
            List<Match> matches = task.getValue();
            matchesData.setAll(matches);
            LoadingDialog.hideLoading();
        });

        task.setOnFailed(e -> {
            MainApp.showError("Error", "Failed to load matches: " + task.getException().getMessage());
            matchesData.clear();
            LoadingDialog.hideLoading();
        });

        new Thread(task).start();
    }

    @FXML
    private void handleShowAllMatches() {
        if (tournamentFilterCombo != null) {
            tournamentFilterCombo.setValue(null);
        }
        loadAllMatches();
    }

    public void loadAllMatches() {
        LoadingDialog.showLoading("Loading all matches...");
        Task<List<Match>> task = new Task<>() {
            @Override
            protected List<Match> call() {
                return matchDAO.getAllMatches();
            }
        };

        task.setOnSucceeded(e -> {
            List<Match> matches = task.getValue();
            matchesData.setAll(matches);
            if (matches.isEmpty()) {
                System.out.println("No matches found in database. Create matches from tournaments.");
            }
            LoadingDialog.hideLoading();
        });

        task.setOnFailed(e -> {
            MainApp.showError("Error", "Failed to load matches: " + task.getException().getMessage());
            matchesData.clear();
            LoadingDialog.hideLoading();
        });

        new Thread(task).start();
    }

    public void updateMatchesList() {
        System.out.println("MatchesTabController.updateMatchesList() called");
        LoadingDialog.showLoading("Refreshing matches...");
        Task<List<Match>> task = new Task<>() {
            @Override
            protected List<Match> call() {
                System.out.println("  MatchesTabController - Loading all matches");
                return matchDAO.getAllMatches();
            }
        };
        
        task.setOnSucceeded(e -> {
            List<Match> matches = task.getValue();
            System.out.println("  MatchesTabController - Loaded " + (matches != null ? matches.size() : 0) + " matches");
            if (matches != null) {
                matchesData.setAll(matches);
            }
            LoadingDialog.hideLoading();
        });
        
        task.setOnFailed(e -> {
            System.err.println("  MatchesTabController - Load failed");
            LoadingDialog.hideLoading();
        });
        
        new Thread(task).start();
    }

    @FXML
    private void handleViewMatchDetails() {
        Match selected = matchesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a match to view details");
            return;
        }

        LoadingDialog.showLoading("Loading match details...");
        Task<MatchDetailData> loadTask = new Task<>() {
            @Override
            protected MatchDetailData call() {
                Match match = matchDAO.getMatchById(selected.getId());
                if (match == null) {
                    throw new IllegalStateException("Match not found");
                }
                Team team1 = teamDAO.getTeamById(match.getTeam1Id());
                Team team2 = teamDAO.getTeamById(match.getTeam2Id());
                List<Player> playersTeam1 = playerDAO.getPlayersByTeam(match.getTeam1Id());
                List<Player> playersTeam2 = playerDAO.getPlayersByTeam(match.getTeam2Id());
                List<com.esports.arena.model.PlayerMatchStats> stats = matchDAO.getPlayerStatsByMatch(match.getId());
                return new MatchDetailData(match, team1, team2, playersTeam1, playersTeam2, stats);
            }
        };

        loadTask.setOnSucceeded(evt -> {
            LoadingDialog.hideLoading();
            MatchDetailData data = loadTask.getValue();

            System.out.println("Viewing match details for match ID: " + data.match().getId());
            System.out.println("Found " + data.stats().size() + " player stats entries");
            for (com.esports.arena.model.PlayerMatchStats stat : data.stats()) {
                System.out.println("  Player " + stat.getPlayerId() + ": K=" + stat.getKills() + " D=" + stat.getDeaths() + " A=" + stat.getAssists());
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Match Details - " + data.match().getId());
            dialog.setHeaderText("View Match Details and Player Stats");

            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            VBox content = new VBox(12);
            content.setPrefWidth(700);

            Label teamsLabel = new Label((data.team1() != null ? data.team1().getName() : "Team 1") + " vs " + (data.team2() != null ? data.team2().getName() : "Team 2"));
            teamsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            HBox scoreBox = new HBox(10);
            Label team1ScoreLabel = new Label("Score: " + data.match().getTeam1Score() + " - " + data.match().getTeam2Score());
            team1ScoreLabel.setStyle("-fx-font-size: 12px;");
            Label statusLabel = new Label("Status: " + data.match().getStatus().toString());
            statusLabel.setStyle("-fx-font-size: 12px;");

            scoreBox.getChildren().addAll(team1ScoreLabel, statusLabel);

            content.getChildren().addAll(teamsLabel, scoreBox, new Separator());

            VBox playersContainer = new VBox(10);

            if (!data.playersTeam1().isEmpty()) {
                Label team1Label = new Label((data.team1() != null ? data.team1().getName() : "Team 1") + " Players");
                team1Label.setStyle("-fx-font-weight: bold;");
                playersContainer.getChildren().add(team1Label);
                for (Player p : data.playersTeam1()) {
                    com.esports.arena.model.PlayerMatchStats pStats = data.stats().stream()
                            .filter(s -> s.getPlayerId() == p.getId())
                            .findFirst().orElse(null);

                    int kills = pStats != null ? pStats.getKills() : 0;
                    int deaths = pStats != null ? pStats.getDeaths() : 0;
                    int assists = pStats != null ? pStats.getAssists() : 0;

                    System.out.println("Team1 Player " + p.getUsername() + " (ID:" + p.getId() + "): Found stats=" + (pStats != null) + ", K=" + kills + " D=" + deaths + " A=" + assists);

                    HBox row = new HBox(15);
                    Label nameLabel = new Label(p.getUsername());
                    nameLabel.setPrefWidth(150);
                    row.getChildren().addAll(
                        nameLabel,
                        new Label("Kills: " + kills),
                        new Label("Deaths: " + deaths),
                        new Label("Assists: " + assists)
                    );
                    playersContainer.getChildren().add(row);
                }
            }

            if (!data.playersTeam2().isEmpty()) {
                playersContainer.getChildren().add(new Separator());
                Label team2Label = new Label((data.team2() != null ? data.team2().getName() : "Team 2") + " Players");
                team2Label.setStyle("-fx-font-weight: bold;");
                playersContainer.getChildren().add(team2Label);
                for (Player p : data.playersTeam2()) {
                    com.esports.arena.model.PlayerMatchStats pStats = data.stats().stream()
                            .filter(s -> s.getPlayerId() == p.getId())
                            .findFirst().orElse(null);

                    int kills = pStats != null ? pStats.getKills() : 0;
                    int deaths = pStats != null ? pStats.getDeaths() : 0;
                    int assists = pStats != null ? pStats.getAssists() : 0;

                    System.out.println("Team2 Player " + p.getUsername() + " (ID:" + p.getId() + "): Found stats=" + (pStats != null) + ", K=" + kills + " D=" + deaths + " A=" + assists);

                    HBox row = new HBox(15);
                    Label nameLabel = new Label(p.getUsername());
                    nameLabel.setPrefWidth(150);
                    row.getChildren().addAll(
                        nameLabel,
                        new Label("Kills: " + kills),
                        new Label("Deaths: " + deaths),
                        new Label("Assists: " + assists)
                    );
                    playersContainer.getChildren().add(row);
                }
            }

            ScrollPane scroll = new ScrollPane(playersContainer);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(450);

            content.getChildren().add(scroll);

            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();
        });

        loadTask.setOnFailed(evt -> {
            LoadingDialog.hideLoading();
            MainApp.showError("Error", "Failed to load match details: " + evt.getSource().getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    @FXML
    private void handleEditMatchStats() {
        Match selected = matchesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a match to edit");
            return;
        }

        LoadingDialog.showLoading("Loading match...");
        Task<MatchDetailData> loadTask = new Task<>() {
            @Override
            protected MatchDetailData call() {
                Match match = matchDAO.getMatchById(selected.getId());
                if (match == null) {
                    throw new IllegalStateException("Match not found");
                }
                Team team1 = teamDAO.getTeamById(match.getTeam1Id());
                Team team2 = teamDAO.getTeamById(match.getTeam2Id());
                List<Player> playersTeam1 = playerDAO.getPlayersByTeam(match.getTeam1Id());
                List<Player> playersTeam2 = playerDAO.getPlayersByTeam(match.getTeam2Id());
                return new MatchDetailData(match, team1, team2, playersTeam1, playersTeam2, java.util.Collections.emptyList());
            }
        };

        loadTask.setOnSucceeded(evt -> {
            LoadingDialog.hideLoading();
            MatchDetailData data = loadTask.getValue();

            if (data.match().getStatus() == Match.MatchStatus.COMPLETED) {
                MainApp.showError("Match Completed", "Cannot edit statistics for matches that are already completed");
                return;
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Match Details - " + data.match().getId());
            dialog.setHeaderText("Match Details and Player Stats");
            dialog.getDialogPane().setPrefSize(900, 700);
            dialog.getDialogPane().setMinSize(800, 600);

            ButtonType finalizeButtonType = new ButtonType("Finalize Match", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(finalizeButtonType, ButtonType.CLOSE);

            VBox content = new VBox(12);
            content.setPrefWidth(850);

            Label teamsLabel = new Label((data.team1() != null ? data.team1().getName() : "Team 1") + " vs " + (data.team2() != null ? data.team2().getName() : "Team 2"));
            teamsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            HBox scoreBox = new HBox(10);
            TextField team1ScoreField = new TextField(String.valueOf(data.match().getTeam1Score()));
            team1ScoreField.setPrefWidth(80);
            TextField team2ScoreField = new TextField(String.valueOf(data.match().getTeam2Score()));
            team2ScoreField.setPrefWidth(80);

            scoreBox.getChildren().addAll(new Label(data.team1() != null ? data.team1().getName() : "Team 1"), team1ScoreField,
                    new Label("-"), team2ScoreField, new Label(data.team2() != null ? data.team2().getName() : "Team 2"));

            content.getChildren().addAll(teamsLabel, scoreBox, new Separator());

            List<Player> playersTeam1 = data.playersTeam1();
            List<Player> playersTeam2 = data.playersTeam2();

            VBox playersContainer = new VBox(10);

            class PlayerInputs {
                int playerId;
                TextField kills = new TextField("0");
                TextField deaths = new TextField("0");
                TextField assists = new TextField("0");
            }

            List<PlayerInputs> inputs = new java.util.ArrayList<>();

            if (!playersTeam1.isEmpty()) {
                playersContainer.getChildren().add(new Label((data.team1() != null ? data.team1().getName() : "Team 1") + " Players"));
                for (Player p : playersTeam1) {
                    PlayerInputs pi = new PlayerInputs();
                    pi.playerId = p.getId();
                    HBox row = new HBox(8);
                    String statusIndicator = p.isAvailable() ? "AVAILABLE" : "UNAVAILABLE";
                    row.getChildren().addAll(
                            new Label(p.getUsername() + " [" + statusIndicator + "]"), 
                            new Label("Kills:"), pi.kills,
                            new Label("Deaths:"), pi.deaths, 
                            new Label("Assists:"), pi.assists);
                    if (!p.isAvailable()) {
                        row.setStyle("-fx-opacity: 0.6;");
                        pi.kills.setDisable(true);
                        pi.deaths.setDisable(true);
                        pi.assists.setDisable(true);
                    }
                    playersContainer.getChildren().add(row);
                    inputs.add(pi);
                }
            }

            if (!playersTeam2.isEmpty()) {
                playersContainer.getChildren().add(new Label((data.team2() != null ? data.team2().getName() : "Team 2") + " Players"));
                for (Player p : playersTeam2) {
                    PlayerInputs pi = new PlayerInputs();
                    pi.playerId = p.getId();
                    HBox row = new HBox(8);
                    String statusIndicator = p.isAvailable() ? "AVAILABLE" : "UNAVAILABLE";
                    row.getChildren().addAll(
                            new Label(p.getUsername() + " [" + statusIndicator + "]"), 
                            new Label("Kills:"), pi.kills,
                            new Label("Deaths:"), pi.deaths, 
                            new Label("Assists:"), pi.assists);
                    if (!p.isAvailable()) {
                        row.setStyle("-fx-opacity: 0.6;");
                        pi.kills.setDisable(true);
                        pi.deaths.setDisable(true);
                        pi.assists.setDisable(true);
                    }
                    playersContainer.getChildren().add(row);
                    inputs.add(pi);
                }
            }

            ScrollPane scroll = new ScrollPane(playersContainer);
            scroll.setFitToWidth(true);
            scroll.setPrefHeight(300);

            content.getChildren().add(scroll);

            dialog.getDialogPane().setContent(content);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == finalizeButtonType) {
                    try {
                        int team1Score = Integer.parseInt(team1ScoreField.getText());
                        int team2Score = Integer.parseInt(team2ScoreField.getText());

                        boolean winnerDecided = team1Score != team2Score;
                        if (!winnerDecided) {
                            MainApp.showError("Invalid Score", "Scores cannot be tied for a completed match");
                            return null;
                        }

                        // Collect input data before closing dialog
                        List<PlayerStatsInput> statsInputs = new java.util.ArrayList<>();
                        for (PlayerInputs pi : inputs) {
                            try {
                                int kills = Integer.parseInt(pi.kills.getText());
                                int deaths = Integer.parseInt(pi.deaths.getText());
                                int assists = Integer.parseInt(pi.assists.getText());
                                statsInputs.add(new PlayerStatsInput(pi.playerId, kills, deaths, assists));
                            } catch (NumberFormatException ex) {
                                MainApp.showError("Invalid Input", "Please enter valid numeric values for all player stats");
                                return null;
                            }
                        }

                        // Run finalization in background
                        javafx.application.Platform.runLater(() -> {
                            LoadingDialog.showLoading("Finalizing match and updating stats...");
                            Task<Boolean> finalizeTask = new Task<>() {
                                @Override
                                protected Boolean call() {
                                    try {
                                        Match match = data.match();
                                        Match.MatchStatus previousStatus = match.getStatus();
                                        Integer previousWinner = match.getWinnerId();

                                        match.setTeam1Score(team1Score);
                                        match.setTeam2Score(team2Score);
                                        match.setStatus(Match.MatchStatus.COMPLETED);

                                        int winningTeamId = team1Score > team2Score ? match.getTeam1Id() : match.getTeam2Id();
                                        int losingTeamId = winningTeamId == match.getTeam1Id() ? match.getTeam2Id() : match.getTeam1Id();
                                        match.setWinnerId(winningTeamId);
                                        boolean shouldUpdateTeamRecords = previousStatus != Match.MatchStatus.COMPLETED || previousWinner == null;

                                        System.out.println("Updating match stats for match ID: " + match.getId());
                                        System.out.println("Should update records: " + shouldUpdateTeamRecords);

                                        // Update player match stats and career stats
                                        for (PlayerStatsInput psi : statsInputs) {
                                            System.out.println("Updating stats for player " + psi.playerId + ": K=" + psi.kills + " D=" + psi.deaths + " A=" + psi.assists);
                                            matchDAO.updatePlayerStats(match.getId(), psi.playerId, psi.kills, psi.deaths, psi.assists);

                                            boolean onWinningTeam = (winningTeamId == match.getTeam1Id() && playersTeam1.stream().anyMatch(p -> p.getId() == psi.playerId))
                                                    || (winningTeamId == match.getTeam2Id() && playersTeam2.stream().anyMatch(p -> p.getId() == psi.playerId));
                                            if (shouldUpdateTeamRecords) {
                                                System.out.println("Updating career stats for player " + psi.playerId + ", won: " + onWinningTeam);
                                                playerDAO.updatePlayerStats(psi.playerId, psi.kills, psi.deaths, psi.assists, onWinningTeam);
                                            }
                                        }

                                        // Update team records
                                        if (shouldUpdateTeamRecords) {
                                            System.out.println("Updating team records: Winner=" + winningTeamId + ", Loser=" + losingTeamId);
                                            teamDAO.updateTeamRecord(winningTeamId, true, false);
                                            teamDAO.updateTeamRecord(losingTeamId, false, false);
                                        }

                                        // Reload match from database to ensure all playerStats are there
                                        Match reloadedMatch = matchDAO.getMatchById(match.getId());
                                        System.out.println("Match reloaded from DB - player stats count: " + (reloadedMatch.getPlayerStats() != null ? reloadedMatch.getPlayerStats().size() : 0));
                                        if (reloadedMatch.getPlayerStats() != null && !reloadedMatch.getPlayerStats().isEmpty()) {
                                            for (PlayerMatchStats pms : reloadedMatch.getPlayerStats()) {
                                                System.out.println("  Stored stat - Player: " + pms.getPlayerId() + ", K=" + pms.getKills() + " D=" + pms.getDeaths() + " A=" + pms.getAssists());
                                            }
                                        }

                                        // Copy the scores and status to the reloaded match
                                        System.out.println("Before merge - reloaded match status: " + reloadedMatch.getStatus() + ", score: " + reloadedMatch.getTeam1Score() + "-" + reloadedMatch.getTeam2Score());
                                        reloadedMatch.setTeam1Score(match.getTeam1Score());
                                        reloadedMatch.setTeam2Score(match.getTeam2Score());
                                        reloadedMatch.setStatus(match.getStatus());
                                        reloadedMatch.setWinnerId(match.getWinnerId());
                                        System.out.println("After merge - reloaded match status: " + reloadedMatch.getStatus() + ", score: " + reloadedMatch.getTeam1Score() + "-" + reloadedMatch.getTeam2Score());

                                        // Save match again to ensure all changes are persisted
                                        System.out.println("Final match save - player stats count before save: " + (reloadedMatch.getPlayerStats() != null ? reloadedMatch.getPlayerStats().size() : 0));
                                        matchDAO.updateMatch(reloadedMatch);
                                        System.out.println("Match finalization complete!");
                                        return true;
                                    } catch (Exception ex) {
                                        System.err.println("EXCEPTION DURING MATCH FINALIZATION: " + ex.getMessage());
                                        ex.printStackTrace();
                                        return false;
                                    }
                                }
                            };

                            finalizeTask.setOnSucceeded(e -> {
                                LoadingDialog.hideLoading();
                                    if (finalizeTask.getValue()) {
                                        System.out.println("Match finalization succeeded!");
                                        loadMatchesForTournament(data.match().getTournamentId());
                                        if (teamsData != null) {
                                            // Refresh team data
                                            Task<List<Team>> refreshTeamsTask = new Task<>() {
                                                @Override
                                                protected List<Team> call() {
                                                    return teamDAO.getAllTeams();
                                                }
                                            };
                                            refreshTeamsTask.setOnSucceeded(ev -> teamsData.setAll(refreshTeamsTask.getValue()));
                                            new Thread(refreshTeamsTask).start();
                                        }
                                        MainApp.showInfo("Success", "Match finalized and stats saved");
                                    } else {
                                        System.err.println("Match finalization returned false!");
                                        MainApp.showError("Error", "Match finalization failed - check logs");
                                }
                            });

                            finalizeTask.setOnFailed(e -> {
                                LoadingDialog.hideLoading();
                                    Throwable cause = finalizeTask.getException();
                                    System.err.println("Match finalization failed: " + (cause != null ? cause.getMessage() : "Unknown error"));
                                    if (cause != null) {
                                        cause.printStackTrace();
                                    }
                                    MainApp.showError("Error", "Failed to finalize match: " + (cause != null ? cause.getMessage() : "Unknown error"));
                            });

                            new Thread(finalizeTask).start();
                        });

                    } catch (NumberFormatException ex) {
                        MainApp.showError("Invalid Input", "Please enter valid numeric scores");
                    }
                }
                return null;
            });

            dialog.showAndWait();
        });

        loadTask.setOnFailed(evt -> {
            LoadingDialog.hideLoading();
            MainApp.showError("Error", "Failed to load match: " + evt.getSource().getException().getMessage());
        });

        new Thread(loadTask).start();
    }

    public void refreshMatchesData() {
        loadAllMatches();
    }
}

