package com.esports.arena.tabs;

import java.util.List;

import com.esports.arena.MainApp;
import com.esports.arena.dao.LeaderVoteDAO;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.model.Player;
import com.esports.arena.model.Team;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class PlayerTeamTabController {
    @FXML private Label myTeamNameLabel;
    @FXML private Label teamWinsLabel;
    @FXML private Label teamLossesLabel;
    @FXML private Label teamLeaderLabel;
    @FXML private ListView<Player> teamMembersList;

    private MainApp mainApp;
    private TeamDAO teamDAO;
    private PlayerDAO playerDAO;
    private LeaderVoteDAO voteDAO;

    private ObservableList<Player> teamMembers = FXCollections.observableArrayList();
    private Player currentPlayer;
    private Team currentTeam;

    public void setMainApp(MainApp mainApp) { this.mainApp = mainApp; }
    public void setCurrentPlayer(Player p) {
        this.currentPlayer = p;
        if (p!=null && p.getTeamId()!=null) loadTeamData(p.getTeamId());
    }

    @FXML
    private void initialize() {
        teamDAO = new TeamDAO();
        playerDAO = new PlayerDAO();
        voteDAO = new LeaderVoteDAO();
        teamMembersList.setItems(teamMembers);
        teamMembersList.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Player item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String status = item.isAvailable() ? "✓" : "✗";
                    setText(status + " " + item.getUsername() + " - " + item.getRole());
                    if (item.isAvailable()) setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #e94560;");
                }
            }
        });
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
            }
        });

        new Thread(task).start();
    }

    private void updateTeamInfo(Team team) {
        myTeamNameLabel.setText(team.getName() + " [" + team.getTag() + "]");
        teamWinsLabel.setText(String.valueOf(team.getWins()));
        teamLossesLabel.setText(String.valueOf(team.getLosses()));

        Task<Player> leaderTask = new Task<>() {
            @Override
            protected Player call() {
                if (team.getLeaderId() != null) return playerDAO.getPlayerById(team.getLeaderId());
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
            teamMembers.setAll(task.getValue());
        });

        new Thread(task).start();
    }
}
