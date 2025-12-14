package com.esports.arena;

import com.esports.arena.dao.*;
import com.esports.arena.model.*;
import com.esports.arena.service.JsonExportImportService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class OrganizerDashboardController {
    // Tab Pane
    @FXML private TabPane mainTabPane;

    // Teams Tab
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

    // Players Tab
    @FXML private TableView<Player> playersTable;
    @FXML private TableColumn<Player, String> playerUsernameCol;
    @FXML private TableColumn<Player, String> playerRealNameCol;
    @FXML private TableColumn<Player, String> playerRoleCol;
    @FXML private TableColumn<Player, String> playerAvailabilityCol;
    @FXML private TableColumn<Player, Integer> playerKillsCol;
    @FXML private TableColumn<Player, Integer> playerDeathsCol;
    @FXML private TableColumn<Player, Double> playerKDCol;
    @FXML private Button addPlayerBtn;
    @FXML private Button editPlayerBtn;
    @FXML private Button deletePlayerBtn;
    @FXML private Button assignTeamBtn;

    // Tournaments Tab
    @FXML private ListView<Tournament> tournamentsList;
    @FXML private Label tournamentNameLabel;
    @FXML private Label tournamentGameLabel;
    @FXML private Label tournamentStatusLabel;
    @FXML private Label tournamentPrizeLabel;
    @FXML private Button createTournamentBtn;
    @FXML private Button startTournamentBtn;
    @FXML private Button viewMatchesBtn;

    // Leaderboard Tab
    @FXML private TableView<Team> leaderboardTable;
    @FXML private TableColumn<Team, Integer> lbRankCol;
    @FXML private TableColumn<Team, String> lbTeamCol;
    @FXML private TableColumn<Team, Integer> lbWinsCol;
    @FXML private TableColumn<Team, Integer> lbLossesCol;
    @FXML private TableColumn<Team, Double> lbWinRateCol;
    @FXML private Button refreshLeaderboardBtn;

    // Export/Import
    @FXML private Button exportDataBtn;
    @FXML private Button importDataBtn;
    @FXML private Button backToMenuBtn;

    private MainApp mainApp;
    private TeamDAO teamDAO;
    private PlayerDAO playerDAO;
    private TournamentDAO tournamentDAO;
    private JsonExportImportService jsonService;

    private ObservableList<Team> teamsData;
    private ObservableList<Player> playersData;
    private ObservableList<Tournament> tournamentsData;

    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    @FXML
    private void initialize() {
        // Initialize DAOs and services
        teamDAO = new TeamDAO();
        playerDAO = new PlayerDAO();
        tournamentDAO = new TournamentDAO();
        jsonService = new JsonExportImportService();

        // Initialize observable lists
        teamsData = FXCollections.observableArrayList();
        playersData = FXCollections.observableArrayList();
        tournamentsData = FXCollections.observableArrayList();

        // Setup tables
        setupTeamsTable();
        setupPlayersTable();
        setupLeaderboardTable();
        setupTournamentsList();

        // Load initial data
        loadAllData();
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

        // Double-click to view details
        teamsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && teamsTable.getSelectionModel().getSelectedItem() != null) {
                handleViewTeamPlayers();
            }
        });
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

    private void setupTournamentsList() {
        tournamentsList.setItems(tournamentsData);
        tournamentsList.setCellFactory(param -> new ListCell<Tournament>() {
            @Override
            protected void updateItem(Tournament item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - " + item.getStatus());
                }
            }
        });

        tournamentsList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> updateTournamentDetails(newVal));
    }

    private void loadAllData() {
        // Load teams in background
        Task<List<Team>> loadTeamsTask = new Task<>() {
            @Override
            protected List<Team> call() {
                return teamDAO.getAllTeams();
            }
        };

        loadTeamsTask.setOnSucceeded(e -> {
            teamsData.setAll(loadTeamsTask.getValue());
            updateLeaderboard();
        });

        loadTeamsTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load teams"));

        new Thread(loadTeamsTask).start();

        // Load players in background
        Task<List<Player>> loadPlayersTask = new Task<>() {
            @Override
            protected List<Player> call() {
                return playerDAO.getAllPlayers();
            }
        };

        loadPlayersTask.setOnSucceeded(e ->
                playersData.setAll(loadPlayersTask.getValue()));

        loadPlayersTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load players"));

        new Thread(loadPlayersTask).start();

        // Load tournaments in background
        Task<List<Tournament>> loadTournamentsTask = new Task<>() {
            @Override
            protected List<Tournament> call() {
                return tournamentDAO.getAllTournaments();
            }
        };

        loadTournamentsTask.setOnSucceeded(e ->
                tournamentsData.setAll(loadTournamentsTask.getValue()));

        loadTournamentsTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to load tournaments"));

        new Thread(loadTournamentsTask).start();
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
            loadAllData();
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
                            loadAllData();
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

    @FXML
    private void handleAddPlayer() {
        Dialog<Player> dialog = new Dialog<>();
        dialog.setTitle("Add Player");
        dialog.setHeaderText("Create New Player");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        TextField realNameField = new TextField();
        realNameField.setPromptText("Real Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField roleField = new TextField();
        roleField.setPromptText("Role");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Real Name:"), 0, 1);
        grid.add(realNameField, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Player player = new Player(
                        usernameField.getText(),
                        realNameField.getText(),
                        emailField.getText(),
                        roleField.getText()
                );
                return player;
            }
            return null;
        });

        Optional<Player> result = dialog.showAndWait();
        result.ifPresent(this::savePlayer);
    }

    private void savePlayer(Player player) {
        Task<Integer> saveTask = new Task<>() {
            @Override
            protected Integer call() {
                return playerDAO.createPlayer(player);
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue() > 0) {
                MainApp.showInfo("Success", "Player created successfully");
                loadAllData();
            } else {
                MainApp.showError("Error", "Failed to create player");
            }
        });

        saveTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to create player"));

        new Thread(saveTask).start();
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

        // Team assignment
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

                return player;
            }
            return null;
        });

        Optional<Player> result = dialog.showAndWait();
        result.ifPresent(this::updatePlayer);
    }

    private void updatePlayer(Player player) {
        Task<Boolean> updateTask = new Task<>() {
            @Override
            protected Boolean call() {
                return playerDAO.updatePlayer(player);
            }
        };

        updateTask.setOnSucceeded(e -> {
            if (updateTask.getValue()) {
                MainApp.showInfo("Success", "Player updated successfully");
                loadAllData();
            } else {
                MainApp.showError("Error", "Failed to update player");
            }
        });

        updateTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to update player"));

        new Thread(updateTask).start();
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
                            loadAllData();
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

    @FXML
    private void handleRefreshLeaderboard() {
        updateLeaderboard();
    }

    private void updateLeaderboard() {
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

    private void updateTournamentDetails(Tournament tournament) {
        if (tournament != null) {
            tournamentNameLabel.setText(tournament.getName());
            tournamentGameLabel.setText(tournament.getGame());
            tournamentStatusLabel.setText(tournament.getStatus().toString());
            tournamentPrizeLabel.setText(String.format("$%.2f", tournament.getPrizePool()));
        } else {
            tournamentNameLabel.setText("-");
            tournamentGameLabel.setText("-");
            tournamentStatusLabel.setText("-");
            tournamentPrizeLabel.setText("-");
        }
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
                    return jsonService.exportAllDataAsync(data, file.getAbsolutePath())
                            .join();
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
            // Implement import logic here
            loadAllData();
        }
    }

    @FXML
    private void handleCreateTournament() {
        Dialog<Tournament> dialog = new Dialog<>();
        dialog.setTitle("Create Tournament");
        dialog.setHeaderText("Create New Tournament");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Tournament Name");
        TextField gameField = new TextField();
        gameField.setPromptText("Game (e.g., League of Legends)");
        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll("Single Elimination", "Double Elimination", "Round Robin");
        formatCombo.setValue("Single Elimination");
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setValue(LocalDate.now().plusDays(7));
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setValue(LocalDate.now().plusDays(14));
        TextField prizePoolField = new TextField();
        prizePoolField.setPromptText("Prize Pool (e.g., 10000)");
        TextField maxTeamsField = new TextField();
        maxTeamsField.setPromptText("Max Teams");
        maxTeamsField.setText("16");

        grid.add(new Label("Tournament Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Game:"), 0, 1);
        grid.add(gameField, 1, 1);
        grid.add(new Label("Format:"), 0, 2);
        grid.add(formatCombo, 1, 2);
        grid.add(new Label("Start Date:"), 0, 3);
        grid.add(startDatePicker, 1, 3);
        grid.add(new Label("End Date:"), 0, 4);
        grid.add(endDatePicker, 1, 4);
        grid.add(new Label("Prize Pool ($):"), 0, 5);
        grid.add(prizePoolField, 1, 5);
        grid.add(new Label("Max Teams:"), 0, 6);
        grid.add(maxTeamsField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try {
                    Tournament tournament = new Tournament(
                            nameField.getText(),
                            gameField.getText(),
                            formatCombo.getValue(),
                            startDatePicker.getValue(),
                            endDatePicker.getValue(),
                            Double.parseDouble(prizePoolField.getText()),
                            Integer.parseInt(maxTeamsField.getText())
                    );
                    tournament.setStatus(Tournament.TournamentStatus.REGISTRATION_OPEN);
                    return tournament;
                } catch (NumberFormatException e) {
                    MainApp.showError("Invalid Input", "Please enter valid numbers for prize pool and max teams");
                    return null;
                }
            }
            return null;
        });

        Optional<Tournament> result = dialog.showAndWait();
        result.ifPresent(this::saveTournament);
    }

    private void saveTournament(Tournament tournament) {
        if (tournament == null) return;

        Task<Integer> saveTask = new Task<>() {
            @Override
            protected Integer call() {
                TournamentDAO tournamentDAO = new TournamentDAO();
                int id = tournamentDAO.createTournament(tournament);
                tournamentDAO.shutdown();
                return id;
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue() > 0) {
                MainApp.showInfo("Success", "Tournament created successfully!");
                loadAllData();
            } else {
                MainApp.showError("Error", "Failed to create tournament");
            }
        });

        saveTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to create tournament: " + e.getSource().getException().getMessage()));

        new Thread(saveTask).start();
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
}