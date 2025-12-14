package com.esports.arena;

import com.esports.arena.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("eSports Arena - Tournament & Player Performance Tracker");

        // Set fullscreen
        primaryStage.setMaximized(true);
        primaryStage.setFullScreen(false); // Use maximized instead of fullscreen for better control

        // Show login/selection screen
        showMainMenu();

        // Handle window close event
        primaryStage.setOnCloseRequest(event -> {
            event.consume(); // Prevent immediate close
            handleExit();
        });

        primaryStage.show();
    }

    public void showMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/MainMenu.fxml"));
            Scene scene = new Scene(loader.load(), 800, 600);

            // Pass MainApp reference to controller
            MainMenuController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Application Error", "Failed to load main menu: " + e.getMessage());
        }
    }

    public void showOrganizerDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/OrganizerDashboard.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);

            OrganizerDashboardController controller = loader.getController();
            controller.setMainApp(this);

            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
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

            primaryStage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Application Error", "Failed to load player dashboard: " + e.getMessage());
        }
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