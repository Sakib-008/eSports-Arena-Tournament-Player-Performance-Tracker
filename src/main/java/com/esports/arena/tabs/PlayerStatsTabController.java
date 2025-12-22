package com.esports.arena.tabs;

import java.util.List;

import com.esports.arena.MainApp;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TournamentDAO;
import com.esports.arena.model.Player;
import com.esports.arena.model.Tournament;
import com.esports.arena.service.TournamentStatsService;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;

public class PlayerStatsTabController {
    @FXML private Label totalKillsLabel;
    @FXML private Label totalDeathsLabel;
    @FXML private Label totalAssistsLabel;
    @FXML private Label matchesPlayedLabel;
    @FXML private Label matchesWonLabel;
    @FXML private ComboBox<Tournament> tournamentCombo;
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

    private MainApp mainApp;
    private Player currentPlayer;
    private PlayerDAO playerDAO;
    private TournamentDAO tournamentDAO;
    private TournamentStatsService tournamentStatsService;

    public void setMainApp(MainApp mainApp) { this.mainApp = mainApp; }
    public void setCurrentPlayer(Player p) { this.currentPlayer = p; if (p!=null) loadPlayerStats(p); }

    @FXML
    private void initialize() {
        playerDAO = new PlayerDAO();
        tournamentDAO = new TournamentDAO();
        tournamentStatsService = new TournamentStatsService();
        setupStatsInputs();
        loadTournaments();
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

    private void loadTournaments() {
        Task<List<Tournament>> task = new Task<>() {
            @Override
            protected List<Tournament> call() {
                return tournamentDAO.getAllTournaments();
            }
        };

        task.setOnSucceeded(e -> {
            List<Tournament> tournaments = task.getValue();
            tournamentCombo.getItems().clear();
            tournamentCombo.getItems().add(null);
            tournamentCombo.getItems().addAll(tournaments);
            tournamentCombo.getSelectionModel().select(0);
        });

        task.setOnFailed(e -> MainApp.showError("Error", "Failed to load tournaments"));

        new Thread(task).start();
    }

    private void loadTournamentStats(Tournament tournament) {
        if (currentPlayer == null) return;

        if (tournament == null) {
            tournamentKillsLabel.setText(String.valueOf(currentPlayer.getTotalKills()));
            tournamentDeathsLabel.setText(String.valueOf(currentPlayer.getTotalDeaths()));
            tournamentAssistsLabel.setText(String.valueOf(currentPlayer.getTotalAssists()));
            tournamentMatchesPlayedLabel.setText(String.valueOf(currentPlayer.getMatchesPlayed()));
            tournamentMatchesWonLabel.setText(String.valueOf(currentPlayer.getMatchesWon()));
            tournamentKDLabel.setText(String.format("%.2f", currentPlayer.getKdRatio()));
            tournamentWinRateLabel.setText(String.format("%.1f%%", currentPlayer.getWinRate()));
            return;
        }

        Task<com.esports.arena.service.TournamentStatsService.TournamentPlayerStats> task = new Task<>() {
            @Override
            protected com.esports.arena.service.TournamentStatsService.TournamentPlayerStats call() {
                return tournamentStatsService.getPlayerTournamentStats(currentPlayer.getId(), tournament.getId());
            }
        };

        task.setOnSucceeded(e -> {
            com.esports.arena.service.TournamentStatsService.TournamentPlayerStats stats = task.getValue();
            tournamentKillsLabel.setText(String.valueOf(stats.kills));
            tournamentDeathsLabel.setText(String.valueOf(stats.deaths));
            tournamentAssistsLabel.setText(String.valueOf(stats.assists));
            tournamentMatchesPlayedLabel.setText(String.valueOf(stats.matchesPlayed));
            tournamentMatchesWonLabel.setText(String.valueOf(stats.matchesWon));
            tournamentKDLabel.setText(String.format("%.2f", stats.getKdRatio()));
            tournamentWinRateLabel.setText(String.format("%.1f%%", stats.getWinRate()));
        });

        task.setOnFailed(e -> MainApp.showError("Error", "Failed to load tournament stats"));

        new Thread(task).start();
    }

    private void loadPlayerStats(Player player) {
        if (player == null) return;
        this.currentPlayer = player;

        totalKillsLabel.setText(String.valueOf(player.getTotalKills()));
        totalDeathsLabel.setText(String.valueOf(player.getTotalDeaths()));
        totalAssistsLabel.setText(String.valueOf(player.getTotalAssists()));
        matchesPlayedLabel.setText(String.valueOf(player.getMatchesPlayed()));
        matchesWonLabel.setText(String.valueOf(player.getMatchesWon()));

        loadTournamentStats(null);
        updatePerformanceChart(player);
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

    public void refreshForPlayer(Player player) {
        loadPlayerStats(player);
    }

    public void recordMatchStats(int kills, int deaths, int assists, boolean won) {
        if (currentPlayer == null) return;
        playerDAO.updatePlayerStats(currentPlayer.getId(), kills, deaths, assists, won);
    }
}
