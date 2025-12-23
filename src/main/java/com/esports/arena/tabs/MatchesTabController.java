package com.esports.arena.tabs;

import java.util.List;

import com.esports.arena.MainApp;
import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.model.Match;
import com.esports.arena.model.Player;
import com.esports.arena.model.Team;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
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

    private MatchDAO matchDAO;
    private TeamDAO teamDAO;
    private PlayerDAO playerDAO;
    private ObservableList<Match> matchesData;
    private ObservableList<Team> teamsData;

    public void initialize(MatchDAO matchDAO, TeamDAO teamDAO, PlayerDAO playerDAO, ObservableList<Team> teamsData) {
        this.matchDAO = matchDAO;
        this.teamDAO = teamDAO;
        this.playerDAO = playerDAO;
        this.teamsData = teamsData;
        this.matchesData = FXCollections.observableArrayList();
        setupMatchesTable();
        loadAllMatches();
    }

    private void setupMatchesTable() {
        matchIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        matchTeam1Col.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            if (match != null) {
                Team team1 = teamDAO.getTeamById(match.getTeam1Id());
                return javafx.beans.binding.Bindings.createStringBinding(
                        () -> team1 != null ? team1.getName() : "Team " + match.getTeam1Id());
            }
            return javafx.beans.binding.Bindings.createStringBinding(() -> "Unknown");
        });

        matchTeam2Col.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            if (match != null) {
                Team team2 = teamDAO.getTeamById(match.getTeam2Id());
                return javafx.beans.binding.Bindings.createStringBinding(
                        () -> team2 != null ? team2.getName() : "Team " + match.getTeam2Id());
            }
            return javafx.beans.binding.Bindings.createStringBinding(() -> "Unknown");
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
    }

    public void loadMatchesForTournament(int tournamentId) {
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
            if (matches.isEmpty()) {
                MainApp.showInfo("No Matches", "No matches found for this tournament. Create matches first.");
            } else {
                MainApp.showInfo("Matches Loaded", "Showing " + matches.size() + " match(es) for tournament");
            }
        });

        task.setOnFailed(e -> {
            MainApp.showError("Error", "Failed to load matches: " + task.getException().getMessage());
            matchesData.clear();
        });

        new Thread(task).start();
    }

    public void loadAllMatches() {
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
        });

        task.setOnFailed(e -> {
            MainApp.showError("Error", "Failed to load matches: " + task.getException().getMessage());
            matchesData.clear();
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

        Match match = matchDAO.getMatchById(selected.getId());
        if (match == null) {
            MainApp.showError("Error", "Failed to load match details");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Match Details - " + match.getId());
        dialog.setHeaderText("View Match Details and Player Stats");

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPrefWidth(700);

        Team team1 = teamDAO.getTeamById(match.getTeam1Id());
        Team team2 = teamDAO.getTeamById(match.getTeam2Id());

        Label teamsLabel = new Label((team1 != null ? team1.getName() : "Team 1") + " vs " + (team2 != null ? team2.getName() : "Team 2"));
        teamsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox scoreBox = new HBox(10);
        Label team1ScoreLabel = new Label("Score: " + match.getTeam1Score() + " - " + match.getTeam2Score());
        team1ScoreLabel.setStyle("-fx-font-size: 12px;");
        Label statusLabel = new Label("Status: " + match.getStatus().toString());
        statusLabel.setStyle("-fx-font-size: 12px;");

        scoreBox.getChildren().addAll(team1ScoreLabel, statusLabel);

        content.getChildren().addAll(teamsLabel, scoreBox, new Separator());

        List<Player> playersTeam1 = playerDAO.getPlayersByTeam(match.getTeam1Id());
        List<Player> playersTeam2 = playerDAO.getPlayersByTeam(match.getTeam2Id());

        VBox playersContainer = new VBox(10);

        List<com.esports.arena.model.PlayerMatchStats> allStats = matchDAO.getPlayerStatsByMatch(match.getId());

        if (!playersTeam1.isEmpty()) {
            playersContainer.getChildren().add(new Label("" + (team1 != null ? team1.getName() : "Team 1") + " Players"));
            for (Player p : playersTeam1) {
                com.esports.arena.model.PlayerMatchStats pStats = allStats.stream()
                        .filter(s -> s.getPlayerId() == p.getId())
                        .findFirst().orElse(null);
                
                int kills = pStats != null ? pStats.getKills() : 0;
                int deaths = pStats != null ? pStats.getDeaths() : 0;
                int assists = pStats != null ? pStats.getAssists() : 0;
                
                HBox row = new HBox(8);
                row.getChildren().addAll(
                    new Label(p.getUsername()),
                    new Label("K: " + kills),
                    new Label("D: " + deaths),
                    new Label("A: " + assists)
                );
                playersContainer.getChildren().add(row);
            }
        }

        if (!playersTeam2.isEmpty()) {
            playersContainer.getChildren().add(new Label("" + (team2 != null ? team2.getName() : "Team 2") + " Players"));
            for (Player p : playersTeam2) {
                com.esports.arena.model.PlayerMatchStats pStats = allStats.stream()
                        .filter(s -> s.getPlayerId() == p.getId())
                        .findFirst().orElse(null);
                
                int kills = pStats != null ? pStats.getKills() : 0;
                int deaths = pStats != null ? pStats.getDeaths() : 0;
                int assists = pStats != null ? pStats.getAssists() : 0;
                
                HBox row = new HBox(8);
                row.getChildren().addAll(
                    new Label(p.getUsername()),
                    new Label("K: " + kills),
                    new Label("D: " + deaths),
                    new Label("A: " + assists)
                );
                playersContainer.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(playersContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);

        content.getChildren().add(scroll);

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    @FXML
    private void handleEditMatchStats() {
        Match selected = matchesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a match to edit");
            return;
        }

        Match match = matchDAO.getMatchById(selected.getId());
        if (match == null) {
            MainApp.showError("Error", "Failed to load match details");
            return;
        }

        // Prevent editing completed matches
        if (match.getStatus() == Match.MatchStatus.COMPLETED) {
            MainApp.showError("Match Completed", "Cannot edit statistics for matches that are already completed");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Match Details - " + match.getId());
        dialog.setHeaderText("Match Details and Player Stats");

        ButtonType finalizeButtonType = new ButtonType("Finalize Match", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(finalizeButtonType, ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPrefWidth(700);

        Team team1 = teamDAO.getTeamById(match.getTeam1Id());
        Team team2 = teamDAO.getTeamById(match.getTeam2Id());

        Label teamsLabel = new Label((team1 != null ? team1.getName() : "Team 1") + " vs " + (team2 != null ? team2.getName() : "Team 2"));
        teamsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox scoreBox = new HBox(10);
        TextField team1ScoreField = new TextField(String.valueOf(match.getTeam1Score()));
        team1ScoreField.setPrefWidth(80);
        TextField team2ScoreField = new TextField(String.valueOf(match.getTeam2Score()));
        team2ScoreField.setPrefWidth(80);

        scoreBox.getChildren().addAll(new Label(team1 != null ? team1.getName() : "Team 1"), team1ScoreField,
                new Label("-"), team2ScoreField, new Label(team2 != null ? team2.getName() : "Team 2"));

        content.getChildren().addAll(teamsLabel, scoreBox, new Separator());

        List<Player> playersTeam1 = playerDAO.getPlayersByTeam(match.getTeam1Id());
        List<Player> playersTeam2 = playerDAO.getPlayersByTeam(match.getTeam2Id());

        VBox playersContainer = new VBox(10);

        class PlayerInputs {
            int playerId;
            TextField kills = new TextField("0");
            TextField deaths = new TextField("0");
            TextField assists = new TextField("0");
        }

        List<PlayerInputs> inputs = new java.util.ArrayList<>();

        if (!playersTeam1.isEmpty()) {
            playersContainer.getChildren().add(new Label("" + (team1 != null ? team1.getName() : "Team 1") + " Players"));
            for (Player p : playersTeam1) {
                PlayerInputs pi = new PlayerInputs();
                pi.playerId = p.getId();
                HBox row = new HBox(8);
                row.getChildren().addAll(new Label(p.getUsername()), new Label("Kills:"), pi.kills,
                        new Label("Deaths:"), pi.deaths, new Label("Assists:"), pi.assists);
                playersContainer.getChildren().add(row);
                inputs.add(pi);
            }
        }

        if (!playersTeam2.isEmpty()) {
            playersContainer.getChildren().add(new Label("" + (team2 != null ? team2.getName() : "Team 2") + " Players"));
            for (Player p : playersTeam2) {
                PlayerInputs pi = new PlayerInputs();
                pi.playerId = p.getId();
                HBox row = new HBox(8);
                row.getChildren().addAll(new Label(p.getUsername()), new Label("Kills:"), pi.kills,
                        new Label("Deaths:"), pi.deaths, new Label("Assists:"), pi.assists);
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
                int s1 = 0, s2 = 0;
                try {
                    s1 = Integer.parseInt(team1ScoreField.getText().trim());
                    s2 = Integer.parseInt(team2ScoreField.getText().trim());
                } catch (NumberFormatException e) {
                    MainApp.showError("Invalid Input", "Please enter valid integer scores");
                    return null;
                }

                int finalS1 = s1, finalS2 = s2;

                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() {
                        match.setTeam1Score(finalS1);
                        match.setTeam2Score(finalS2);
                        if (match.getActualStartTime() == null) {
                            match.setActualStartTime(java.time.LocalDateTime.now());
                        }
                        match.setActualEndTime(java.time.LocalDateTime.now());

                        Integer winnerId = null;
                        if (finalS1 > finalS2) {
                            winnerId = match.getTeam1Id();
                        } else if (finalS2 > finalS1) {
                            winnerId = match.getTeam2Id();
                        }
                        match.setWinnerId(winnerId);
                        match.setStatus(Match.MatchStatus.COMPLETED);

                        matchDAO.updateMatch(match);

                        if (winnerId == null) {
                            teamDAO.updateTeamRecord(match.getTeam1Id(), false, true);
                            teamDAO.updateTeamRecord(match.getTeam2Id(), false, true);
                        } else if (winnerId.equals(match.getTeam1Id())) {
                            teamDAO.updateTeamRecord(match.getTeam1Id(), true, false);
                            teamDAO.updateTeamRecord(match.getTeam2Id(), false, false);
                        } else {
                            teamDAO.updateTeamRecord(match.getTeam2Id(), true, false);
                            teamDAO.updateTeamRecord(match.getTeam1Id(), false, false);
                        }

                        for (PlayerInputs pi : inputs) {
                            int kills = 0, deaths = 0, assists = 0;
                            try {
                                kills = Integer.parseInt(pi.kills.getText().trim());
                                deaths = Integer.parseInt(pi.deaths.getText().trim());
                                assists = Integer.parseInt(pi.assists.getText().trim());
                            } catch (NumberFormatException ignored) {
                            }

                            // Adding player stats here
                            com.esports.arena.model.PlayerMatchStats stats = new com.esports.arena.model.PlayerMatchStats();
                            stats.setMatchId(match.getId());
                            stats.setPlayerId(pi.playerId);
                            stats.setKills(kills);
                            stats.setDeaths(deaths);
                            stats.setAssists(assists);

                            boolean playerWon = false;
                            com.esports.arena.model.Player p = playerDAO.getPlayerById(pi.playerId);
                            if (p != null && p.getTeamId() != null && winnerId != null) {
                                playerWon = p.getTeamId().equals(winnerId);
                            }

                            matchDAO.addPlayerStats(stats);
                            playerDAO.updatePlayerStats(pi.playerId, kills, deaths, assists, playerWon);
                        }

                        return null;
                    }
                };

                task.setOnSucceeded(e -> {
                    MainApp.showInfo("Success", "Match finalized and stats recorded");
                    // Real-time update: fetch the updated match and refresh it in the table immediately
                    Task<Match> refreshTask = new Task<>() {
                        @Override
                        protected Match call() {
                            return matchDAO.getMatchById(match.getId());
                        }
                    };
                    
                    refreshTask.setOnSucceeded(refreshEvent -> {
                        Match updatedMatch = refreshTask.getValue();
                        if (updatedMatch != null) {
                            // Find and replace the match in the observable list
                            for (int i = 0; i < matchesData.size(); i++) {
                                if (matchesData.get(i).getId() == updatedMatch.getId()) {
                                    matchesData.set(i, updatedMatch);
                                    break;
                                }
                            }
                        }
                    });
                    
                    new Thread(refreshTask).start();
                });

                task.setOnFailed(e -> {
                    MainApp.showError("Error", "Failed to finalize match: " + task.getException().getMessage());
                });

                new Thread(task).start();

            }
            return null;
        });

        dialog.showAndWait();
    }

    public void refreshMatchesData() {
        loadAllMatches();
    }
}

