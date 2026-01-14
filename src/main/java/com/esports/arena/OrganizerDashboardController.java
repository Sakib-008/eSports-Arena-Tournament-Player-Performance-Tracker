package com.esports.arena;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.esports.arena.dao.LeaderVoteDAO;
import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.dao.TournamentDAO;
import com.esports.arena.model.Match;
import com.esports.arena.model.PlayerMatchStats;
import com.esports.arena.model.Team;
import com.esports.arena.service.JsonExportImportService;
import com.esports.arena.tabs.LeaderboardTabController;
import com.esports.arena.tabs.MatchesTabController;
import com.esports.arena.tabs.PlayersTabController;
import com.esports.arena.tabs.TeamsTabController;
import com.esports.arena.tabs.TournamentsTabController;
import com.esports.arena.util.LoadingDialog;

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
    private LeaderVoteDAO leaderVoteDAO;
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
        leaderVoteDAO = new LeaderVoteDAO();
        jsonService = new JsonExportImportService();
        teamsData = FXCollections.observableArrayList();
        
        LoadingDialog.showLoading("Loading organizer dashboard...");
        initializeTabControllers();
        loadAllData();
    }

    @FXML
    private void handleRefresh() {
        LoadingDialog.showLoading("Refreshing all data...");
        
        Task<List<Team>> refreshTask = new Task<>() {
            @Override
            protected List<Team> call() {
                // Reload shared teams data
                return teamDAO.getAllTeams();
            }
        };
        
        refreshTask.setOnSucceeded(e -> {
            // Update shared teams data
            List<Team> teams = refreshTask.getValue();
            if (teams != null) {
                teamsData.setAll(teams);
            }
            
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

            // Connect leaderboard to tournaments tab for refresh
            if (tournamentsTabController != null) {
                tournamentsTabController.setLeaderboardTabController(leaderboardTabController);
            }

            // Set up team update callback to refresh other tabs
            if (teamsTabController != null) {
                teamsTabController.setOnTeamUpdateCallback(() -> {
                    System.out.println("\n>>> CALLBACK FIRED: Team updated - refreshing other tabs");
                    if (leaderboardTabController != null) {
                        System.out.println("    [1] Leaderboard controller found - calling updateLeaderboard()");
                        leaderboardTabController.updateLeaderboard();
                        System.out.println("    [1] updateLeaderboard() call completed");
                    } else {
                        System.err.println("    [ERROR] LeaderboardTabController is NULL!");
                    }
                    if (playersTabController != null) {
                        System.out.println("    [2] Players controller found - calling updatePlayersList()");
                        playersTabController.updatePlayersList();
                        System.out.println("    [2] updatePlayersList() call completed");
                    }
                    if (matchesTabController != null) {
                        System.out.println("    [3] Matches controller found - calling updateMatchesList()");
                        matchesTabController.updateMatchesList();
                        System.out.println("    [3] updateMatchesList() call completed");
                    }
                    System.out.println(">>> Callback execution finished\n");
                });
                System.out.println("Team update callback set successfully");
            } else {
                System.err.println("ERROR: TeamsTabController is NULL - cannot set callback!");
            }

            // Set up player update callback to refresh other tabs
            if (playersTabController != null) {
                playersTabController.setOnPlayerUpdateCallback(() -> {
                    System.out.println("Player updated - refreshing other tabs");
                    if (matchesTabController != null) {
                        matchesTabController.updateMatchesList();
                    }
                });
            }

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
            LoadingDialog.hideLoading();
        });

        loadTeamsTask.setOnFailed(e -> {
            MainApp.showError("Error", "Failed to load teams");
            LoadingDialog.hideLoading();
        });

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
            LoadingDialog.showLoading("Exporting all data...");
            
            Task<Boolean> exportTask = new Task<>() {
                @Override
                protected Boolean call() {
                    try {
                        JsonExportImportService.ExportData data = new JsonExportImportService.ExportData();
                        
                        // Gather all data
                        data.setPlayers(playerDAO.getAllPlayers());
                        data.setTeams(teamDAO.getAllTeams());
                        data.setTournaments(tournamentDAO.getAllTournaments());
                        data.setMatches(matchDAO.getAllMatches());
                        
                        // Collect all player match stats from all matches
                        List<PlayerMatchStats> allStats = new ArrayList<>();
                        for (Match match : data.getMatches()) {
                            if (match.getPlayerStats() != null) {
                                allStats.addAll(match.getPlayerStats());
                            }
                        }
                        data.setStats(allStats);
                        
                        return jsonService.exportAllDataAsync(data, file.getAbsolutePath()).join();
                    } catch (Exception e) {
                        System.err.println("Export error: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            };

            exportTask.setOnSucceeded(e -> {
                LoadingDialog.hideLoading();
                if (exportTask.getValue()) {
                    MainApp.showInfo("Export Complete", 
                        "All data exported successfully!\n\n" +
                        "File: " + file.getName() + "\n" +
                        "Location: " + file.getParent());
                } else {
                    MainApp.showError("Export Failed", "Failed to export data. Check console for details.");
                }
            });
            
            exportTask.setOnFailed(e -> {
                LoadingDialog.hideLoading();
                MainApp.showError("Export Error", "An error occurred during export: " + 
                    exportTask.getException().getMessage());
            });

            new Thread(exportTask).start();
        }
    }

    @FXML
    private void handleImportData() {
        // Confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Import Data");
        confirmAlert.setHeaderText("Import data from JSON file?");
        confirmAlert.setContentText("Warning: This will add imported data to the existing database.\n" +
                "Duplicate IDs may cause conflicts.\n\nContinue?");
        
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Data");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        File file = fileChooser.showOpenDialog(mainTabPane.getScene().getWindow());

        if (file != null) {
            LoadingDialog.showLoading("Importing data...");
            
            Task<Boolean> importTask = new Task<>() {
                @Override
                protected Boolean call() {
                    try {
                        JsonExportImportService.ExportData data = 
                            jsonService.importAllDataAsync(file.getAbsolutePath()).join();
                        
                        if (data == null) {
                            return false;
                        }
                        
                        int playersCount = 0, teamsCount = 0, tournamentsCount = 0, 
                            matchesCount = 0, statsCount = 0;
                        
                        // Import players
                        if (data.getPlayers() != null) {
                            for (com.esports.arena.model.Player player : data.getPlayers()) {
                                if (playerDAO.createPlayer(player) > 0) {
                                    playersCount++;
                                }
                            }
                        }
                        
                        // Import teams
                        if (data.getTeams() != null) {
                            for (Team team : data.getTeams()) {
                                if (teamDAO.createTeam(team) > 0) {
                                    teamsCount++;
                                }
                            }
                        }
                        
                        // Import tournaments
                        if (data.getTournaments() != null) {
                            for (com.esports.arena.model.Tournament tournament : data.getTournaments()) {
                                if (tournamentDAO.createTournament(tournament) > 0) {
                                    tournamentsCount++;
                                }
                            }
                        }
                        
                        // Import matches (with their stats embedded)
                        if (data.getMatches() != null) {
                            for (Match match : data.getMatches()) {
                                if (matchDAO.createMatch(match) > 0) {
                                    matchesCount++;
                                    if (match.getPlayerStats() != null) {
                                        statsCount += match.getPlayerStats().size();
                                    }
                                }
                            }
                        }
                        
                        updateMessage("Imported:\n" +
                            playersCount + " players\n" +
                            teamsCount + " teams\n" +
                            tournamentsCount + " tournaments\n" +
                            matchesCount + " matches\n" +
                            statsCount + " player stats");
                        
                        return true;
                    } catch (Exception e) {
                        System.err.println("Import error: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            };

            importTask.setOnSucceeded(e -> {
                LoadingDialog.hideLoading();
                if (importTask.getValue()) {
                    MainApp.showInfo("Import Complete", 
                        "Data imported successfully!\n\n" + importTask.getMessage());
                    // Refresh all tabs
                    handleRefresh();
                } else {
                    MainApp.showError("Import Failed", 
                        "Failed to import data. Check console for details.");
                }
            });
            
            importTask.setOnFailed(e -> {
                LoadingDialog.hideLoading();
                MainApp.showError("Import Error", 
                    "An error occurred during import: " + importTask.getException().getMessage());
            });

            new Thread(importTask).start();
        }
    }
}
