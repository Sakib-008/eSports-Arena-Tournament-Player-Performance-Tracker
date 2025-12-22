package com.esports.arena.tabs;

import java.util.List;

import com.esports.arena.MainApp;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.model.Team;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    @FXML private Button refreshLeaderboardBtn;

    private TeamDAO teamDAO;

    public void initialize(TeamDAO teamDAO) {
        this.teamDAO = teamDAO;
        setupLeaderboardTable();
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

    @FXML
    private void handleRefreshLeaderboard() {
        updateLeaderboard();
    }

    public void updateLeaderboard() {
        Task<List<Team>> task = new Task<>() {
            @Override
            protected List<Team> call() {
                return teamDAO.getLeaderboard();
            }
        };

        task.setOnSucceeded(e ->
                leaderboardTable.setItems(FXCollections.observableArrayList(task.getValue())));

        task.setOnFailed(e ->
                MainApp.showError("Error", "Failed to update leaderboard"));

        new Thread(task).start();
    }
}

