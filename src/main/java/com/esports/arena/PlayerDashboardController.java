package com.esports.arena;

import com.esports.arena.dao.LeaderVoteDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.dao.TournamentDAO;
import com.esports.arena.model.Player;
import com.esports.arena.tabs.LeaderboardTabController;
import com.esports.arena.tabs.PlayerProfileTabController;
import com.esports.arena.tabs.PlayerStatsTabController;
import com.esports.arena.tabs.PlayerTeamTabController;
import com.esports.arena.tabs.PlayerVotingTabController;
import com.esports.arena.util.LoadingDialog;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class PlayerDashboardController {

    @FXML
    private TabPane playerTabPane;
    @FXML
    private Button backToMenuBtn;

    private MainApp mainApp;
    private PlayerDAO playerDAO;
    private TeamDAO teamDAO;
    private TournamentDAO tournamentDAO;
    private LeaderVoteDAO voteDAO;

    private Player currentPlayer;

    // Child tab controllers
    private PlayerProfileTabController profileTabController;
    private PlayerStatsTabController statsTabController;
    private PlayerTeamTabController teamTabController;
    private PlayerVotingTabController votingTabController;
    private LeaderboardTabController leaderboardTabController;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void injectMainAppToTabs() {
        if (profileTabController != null) {
            profileTabController.setMainApp(this.mainApp);
        }
        if (statsTabController != null) {
            statsTabController.setMainApp(this.mainApp);
        }
        if (teamTabController != null) {
            teamTabController.setMainApp(this.mainApp);
        }
        if (votingTabController != null) {
            votingTabController.setMainApp(this.mainApp);
        }
        if (leaderboardTabController != null) {
            leaderboardTabController.updateLeaderboard();
        }
    }

    public void setCurrentPlayer(Player player) {
        LoadingDialog.showLoading("Loading player data...");
        this.currentPlayer = player;
        
        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() {
                return null;
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            if (profileTabController != null) {
                profileTabController.loadPlayer(player);
            
            }if (statsTabController != null) {
                statsTabController.refreshForPlayer(player);
            
            }if (teamTabController != null) {
                teamTabController.setCurrentPlayer(player);
            
            }if (votingTabController != null) {
                votingTabController.setCurrentPlayer(player);
            }
            LoadingDialog.hideLoading();
        });
        
        new Thread(loadTask).start();
    }
    
    @FXML
    private void handleRefresh() {
        if (currentPlayer != null) {
            LoadingDialog.showLoading("Refreshing data...");
            Task<Player> refreshTask = new Task<>() {
                @Override
                protected Player call() {
                    return playerDAO.getPlayerById(currentPlayer.getId());
                }
            };
            
            refreshTask.setOnSucceeded(e -> {
                Player refreshedPlayer = refreshTask.getValue();
                if (refreshedPlayer != null) {
                    setCurrentPlayer(refreshedPlayer);
                } else {
                    LoadingDialog.hideLoading();
                }
            });
            
            refreshTask.setOnFailed(e -> LoadingDialog.hideLoading());
            
            new Thread(refreshTask).start();
        }
    }

    @FXML
    private void initialize() {
        playerDAO = new PlayerDAO();
        teamDAO = new TeamDAO();
        tournamentDAO = new TournamentDAO();
        voteDAO = new LeaderVoteDAO();

        initializeTabControllers();
    }

    private void initializeTabControllers() {
        try {
            Tab profileTab = playerTabPane.getTabs().get(0);
            FXMLLoader profileLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/PlayerProfileTab.fxml"));
            profileTab.setContent(profileLoader.load());
            profileTabController = profileLoader.getController();
            profileTabController.setMainApp(this.mainApp);

            Tab statsTab = playerTabPane.getTabs().get(1);
            FXMLLoader statsLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/PlayerStatsTab.fxml"));
            statsTab.setContent(statsLoader.load());
            statsTabController = statsLoader.getController();
            statsTabController.setMainApp(this.mainApp);

            Tab teamTab = playerTabPane.getTabs().get(2);
            FXMLLoader teamLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/PlayerTeamTab.fxml"));
            teamTab.setContent(teamLoader.load());
            teamTabController = teamLoader.getController();
            teamTabController.setMainApp(this.mainApp);

            Tab votingTab = playerTabPane.getTabs().get(3);
            FXMLLoader votingLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/PlayerVotingTab.fxml"));
            votingTab.setContent(votingLoader.load());
            votingTabController = votingLoader.getController();
            votingTabController.setMainApp(this.mainApp);

            Tab leaderboardTab = playerTabPane.getTabs().get(4);
            FXMLLoader lbLoader = new FXMLLoader(getClass().getResource("/fxml/tabs/LeaderboardTab.fxml"));
            leaderboardTab.setContent(lbLoader.load());
            leaderboardTabController = lbLoader.getController();
            leaderboardTabController.initialize(teamDAO);

            // If a player was already set before
            if (currentPlayer != null) {
                setCurrentPlayer(currentPlayer);
            }

        } catch (Exception e) {
            System.err.println("Error initializing player tab controllers: " + e.getMessage());
            e.printStackTrace();
            MainApp.showError("Initialization Error", "Failed to load player tabs: " + e.getMessage());
        }
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

    public void refreshPlayerData() {
        if (currentPlayer != null) {
            Task<Player> refreshTask = new Task<>() {
                @Override
                protected Player call() {
                    return playerDAO.getPlayerById(currentPlayer.getId());
                }
            };

            refreshTask.setOnSucceeded(e -> {
                Player refreshed = refreshTask.getValue();
                if (refreshed != null) {
                    setCurrentPlayer(refreshed);
                }
            });

            refreshTask.setOnFailed(e -> MainApp.showError("Error", "Failed to refresh player data"));

            new Thread(refreshTask).start();
        }
    }

}
