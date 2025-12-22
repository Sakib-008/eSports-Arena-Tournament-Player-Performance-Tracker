package com.esports.arena;

import java.io.IOException;

import com.esports.arena.database.DatabaseManager;
import com.esports.arena.model.Player;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class MainApp extends Application {
    private Stage primaryStage;
    private Player currentPlayer;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("eSports Arena - Tournament & Player Performance Tracker");

        // Set fullscreen
//        primaryStage.setMaximized(true);
//        primaryStage.setFullScreen(false); // Use maximized instead of fullscreen for better control

        showMainMenu();

        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            handleExit();
        });

        primaryStage.show();
    }

    public void showMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/MainMenu.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            MainMenuController controller = loader.getController();
            controller.setMainApp(this);
            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Failed to load main menu: " + e.getMessage());
            showError("Application Error", "Failed to load main menu: " + e.getMessage());
        }
    }

    public void showOrganizerLogin() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/OrganizerLogin.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            OrganizerLoginController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Failed to load organizer login: " + e.getMessage());
            showError("Application Error", "Failed to load organizer login: " + e.getMessage());
        }
    }

    public void showOrganizerDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/OrganizerDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            OrganizerDashboardController controller = loader.getController();
            controller.setMainApp(this);
            // Inject MainApp into any tab controllers that need it
            controller.injectMainAppToTabs();

            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Failed to load organizer dashboard: " + e.getMessage());
            showError("Application Error", "Failed to load organizer dashboard: " + e.getMessage());
        }
    }

    public void showPlayerDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/PlayerDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            PlayerDashboardController controller = loader.getController();
            controller.setMainApp(this);
            if (currentPlayer != null) {
                controller.setCurrentPlayer(currentPlayer);
                // Refresh player data to get latest team assignment
                controller.refreshPlayerData();
            }

            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Failed to load player dashboard: " + e.getMessage());
            showError("Application Error", "Failed to load player dashboard: " + e.getMessage());
        }
    }

    public void showPlayerLogin() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/PlayerLogin.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            PlayerLoginController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Failed to load player login: " + e.getMessage());
            showError("Application Error", "Failed to load player login: " + e.getMessage());
        }
    }

    public void showPlayerSignup() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/PlayerSignup.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            PlayerSignupController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setScene(scene);
        } catch (IOException e) {
            System.err.println("Failed to load player signup: " + e.getMessage());
            showError("Application Error", "Failed to load player signup: " + e.getMessage());
        }
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    private void handleExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Application");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved data will be lost.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Close database connection
                DatabaseManager.getInstance().close();
                primaryStage.close();
                System.exit(0);
            }
        });
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}