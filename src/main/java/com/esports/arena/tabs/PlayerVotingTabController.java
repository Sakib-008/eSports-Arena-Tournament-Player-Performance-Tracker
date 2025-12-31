package com.esports.arena.tabs;

import java.util.List;
import java.util.Map;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class PlayerVotingTabController {
    @FXML private ListView<Player> voteCandidatesList;
    @FXML private Button castVoteBtn;
    @FXML private Label votingStatusLabel;
    @FXML private Button viewVotingResultsBtn;

    private MainApp mainApp;
    private LeaderVoteDAO voteDAO;
    private PlayerDAO playerDAO;
    private TeamDAO teamDAO;
    private ObservableList<Player> voteCandidates = FXCollections.observableArrayList();

    private Player currentPlayer;
    private Team currentTeam;

    public void setMainApp(MainApp mainApp) { this.mainApp = mainApp; }
    public void setCurrentPlayer(Player p) {
        this.currentPlayer = p;
        if (p!=null && p.getTeamId()!=null) loadVotingData(p.getTeamId());
    }

    @FXML
    private void initialize() {
        voteDAO = new LeaderVoteDAO();
        playerDAO = new PlayerDAO();
        teamDAO = new TeamDAO();
        voteCandidatesList.setItems(voteCandidates);
        voteCandidatesList.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Player item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText(item.getUsername() + " (" + item.getRole() + ")");
            }
        });
    }

    private void loadVotingData(int teamId) {
        Task<Map<Integer, Integer>> task = new Task<>() {
            @Override
            protected Map<Integer, Integer> call() {
                return voteDAO.getVoteCounts(teamId);
            }
        };

        task.setOnSucceeded(e -> {
            boolean hasVoted = voteDAO.hasVoted(teamId, currentPlayer.getId());
            votingStatusLabel.setText(hasVoted ? "You have already voted" : "You haven't voted yet");
            castVoteBtn.setDisable(hasVoted);

            // load candidates
            List<Player> members = playerDAO.getPlayersByTeam(teamId);
            voteCandidates.setAll(members);
        });

        new Thread(task).start();
    }

    @FXML
    private void handleCastVote() {
        if (currentPlayer == null || currentPlayer.getTeamId() == null) {
            MainApp.showError("Error", "No player or team selected");
            return;
        }

        Player candidate = voteCandidatesList.getSelectionModel().getSelectedItem();
        if (candidate == null) {
            MainApp.showError("No Selection", "Please select a candidate to vote for");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Vote");
        confirm.setHeaderText("Vote for Team Leader");
        confirm.setContentText("Vote for: " + candidate.getUsername() + "?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                castVote(candidate);
            }
        });
    }

    private void castVote(Player candidate) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return voteDAO.castVote(currentPlayer.getTeamId(), currentPlayer.getId(), candidate.getId());
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                MainApp.showInfo("Success", "Vote cast successfully!");
                loadVotingData(currentPlayer.getTeamId());
            } else {
                MainApp.showError("Error", "Failed to cast vote");
            }
        });

        task.setOnFailed(e -> MainApp.showError("Error", "Failed to cast vote"));

        new Thread(task).start();
    }

    @FXML
    private void handleViewVotingResults() {
        if (currentPlayer == null || currentPlayer.getTeamId() == null) {
            MainApp.showError("Error", "No team selected");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Voting Results");
        dialog.setHeaderText("Current Leader Election Results");

        TableView<VoteResultDisplay> resultsTable = new TableView<>();
        resultsTable.setPrefWidth(400);
        resultsTable.setPrefHeight(300);

        TableColumn<VoteResultDisplay, String> nameCol = new TableColumn<>("Candidate");
        nameCol.setPrefWidth(250);
        TableColumn<VoteResultDisplay, Integer> votesCol = new TableColumn<>("Votes");
        votesCol.setPrefWidth(100);

        resultsTable.getColumns().addAll(nameCol, votesCol);

        Task<Map<Integer, Integer>> task = new Task<>() {
            @Override
            protected Map<Integer, Integer> call() {
                return voteDAO.getVoteCounts(currentPlayer.getTeamId());
            }
        };

        task.setOnSucceeded(e -> {
            Map<Integer, Integer> voteCounts = task.getValue();
            javafx.collections.ObservableList<VoteResultDisplay> results = javafx.collections.FXCollections.observableArrayList();
            List<Player> members = playerDAO.getPlayersByTeam(currentPlayer.getTeamId());
            for (Player p : members) {
                int votes = voteCounts.getOrDefault(p.getId(), 0);
                results.add(new VoteResultDisplay(p.getUsername(), votes));
            }
            results.sort((a,b)->Integer.compare(b.votes, a.votes));
            
            nameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
            votesCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().votes).asObject());
            resultsTable.setItems(results);
        });

        new Thread(task).start();

        dialog.getDialogPane().setContent(resultsTable);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static class VoteResultDisplay {
        String name;
        int votes;

        VoteResultDisplay(String name, int votes) {
            this.name = name;
            this.votes = votes;
        }
    }
}
