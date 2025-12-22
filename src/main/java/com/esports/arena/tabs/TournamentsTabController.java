package com.esports.arena.tabs;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.esports.arena.MainApp;
import com.esports.arena.dao.MatchDAO;
import com.esports.arena.dao.TeamDAO;
import com.esports.arena.dao.TournamentDAO;
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
    @FXML private Button createTournamentBtn;
    @FXML private Button startTournamentBtn;
    @FXML private Button viewMatchesBtn;

    private TournamentDAO tournamentDAO;
    private ObservableList<Tournament> tournamentsData;
    private ObservableList<Team> teamsData;
    private Runnable onMatchesRequested;

    public void initialize(TournamentDAO tournamentDAO, MatchDAO matchDAO, TeamDAO teamDAO, 
                          ObservableList<Team> teamsData, Runnable onMatchesRequested) {
        this.tournamentDAO = tournamentDAO;
        this.teamsData = teamsData;
        this.onMatchesRequested = onMatchesRequested;
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

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Manage Tournament: " + selected.getName());
        dialog.setHeaderText("Add/Remove Teams");

        ButtonType addButtonType = new ButtonType("Add Teams", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CLOSE);

        VBox content = new VBox(10);
        Label label = new Label("Select teams to add to this tournament:");
        ListView<Team> teamsList = new ListView<>();
        teamsList.setItems(teamsData);
        teamsList.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);

        content.getChildren().addAll(label, teamsList);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                List<Team> selectedTeams = teamsList.getSelectionModel().getSelectedItems();
                for (Team selectedTeam : selectedTeams) {
                    // TODO: Implement tournament team registration
                    // tournamentDAO.registerTeam(selected.getId(), selectedTeam.getId());
                    System.out.println("Would register team: " + selectedTeam.getName() + " to tournament");
                }
            }
            return null;
        });

        dialog.showAndWait();

        if (onMatchesRequested != null) {
            onMatchesRequested.run();
        }
    }
}

