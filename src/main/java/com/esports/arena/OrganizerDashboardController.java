package com.esports.arena;

import java.io.File;
import java.util.List;

import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.dao.TournamentDAO;
import com.esports.arena.model.Team;
import com.esports.arena.service.JsonExportImportService;
import com.esports.arena.util.LoadingDialog;
import com.esports.arena.tabs.LeaderboardTabController;
import com.esports.arena.tabs.MatchesTabController;
import com.esports.arena.tabs.PlayersTabController;
import com.esports.arena.tabs.TeamsTabController;
import com.esports.arena.tabs.TournamentsTabController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.FileChooser;

public class OrganizerDashboardController {
    @FXML private TabPane mainTabPane;
    @FXML private Button exportDataBtn;
    @FXML private Button importDataBtn;
    @FXML private Button backToMenuBtn;

    // Tab controllers
    private TeamsTabController teamsTabController;
    private PlayersTabController playersTabController;
    private TournamentsTabController tournamentsTabController;
    private MatchesTabController matchesTabController;
    private LeaderboardTabController leaderboardTabController;

    private MainApp mainApp;
    private TeamDAO teamDAO;
    private PlayerDAO playerDAO;
    private TournamentDAO tournamentDAO;
    private MatchDAO matchDAO;
    private JsonExportImportService jsonService;

    private ObservableList<Team> teamsData;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void injectMainAppToTabs() {
        if (playersTabController != null) {
            playersTabController.setMainApp(this.mainApp);
        }
    }

    @FXML
    private void initialize() {
        teamDAO = new TeamDAO();
        playerDAO = new PlayerDAO();
        tournamentDAO = new TournamentDAO();
        matchDAO = new MatchDAO();
        jsonService = new JsonExportImportService();
        teamsData = FXCollections.observableArrayList();
        
        LoadingDialog.showLoading("Loading organizer dashboard...");
        initializeTabControllers();
        loadAllData();
    }

    @FXML
    private void handleRefresh() {
        LoadingDialog.showLoading("Refreshing all data...");
        
        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() {
                // Load all data in background
                return null;
            }
        };
        
        refreshTask.setOnSucceeded(e -> {
            // Trigger all tab updates (they are already async)
            if (teamsTabController != null) {
                teamsTabController.updateTeamsList();
            }
            if (playersTabController != null) {
                playersTabController.updatePlayersList();
            }
            if (tournamentsTabController != null) {
                tournamentsTabController.updateTournamentsList();
            }
            if (matchesTabController != null) {
                matchesTabController.updateMatchesList();
            }
            if (leaderboardTabController != null) {
                leaderboardTabController.updateLeaderboard();
            }
            
            // Hide loading after a short delay to ensure all updates start
            javafx.application.Platform.runLater(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    // Ignore
                }
                LoadingDialog.hideLoading();
            });
        });
        
        refreshTask.setOnFailed(e -> LoadingDialog.hideLoading());
        
        new Thread(refreshTask).start();
    }

    private void initializeTabControllers() {
        try {
            Tab teamsTab = mainTabPane.getTabs().get(0);
            FXMLLoader teamsLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/TeamsTab.fxml"));
            teamsTab.setContent(teamsLoader.load());
            teamsTabController = teamsLoader.getController();
            teamsTabController.initialize(teamDAO, playerDAO);

            Tab playersTab = mainTabPane.getTabs().get(1);
            FXMLLoader playersLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/PlayersTab.fxml"));
            playersTab.setContent(playersLoader.load());
            playersTabController = playersLoader.getController();
            playersTabController.initialize(playerDAO, teamDAO, teamsData);

            Tab matchesTab = mainTabPane.getTabs().get(4);
            FXMLLoader matchesLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/MatchesTab.fxml"));
            matchesTab.setContent(matchesLoader.load());
            matchesTabController = matchesLoader.getController();
            matchesTabController.initialize(matchDAO, teamDAO, playerDAO, teamsData);

            Tab tournamentsTab = mainTabPane.getTabs().get(2);
            FXMLLoader tournamentsLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/TournamentsTab.fxml"));
            tournamentsTab.setContent(tournamentsLoader.load());
            tournamentsTabController = tournamentsLoader.getController();
            tournamentsTabController.initialize(tournamentDAO, matchDAO, teamDAO, teamsData, matchesTabController);

            Tab leaderboardTab = mainTabPane.getTabs().get(3);
            FXMLLoader leaderboardLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/LeaderboardTab.fxml"));
            leaderboardTab.setContent(leaderboardLoader.load());
            leaderboardTabController = leaderboardLoader.getController();
            leaderboardTabController.initialize(teamDAO);

        } catch (Exception e) {
            System.err.println("Error initializing tab controllers: " + e.getMessage());
            e.printStackTrace();
            MainApp.showError("Initialization Error", "Failed to load dashboard tabs: " + e.getMessage());
        }
    }

    private void loadAllData() {
        Task<List<Team>> loadTeamsTask = new Task<>() {
            @Override
            protected List<Team> call() {
                return teamDAO.getAllTeams();
            }
        };

        loadTeamsTask.setOnSucceeded(e -> {
            teamsData.setAll(loadTeamsTask.getValue());
            if (leaderboardTabController != null) {
                leaderboardTabController.updateLeaderboard();
            }
        });

        loadTeamsTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load teams"));

        new Thread(loadTeamsTask).start();
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

    @FXML
    private void handleExportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showSaveDialog(mainTabPane.getScene().getWindow());

        if (file != null) {
            Task<Boolean> exportTask = new Task<>() {
                @Override
                protected Boolean call() {
                    JsonExportImportService.ExportData data = new JsonExportImportService.ExportData();
                    data.setPlayers(playerDAO.getAllPlayers());
                    data.setTeams(teamDAO.getAllTeams());
                    return jsonService.exportAllDataAsync(data, file.getAbsolutePath()).join();
                }
            };

            exportTask.setOnSucceeded(e -> {
                if (exportTask.getValue()) {
                    MainApp.showInfo("Export", "Data exported successfully to: " + file.getName());
                } else {
                    MainApp.showError("Export", "Failed to export data");
                }
            });

            new Thread(exportTask).start();
        }
    }

    @FXML
    private void handleImportData() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());

        if (file != null) {
            MainApp.showInfo("Import", "Import functionality - data will be loaded from: " + file.getName());
            loadAllData();
        }
    }
}
