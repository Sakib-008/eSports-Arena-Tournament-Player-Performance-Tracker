package com.esports.arena.tabs;

import java.util.List;
import java.util.stream.Collectors;

import com.esports.arena.MainApp;
import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.dao.TournamentDAO;
import com.esports.arena.model.Match;
import com.esports.arena.model.Team;
import com.esports.arena.model.Tournament;
import com.esports.arena.util.LoadingDialog;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class LeaderboardTabController {
    @FXML private TableView<Team> leaderboardTable;
    @FXML private TableColumn<Team, Integer> lbRankCol;
    @FXML private TableColumn<Team, String> lbTeamCol;
    @FXML private TableColumn<Team, Integer> lbWinsCol;
    @FXML private TableColumn<Team, Integer> lbLossesCol;
    @FXML private TableColumn<Team, Double> lbWinRateCol;
    @FXML private ComboBox<Tournament> tournamentFilterCombo;

    private TeamDAO teamDAO;
    private TournamentDAO tournamentDAO;
    private MatchDAO matchDAO;
    private Integer currentTournamentFilter;

    public void initialize(TeamDAO teamDAO) {
        this.teamDAO = teamDAO;
        this.tournamentDAO = new TournamentDAO();
        this.matchDAO = new MatchDAO();
        this.currentTournamentFilter = null;
        setupLeaderboardTable();
        loadTournamentFilter();
        updateLeaderboard();
    }

    private void setupLeaderboardTable() {
        lbRankCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createIntegerBinding(
                        () -> leaderboardTable.getItems().indexOf(cellData.getValue()) + 1
                ).asObject());
        lbTeamCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        lbWinsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        lbLossesCol.setCellValueFactory(new PropertyValueFactory<>("losses"));
        lbWinRateCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> cellData.getValue().getWinRate()
                ).asObject());
    }

    private void loadTournamentFilter() {
        Task<List<Tournament>> task = new Task<>() {
            @Override
            protected List<Tournament> call() {
                return tournamentDAO.getAllTournaments();
            }
        };

        task.setOnSucceeded(e -> {
            List<Tournament> tournaments = task.getValue();
            tournamentFilterCombo.setItems(FXCollections.observableArrayList(tournaments));
            tournamentFilterCombo.setCellFactory(param -> new javafx.scene.control.ListCell<Tournament>() {
                @Override
                protected void updateItem(Tournament item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
            tournamentFilterCombo.setButtonCell(new javafx.scene.control.ListCell<Tournament>() {
                @Override
                protected void updateItem(Tournament item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "Select Tournament" : item.getName());
                }
            });
        });

        new Thread(task).start();
    }

    @FXML
    private void handleApplyTournamentFilter() {
        Tournament selected = tournamentFilterCombo.getSelectionModel().getSelectedItem();
        if (selected != null) {
            currentTournamentFilter = selected.getId();
            updateLeaderboard();
        } else {
            MainApp.showError("No Selection", "Please select a tournament");
        }
    }

    @FXML
    private void handleShowOverallLeaderboard() {
        currentTournamentFilter = null;
        tournamentFilterCombo.getSelectionModel().clearSelection();
        updateLeaderboard();
    }

    public void updateLeaderboard() {
        LoadingDialog.showLoading("Updating leaderboard...");
        Task<List<Team>> task = new Task<>() {
            @Override
            protected List<Team> call() {
                if (currentTournamentFilter == null) {
                    // Overall leaderboard
                    return teamDAO.getLeaderboard();
                } else {
                    // Tournament-specific leaderboard
                    return getTournamentLeaderboard(currentTournamentFilter);
                }
            }
        };

        task.setOnSucceeded(e -> {
                leaderboardTable.setItems(FXCollections.observableArrayList(task.getValue()));
                LoadingDialog.hideLoading();
        });

        task.setOnFailed(e -> {
                MainApp.showError("Error", "Failed to update leaderboard");
                LoadingDialog.hideLoading();
        });

        new Thread(task).start();
    }

    private List<Team> getTournamentLeaderboard(int tournamentId) {
        List<Match> tournamentMatches = matchDAO.getAllMatches().stream()
                .filter(m -> m.getTournamentId() == tournamentId && m.getStatus() == Match.MatchStatus.COMPLETED)
                .collect(Collectors.toList());

        List<Team> allTeams = teamDAO.getAllTeams();
        
        // Create a copy of teams with tournament-specific stats
        List<Team> tournamentTeams = allTeams.stream()
                .map(team -> {
                    Team copy = new Team();
                    copy.setId(team.getId());
                    copy.setName(team.getName());
                    copy.setTag(team.getTag());
                    copy.setRegion(team.getRegion());
                    copy.setLeaderId(team.getLeaderId());
                    copy.setCreatedDate(team.getCreatedDate());
                    
                    // Calculate tournament-specific stats
                    int wins = 0;
                    int losses = 0;
                    int draws = 0;
                    
                    for (Match match : tournamentMatches) {
                        if (match.getTeam1Id() == team.getId() || match.getTeam2Id() == team.getId()) {
                            if (match.getWinnerId() == null) {
                                draws++;
                            } else if (match.getWinnerId() == team.getId()) {
                                wins++;
                            } else {
                                losses++;
                            }
                        }
                    }
                    
                    copy.setWins(wins);
                    copy.setLosses(losses);
                    copy.setDraws(draws);
                    
                    return copy;
                })
                .filter(team -> team.getWins() > 0 || team.getLosses() > 0 || team.getDraws() > 0) // Only teams that played
                .sorted((a, b) -> {
                    // Sort by wins first, then by win rate
                    int winsComp = Integer.compare(b.getWins(), a.getWins());
                    if (winsComp != 0) return winsComp;
                    return Double.compare(b.getWinRate(), a.getWinRate());
                })
                .collect(Collectors.toList());
        
        return tournamentTeams;
    }
}

