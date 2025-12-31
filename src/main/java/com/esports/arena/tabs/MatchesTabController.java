package com.esports.arena.tabs;

import java.util.List;

import com.esports.arena.MainApp;
import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.util.LoadingDialog;
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
        LoadingDialog.showLoading("Refreshing matches...");
        Task<List<Match>> task = new Task<>() {
            @Override
            protected List<Match> call() {
                return matchDAO.getAllMatches();
            }
        };
        
        task.setOnSucceeded(e -> {
            List<Match> matches = task.getValue();
            if (matches != null) {
                matchesData.setAll(matches);
            }
            LoadingDialog.hideLoading();
        });
        
        task.setOnFailed(e -> LoadingDialog.hideLoading());
        
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
                playersContainer.getChildren().add(new Label((data.team1() != null ? data.team1().getName() : "Team 1") + " Players"));
                for (Player p : data.playersTeam1()) {
                    com.esports.arena.model.PlayerMatchStats pStats = data.stats().stream()
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

            if (!data.playersTeam2().isEmpty()) {
                playersContainer.getChildren().add(new Label((data.team2() != null ? data.team2().getName() : "Team 2") + " Players"));
                for (Player p : data.playersTeam2()) {
                    com.esports.arena.model.PlayerMatchStats pStats = data.stats().stream()
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

                        Match match = data.match();
                        Match.MatchStatus previousStatus = match.getStatus();
                        Integer previousWinner = match.getWinnerId();

                        match.setTeam1Score(team1Score);
                        match.setTeam2Score(team2Score);
                        match.setStatus(Match.MatchStatus.COMPLETED);

                        boolean winnerDecided = team1Score != team2Score;
                        if (!winnerDecided) {
                            MainApp.showError("Invalid Score", "Scores cannot be tied for a completed match");
                            return null;
                        }

                        int winningTeamId = team1Score > team2Score ? match.getTeam1Id() : match.getTeam2Id();
                        int losingTeamId = winningTeamId == match.getTeam1Id() ? match.getTeam2Id() : match.getTeam1Id();
                        match.setWinnerId(winningTeamId);
                        boolean shouldUpdateTeamRecords = previousStatus != Match.MatchStatus.COMPLETED || previousWinner == null;

                        for (PlayerInputs pi : inputs) {
                            int kills = Integer.parseInt(pi.kills.getText());
                            int deaths = Integer.parseInt(pi.deaths.getText());
                            int assists = Integer.parseInt(pi.assists.getText());
                            matchDAO.updatePlayerStats(match.getId(), pi.playerId, kills, deaths, assists);

                            boolean onWinningTeam = (winningTeamId == match.getTeam1Id() && playersTeam1.stream().anyMatch(p -> p.getId() == pi.playerId))
                                    || (winningTeamId == match.getTeam2Id() && playersTeam2.stream().anyMatch(p -> p.getId() == pi.playerId));
                            if (shouldUpdateTeamRecords) { // reuse flag to avoid double-counting player totals on re-finalize
                                playerDAO.updatePlayerStats(pi.playerId, kills, deaths, assists, onWinningTeam);
                            }
                        }

                        if (shouldUpdateTeamRecords) {
                            teamDAO.updateTeamRecord(winningTeamId, true, false);
                            teamDAO.updateTeamRecord(losingTeamId, false, false);
                        }

                        matchDAO.updateMatch(match);
                        loadMatchesForTournament(match.getTournamentId());
                        MainApp.showInfo("Success", "Match finalized and stats saved");
                    } catch (NumberFormatException ex) {
                        MainApp.showError("Invalid Input", "Please enter valid numeric scores and stats");
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

