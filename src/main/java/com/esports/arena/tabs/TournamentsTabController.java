package com.esports.arena.tabs;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.esports.arena.MainApp;
import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.dao.TournamentDAO;
import com.esports.arena.model.Match;
import com.esports.arena.model.Team;
import com.esports.arena.model.Tournament;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class TournamentsTabController {
    @FXML private ListView<Tournament> tournamentsList;
    @FXML private Label tournamentNameLabel;
    @FXML private Label tournamentGameLabel;
    @FXML private Label tournamentStatusLabel;
    @FXML private Label tournamentPrizeLabel;
    @FXML private Label tournamentTeamsLabel;
    @FXML private Button createTournamentBtn;
    @FXML private Button startTournamentBtn;
    @FXML private Button manageTeamsBtn;
    @FXML private Button createMatchBtn;
    @FXML private Button viewMatchesBtn;

    private TournamentDAO tournamentDAO;
    private MatchDAO matchDAO;
    private ObservableList<Tournament> tournamentsData;
    private ObservableList<Team> teamsData;
    private MatchesTabController matchesTabController;

    public void initialize(TournamentDAO tournamentDAO, MatchDAO matchDAO, TeamDAO teamDAO, 
                          ObservableList<Team> teamsData, MatchesTabController matchesTabController) {
        this.tournamentDAO = tournamentDAO;
        this.matchDAO = matchDAO;
        this.teamsData = teamsData;
        this.matchesTabController = matchesTabController;
        this.tournamentsData = FXCollections.observableArrayList();
        setupTournamentsList();
        loadTournaments();
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

    public void loadTournaments() {
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

    public void updateTournamentsList() {
        // Real-time update method for background refresh
        List<Tournament> tournaments = tournamentDAO.getAllTournaments();
        if (tournaments != null) {
            tournamentsData.setAll(tournaments);
        }
    }

    private void updateTournamentDetails(Tournament tournament) {
        if (tournament != null) {
            tournamentNameLabel.setText(tournament.getName());
            tournamentGameLabel.setText(tournament.getGame());
            tournamentStatusLabel.setText(tournament.getStatus().toString());
            tournamentPrizeLabel.setText(String.format("$%.2f", tournament.getPrizePool()));
            
            // Load and display registered teams count
            Task<List<Team>> loadTeamsTask = new Task<>() {
                @Override
                protected List<Team> call() {
                    return tournamentDAO.getRegisteredTeams(tournament.getId());
                }
            };
            
            loadTeamsTask.setOnSucceeded(e -> {
                List<Team> registeredTeams = loadTeamsTask.getValue();
                tournamentTeamsLabel.setText(registeredTeams.size() + "/" + tournament.getMaxTeams());
            });
            
            new Thread(loadTeamsTask).start();
        } else {
            tournamentNameLabel.setText("-");
            tournamentGameLabel.setText("-");
            tournamentStatusLabel.setText("-");
            tournamentPrizeLabel.setText("-");
            tournamentTeamsLabel.setText("-");
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
        gameField.setPromptText("Game (e.g., VALORANT)");
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
                return tournamentDAO.createTournament(tournament);
            }
        };

        saveTask.setOnSucceeded(e -> {
            if (saveTask.getValue() > 0) {
                MainApp.showInfo("Success", "Tournament created successfully!");
                loadTournaments();
            } else {
                MainApp.showError("Error", "Failed to create tournament");
            }
        });

        saveTask.setOnFailed(e ->
                MainApp.showError("Error", "Failed to create tournament: " + e.getSource().getException().getMessage()));

        new Thread(saveTask).start();
    }

    @FXML
    private void handleStartTournament() {
        Tournament selected = tournamentsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a tournament to start");
            return;
        }

        if (selected.getStatus() != Tournament.TournamentStatus.REGISTRATION_OPEN) {
            MainApp.showError("Invalid Status", "Tournament must be in REGISTRATION_OPEN status");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Start Tournament");
        alert.setHeaderText("Start " + selected.getName() + "?");
        alert.setContentText("This will close registration and begin the tournament.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Boolean> task = new Task<>() {
                    @Override
                    protected Boolean call() {
                        selected.setStatus(Tournament.TournamentStatus.IN_PROGRESS);
                        return tournamentDAO.updateTournament(selected);
                    }
                };

                task.setOnSucceeded(e -> {
                    if (task.getValue()) {
                        MainApp.showInfo("Success", "Tournament started successfully!");
                        loadTournaments();
                    } else {
                        MainApp.showError("Error", "Failed to start tournament");
                    }
                });

                new Thread(task).start();
            }
        });
    }

    @FXML
    private void handleViewMatches() {
        Tournament selected = tournamentsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a tournament");
            return;
        }

        // Load matches for the selected tournament
        if (matchesTabController != null) {
            matchesTabController.loadMatchesForTournament(selected.getId());
            // Switch to matches tab
            javafx.application.Platform.runLater(() -> {
                javafx.scene.Node node = tournamentsList.getScene().lookup("#mainTabPane");
                if (node instanceof javafx.scene.control.TabPane) {
                    javafx.scene.control.TabPane tabPane = (javafx.scene.control.TabPane) node;
                    tabPane.getSelectionModel().select(4); // Switch to Matches tab
                }
            });
        } else {
            MainApp.showError("Error", "Matches tab not initialized");
        }
    }

    @FXML
    private void handleManageTeams() {
        Tournament selected = tournamentsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a tournament");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Tournament Teams: " + selected.getName());
        dialog.setHeaderText("Add/Remove Teams");

        ButtonType addButtonType = new ButtonType("Add Teams", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CLOSE);

        VBox content = new VBox(10);
        Label label = new Label("Select teams to add to this tournament:");
        
        // Get already registered teams
        List<Team> registeredTeams = tournamentDAO.getRegisteredTeams(selected.getId());
        ListView<Team> teamsList = new ListView<>();
        teamsList.setItems(teamsData);
        teamsList.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        // Show registered teams
        Label registeredLabel = new Label("Registered Teams: " + registeredTeams.size() + "/" + selected.getMaxTeams());
        ListView<Team> registeredList = new ListView<>();
        registeredList.setItems(FXCollections.observableArrayList(registeredTeams));
        registeredList.setCellFactory(param -> new ListCell<Team>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " [" + item.getTag() + "]");
                }
            }
        });

        content.getChildren().addAll(registeredLabel, registeredList, new javafx.scene.control.Separator(), label, teamsList);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefSize(500, 600);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                List<Team> selectedTeams = teamsList.getSelectionModel().getSelectedItems();
                if (selectedTeams.isEmpty()) {
                    MainApp.showError("No Selection", "Please select at least one team");
                    return null;
                }

                if (registeredTeams.size() + selectedTeams.size() > selected.getMaxTeams()) {
                    MainApp.showError("Limit Exceeded", "Cannot add more teams. Tournament limit: " + selected.getMaxTeams());
                    return null;
                }

                Task<Integer> registerTask = new Task<>() {
                    @Override
                    protected Integer call() {
                        int count = 0;
                        for (Team team : selectedTeams) {
                            // Check if team is already registered
                            List<Team> existing = tournamentDAO.getRegisteredTeams(selected.getId());
                            boolean alreadyRegistered = existing.stream()
                                    .anyMatch(t -> t.getId() == team.getId());
                            
                            if (!alreadyRegistered && tournamentDAO.registerTeam(selected.getId(), team.getId())) {
                                count++;
                            }
                        }
                        return count;
                    }
                };

                registerTask.setOnSucceeded(e -> {
                    int count = registerTask.getValue();
                    MainApp.showInfo("Success", "Registered " + count + " team(s) to tournament");
                    loadTournaments(); // Refresh tournament list
                });

                registerTask.setOnFailed(e ->
                        MainApp.showError("Error", "Failed to register teams"));

                new Thread(registerTask).start();
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleCreateMatch() {
        Tournament selected = tournamentsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainApp.showError("No Selection", "Please select a tournament");
            return;
        }

        if (selected.getStatus() == Tournament.TournamentStatus.UPCOMING) {
            MainApp.showError("Invalid Status", "Tournament must be started before creating matches");
            return;
        }

        // Get registered teams
        List<Team> registeredTeams = tournamentDAO.getRegisteredTeams(selected.getId());
        if (registeredTeams.size() < 2) {
            MainApp.showError("Not Enough Teams", "Tournament needs at least 2 registered teams to create matches");
            return;
        }

        Dialog<Match> dialog = new Dialog<>();
        dialog.setTitle("Create Match");
        dialog.setHeaderText("Create a new match for: " + selected.getName());

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<Team> team1Combo = new ComboBox<>();
        ComboBox<Team> team2Combo = new ComboBox<>();
        team1Combo.setItems(FXCollections.observableArrayList(registeredTeams));
        team2Combo.setItems(FXCollections.observableArrayList(registeredTeams));
        team1Combo.setPrefWidth(250);
        team2Combo.setPrefWidth(250);

        team1Combo.setCellFactory(param -> new ListCell<Team>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " [" + item.getTag() + "]");
            }
        });

        team2Combo.setCellFactory(param -> new ListCell<Team>() {
            @Override
            protected void updateItem(Team item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " [" + item.getTag() + "]");
            }
        });

        TextField roundField = new TextField();
        roundField.setPromptText("e.g., Round 1, Quarterfinals, Semifinals");
        roundField.setText("Round 1");

        javafx.scene.control.DatePicker datePicker = new javafx.scene.control.DatePicker();
        datePicker.setValue(LocalDate.now().plusDays(1));
        javafx.scene.control.TextField timeField = new javafx.scene.control.TextField();
        timeField.setPromptText("Time (e.g., 14:00)");
        timeField.setText("14:00");

        grid.add(new Label("Team 1:"), 0, 0);
        grid.add(team1Combo, 1, 0);
        grid.add(new Label("Team 2:"), 0, 1);
        grid.add(team2Combo, 1, 1);
        grid.add(new Label("Round:"), 0, 2);
        grid.add(roundField, 1, 2);
        grid.add(new Label("Date:"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Time:"), 0, 4);
        grid.add(timeField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Team t1 = team1Combo.getValue();
                Team t2 = team2Combo.getValue();
                
                if (t1 == null || t2 == null) {
                    MainApp.showError("Invalid Input", "Please select both teams");
                    return null;
                }
                
                if (t1.getId() == t2.getId()) {
                    MainApp.showError("Invalid Input", "Team 1 and Team 2 must be different");
                    return null;
                }

                try {
                    java.time.LocalDate date = datePicker.getValue();
                    String timeStr = timeField.getText().trim();
                    java.time.LocalTime time = java.time.LocalTime.parse(timeStr);
                    java.time.LocalDateTime scheduledTime = java.time.LocalDateTime.of(date, time);

                    Match match = new Match(
                            selected.getId(),
                            t1.getId(),
                            t2.getId(),
                            scheduledTime,
                            roundField.getText()
                    );
                    match.setStatus(Match.MatchStatus.SCHEDULED);
                    return match;
                } catch (Exception e) {
                    MainApp.showError("Invalid Input", "Please enter valid date and time (HH:mm format)");
                    return null;
                }
            }
            return null;
        });

        Optional<Match> result = dialog.showAndWait();
        result.ifPresent(match -> {
            Task<Integer> createTask = new Task<>() {
                @Override
                protected Integer call() {
                    return matchDAO.createMatch(match);
                }
            };

            createTask.setOnSucceeded(e -> {
                if (createTask.getValue() > 0) {
                    MainApp.showInfo("Success", "Match created successfully!");
                    // Refresh matches if matches tab controller is available
                    if (matchesTabController != null) {
                        matchesTabController.loadMatchesForTournament(selected.getId());
                    }
                } else {
                    MainApp.showError("Error", "Failed to create match");
                }
            });

            createTask.setOnFailed(e ->
                    MainApp.showError("Error", "Failed to create match: " + createTask.getException().getMessage()));

            new Thread(createTask).start();
        });
    }
}

