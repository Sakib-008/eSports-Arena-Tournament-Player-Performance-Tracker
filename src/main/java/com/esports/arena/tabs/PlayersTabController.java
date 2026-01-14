package com.esports.arena.tabs;

import java.util.List;
import java.util.Optional;

import com.esports.arena.MainApp;
import com.esports.arena.dao.PlayerDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.model.Player;
import com.esports.arena.model.Team;
import com.esports.arena.util.LoadingDialog;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class PlayersTabController {
    @FXML private TableView<Player> playersTable;
    @FXML private TableColumn<Player, String> playerUsernameCol;
    @FXML private TableColumn<Player, String> playerRealNameCol;
    @FXML private TableColumn<Player, String> playerRoleCol;
    @FXML private TableColumn<Player, String> playerAvailabilityCol;
    @FXML private TableColumn<Player, Integer> playerKillsCol;
    @FXML private TableColumn<Player, Integer> playerDeathsCol;
    @FXML private TableColumn<Player, Double> playerKDCol;
    @FXML private Button editPlayerBtn;
    @FXML private Button deletePlayerBtn;
    @FXML private Button assignTeamBtn;

    private PlayerDAO playerDAO;
    private ObservableList<Player> playersData;
    private ObservableList<Team> teamsData;
    private com.esports.arena.MainApp mainApp;
    private Runnable onPlayerUpdateCallback;

    public void initialize(PlayerDAO playerDAO, TeamDAO teamDAO, ObservableList<Team> teamsData) {
        this.playerDAO = playerDAO;
        this.teamsData = teamsData;
        this.playersData = FXCollections.observableArrayList();
        setupPlayersTable();
        loadPlayers();
    }

    public void setOnPlayerUpdateCallback(Runnable callback) {
        this.onPlayerUpdateCallback = callback;
    }

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    private void setupPlayersTable() {
        playerUsernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        playerRealNameCol.setCellValueFactory(new PropertyValueFactory<>("realName"));
        playerRoleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        playerAvailabilityCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> cellData.getValue().isAvailable() ? "Available" : "Unavailable"
                ));
        playerKillsCol.setCellValueFactory(new PropertyValueFactory<>("totalKills"));
        playerDeathsCol.setCellValueFactory(new PropertyValueFactory<>("totalDeaths"));
        playerKDCol.setCellValueFactory(cellData ->
                javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> cellData.getValue().getKdRatio()
                ).asObject());

        playersTable.setItems(playersData);
    }

    public void loadPlayers() {
        LoadingDialog.showLoading("Loading players...");
        Task<List<Player>> loadPlayersTask = new Task<>() {
            @Override
            protected List<Player> call() {
                return playerDAO.getAllPlayers();
            }
        };

        loadPlayersTask.setOnSucceeded(e -> {
                playersData.setAll(loadPlayersTask.getValue());
                LoadingDialog.hideLoading();
        });

        loadPlayersTask.setOnFailed(e -> {
                MainApp.showError("Error", "Failed to load players");
                LoadingDialog.hideLoading();
        });

        new Thread(loadPlayersTask).start();
    }

    public void updatePlayersList() {
        System.out.println("PlayersTabController.updatePlayersList() called");
        LoadingDialog.showLoading("Refreshing players...");
        Task<List<Player>> task = new Task<>() {
            @Override
            protected List<Player> call() {
                System.out.println("  PlayersTabController - Loading all players");
                return playerDAO.getAllPlayers();
            }
        };
        
        task.setOnSucceeded(e -> {
            List<Player> players = task.getValue();
            System.out.println("  PlayersTabController - Loaded " + (players != null ? players.size() : 0) + " players");
            if (players != null) {
                playersData.setAll(players);
            }
            LoadingDialog.hideLoading();
        });
        
        task.setOnFailed(e -> {
            System.err.println("  PlayersTabController - Load failed");
            LoadingDialog.hideLoading();
        });
        
        new Thread(task).start();
    }

    @FXML
    private void handleEditPlayer() {
        Player selected = playersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showPlayerEditDialog(selected);
        } else {
            MainApp.showError("No Selection", "Please select a player to edit");
        }
    }

    private void showPlayerEditDialog(Player player) {
        Dialog<Player> dialog = new Dialog<>();
        dialog.setTitle("Edit Player");
        dialog.setHeaderText("Edit Player: " + player.getUsername());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField(player.getUsername());
        TextField realNameField = new TextField(player.getRealName());
        TextField emailField = new TextField(player.getEmail());
        TextField roleField = new TextField(player.getRole());

        ComboBox<Team> teamCombo = new ComboBox<>();
        teamCombo.setItems(teamsData);
        teamCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " [" + item.getTag() + "]");
            }
        });
        teamCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "No Team" : item.getName() + " [" + item.getTag() + "]");
            }
        });

        if (player.getTeamId() != null) {
            teamsData.stream()
                    .filter(t -> t.getId() == player.getTeamId())
                    .findFirst()
                    .ifPresent(teamCombo::setValue);
        }

        CheckBox availableCheckBox = new CheckBox("Available");
        availableCheckBox.setSelected(player.isAvailable());

        TextField killsField = new TextField(String.valueOf(player.getTotalKills()));
        TextField deathsField = new TextField(String.valueOf(player.getTotalDeaths()));
        TextField assistsField = new TextField(String.valueOf(player.getTotalAssists()));
        TextField matchesPlayedField = new TextField(String.valueOf(player.getMatchesPlayed()));
        TextField matchesWonField = new TextField(String.valueOf(player.getMatchesWon()));

        boolean isSelf;
        if (this.mainApp != null && this.mainApp.getCurrentPlayer() != null) {
            isSelf = this.mainApp.getCurrentPlayer().getId() == player.getId();
        } else {
            isSelf = false;
        }

        if (isSelf) {
            // Prevent players from editing their own recorded statistics
            killsField.setDisable(true);
            deathsField.setDisable(true);
            assistsField.setDisable(true);
            matchesPlayedField.setDisable(true);
            matchesWonField.setDisable(true);
        }

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Real Name:"), 0, 1);
        grid.add(realNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleField, 1, 3);
        grid.add(new Label("Team:"), 0, 4);
        grid.add(teamCombo, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(availableCheckBox, 1, 5);
        grid.add(new Separator(), 0, 6);
        grid.add(new Label("Total Kills:"), 0, 7);
        grid.add(killsField, 1, 7);
        grid.add(new Label("Total Deaths:"), 0, 8);
        grid.add(deathsField, 1, 8);
        grid.add(new Label("Total Assists:"), 0, 9);
        grid.add(assistsField, 1, 9);
        grid.add(new Label("Matches Played:"), 0, 10);
        grid.add(matchesPlayedField, 1, 10);
        grid.add(new Label("Matches Won:"), 0, 11);
        grid.add(matchesWonField, 1, 11);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                player.setUsername(usernameField.getText());
                player.setRealName(realNameField.getText());
                player.setEmail(emailField.getText());
                player.setRole(roleField.getText());
                player.setAvailable(availableCheckBox.isSelected());

                Team selectedTeam = teamCombo.getValue();
                player.setTeamId(selectedTeam != null ? selectedTeam.getId() : null);

                try {
                    if (!isSelf) {
                        player.setTotalKills(Integer.parseInt(killsField.getText()));
                        player.setTotalDeaths(Integer.parseInt(deathsField.getText()));
                        player.setTotalAssists(Integer.parseInt(assistsField.getText()));
                        player.setMatchesPlayed(Integer.parseInt(matchesPlayedField.getText()));
                        player.setMatchesWon(Integer.parseInt(matchesWonField.getText()));
                    }
                } catch (NumberFormatException e) {
                    MainApp.showError("Invalid Input", "Please enter valid numbers for stats");
                    return null;
                }

                return player;
            }
            return null;
        });

        Optional<Player> result = dialog.showAndWait();
        result.ifPresent(this::updatePlayer);
    }

    private void updatePlayer(Player player) {
        System.out.println("\n=== PlayersTabController.updatePlayer() START ===");
        System.out.println("Player ID: " + player.getId() + ", Username: " + player.getUsername());
        Task<Boolean> updateTask = new Task<>() {
            @Override
            protected Boolean call() {
                System.out.println("  Background task: Updating player in database");
                return playerDAO.updatePlayer(player);
            }
        };

        updateTask.setOnSucceeded(e -> {
            System.out.println("  Update task succeeded");
            if (updateTask.getValue()) {
                MainApp.showInfo("Success", "Player updated successfully");
                loadPlayers();
                if (onPlayerUpdateCallback != null) {
                    onPlayerUpdateCallback.run();
                }
            } else {
                MainApp.showError("Error", "Failed to update player");
            }
            System.out.println("=== PlayersTabController.updatePlayer() END ===\n");
        });

        updateTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to update player"));

        new Thread(updateTask).start();
    }

    @FXML
    private void handleAssignTeam() {
        Player selected = playersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a player to assign to a team");
            return;
        }

        Dialog<Team> dialog = new Dialog<>();
        dialog.setTitle("Assign to Team");
        dialog.setHeaderText("Assign " + selected.getUsername() + " to a team");

        ButtonType assignButtonType = new ButtonType("Assign", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(assignButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        Label label = new Label("Select Team:");
        ComboBox<Team> teamCombo = new ComboBox<>();
        teamCombo.setItems(teamsData);
        teamCombo.setPrefWidth(300);

        teamCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " [" + item.getTag() + "]");
            }
        });

        teamCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Select a team" : item.getName() + " [" + item.getTag() + "]");
            }
        });

        if (selected.getTeamId() != null) {
            teamsData.stream()
                    .filter(t -> t.getId() == selected.getTeamId())
                    .findFirst()
                    .ifPresent(teamCombo::setValue);
        }

        content.getChildren().addAll(label, teamCombo);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == assignButtonType) {
                return teamCombo.getValue();
            }
            return null;
        });

        Optional<Team> result = dialog.showAndWait();
        result.ifPresent(team -> {
            selected.setTeamId(team != null ? team.getId() : null);
            updatePlayer(selected);
        });
    }

    @FXML
    private void handleDeletePlayer() {
        Player selected = playersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Delete");
            alert.setContentText("Delete player: " + selected.getUsername() + "?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    Task<Boolean> deleteTask = new Task<>() {
                        @Override
                        protected Boolean call() {
                            return playerDAO.deletePlayer(selected.getId());
                        }
                    };

                    deleteTask.setOnSucceeded(e -> {
                        if (deleteTask.getValue()) {
                            MainApp.showInfo("Success", "Player deleted successfully");
                            loadPlayers();
                        } else {
                            MainApp.showError("Error", "Failed to delete player");
                        }
                    });

                    new Thread(deleteTask).start();
                }
            });
        } else {
            MainApp.showError("No Selection", "Please select a player to delete");
        }
    }
}

