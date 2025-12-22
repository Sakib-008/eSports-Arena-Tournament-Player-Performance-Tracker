package com.esports.arena.tabs;

import java.util.List;
import java.util.Optional;

import com.esports.arena.MainApp;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

public class TeamsTabController {
    @FXML private TableView<Team> teamsTable;
    @FXML private TableColumn<Team, String> teamNameCol;
    @FXML private TableColumn<Team, String> teamTagCol;
    @FXML private TableColumn<Team, String> teamRegionCol;
    @FXML private TableColumn<Team, Integer> teamWinsCol;
    @FXML private TableColumn<Team, Integer> teamLossesCol;
    @FXML private TableColumn<Team, Double> teamWinRateCol;
    @FXML private Button addTeamBtn;
    @FXML private Button editTeamBtn;
    @FXML private Button deleteTeamBtn;
    @FXML private Button viewTeamPlayersBtn;

    private TeamDAO teamDAO;
    private PlayerDAO playerDAO;
    private ObservableList<Team> teamsData;

    public void initialize(TeamDAO teamDAO, PlayerDAO playerDAO) {
        this.teamDAO = teamDAO;
        this.playerDAO = playerDAO;
        this.teamsData = FXCollections.observableArrayList();
        setupTeamsTable();
        loadTeams();
    }

    private void setupTeamsTable() {
        teamNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        teamTagCol.setCellValueFactory(new PropertyValueFactory<>("tag"));
        teamRegionCol.setCellValueFactory(new PropertyValueFactory<>("region"));
        teamWinsCol.setCellValueFactory(new PropertyValueFactory<>("wins"));
        teamLossesCol.setCellValueFactory(new PropertyValueFactory<>("losses"));
        teamWinRateCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> cellData.getValue().getWinRate()
                ).asObject());

        teamsTable.setItems(teamsData);

        teamsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && teamsTable.getSelectionModel().getSelectedItem() != null) {
                handleViewTeamPlayers();
            }
        });
    }

    public void loadTeams() {
        Task<List<Team>> loadTeamsTask = new Task<>() {
            @Override
            protected List<Team> call() {
                return teamDAO.getAllTeams();
            }
        };

        loadTeamsTask.setOnSucceeded(e -> {
            teamsData.setAll(loadTeamsTask.getValue());
        });

        loadTeamsTask.setOnFailed(e -> MainApp.showError("Error", "Failed to load teams"));

        new Thread(loadTeamsTask).start();
    }

    @FXML
    private void handleAddTeam() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Team");
        dialog.setHeaderText("Create New Team");
        dialog.setContentText("Team Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                showTeamDialog(null, name);
            }
        });
    }

    private void showTeamDialog(Team team, String initialName) {
        Dialog<Team> dialog = new Dialog<>();
        dialog.setTitle(team == null ? "Create Team" : "Edit Team");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField(team != null ? team.getName() : initialName);
        TextField tagField = new TextField(team != null ? team.getTag() : "");
        TextField regionField = new TextField(team != null ? team.getRegion() : "");

        grid.add(new Label("Team Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Team Tag:"), 0, 1);
        grid.add(tagField, 1, 1);
        grid.add(new Label("Region:"), 0, 2);
        grid.add(regionField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Team t = team != null ? team : new Team();
                t.setName(nameField.getText());
                t.setTag(tagField.getText());
                t.setRegion(regionField.getText());
                return t;
            }
            return null;
        });

        Optional<Team> result = dialog.showAndWait();
        result.ifPresent(this::saveTeam);
    }

    private void saveTeam(Team team) {
        Task<Integer> saveTask = new Task<>() {
            @Override
            protected Integer call() {
                if (team.getId() == 0) {
                    return teamDAO.createTeam(team);
                } else {
                    teamDAO.updateTeam(team);
                    return team.getId();
                }
            }
        };

        saveTask.setOnSucceeded(e -> {
            MainApp.showInfo("Success", "Team saved successfully");
            loadTeams();
        });

        saveTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to save team"));

        new Thread(saveTask).start();
    }

    @FXML
    private void handleEditTeam() {
        Team selected = teamsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showTeamDialog(selected, selected.getName());
        } else {
            MainApp.showError("No Selection", "Please select a team to edit");
        }
    }

    @FXML
    private void handleDeleteTeam() {
        Team selected = teamsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setContentText("Delete team: " + selected.getName() + "?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    Task<Boolean> deleteTask = new Task<>() {
                        @Override
                        protected Boolean call() {
                            return teamDAO.deleteTeam(selected.getId());
                        }
                    };

                    deleteTask.setOnSucceeded(e -> {
                        if (deleteTask.getValue()) {
                            MainApp.showInfo("Success", "Team deleted successfully");
                            loadTeams();
                        } else {
                            MainApp.showError("Error", "Failed to delete team");
                        }
                    });

                    new Thread(deleteTask).start();
                }
            });
        } else {
            MainApp.showError("No Selection", "Please select a team to delete");
        }
    }

    @FXML
    private void handleViewTeamPlayers() {
        Team selected = teamsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showTeamPlayersDialog(selected);
        } else {
            MainApp.showError("No Selection", "Please select a team to view players");
        }
    }

    private void showTeamPlayersDialog(Team team) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Team Players - " + team.getName());
        dialog.setHeaderText("Members of " + team.getName());

        ListView<Player> playersList = new ListView<>();
        playersList.setPrefWidth(400);
        playersList.setPrefHeight(300);

        Task<List<Player>> loadTask = new Task<>() {
            @Override
            protected List<Player> call() {
                return playerDAO.getPlayersByTeam(team.getId());
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Player> players = loadTask.getValue();
            playersList.setItems(FXCollections.observableArrayList(players));
            playersList.setCellFactory(param -> new ListCell<Player>() {
                @Override
                protected void updateItem(Player item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        String status = item.isAvailable() ? "✓" : "✗";
                        setText(status + " " + item.getUsername() + " - " + item.getRole() +
                                " (K/D: " + String.format("%.2f", item.getKdRatio()) + ")");
                    }
                }
            });
        });

        new Thread(loadTask).start();

        dialog.getDialogPane().setContent(playersList);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    public ObservableList<Team> getTeamsData() {
        return teamsData;
    }
}

