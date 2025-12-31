package com.esports.arena.util;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoadingDialog {
    private Stage loadingStage;
    private static LoadingDialog instance;

    private LoadingDialog() {
        // Private constructor for singleton
    }

    public static LoadingDialog getInstance() {
        if (instance == null) {
            instance = new LoadingDialog();
        }
        return instance;
    }

    public void show(String message) {
        Platform.runLater(() -> {
            if (loadingStage != null && loadingStage.isShowing()) {
                return; // Already showing
            }

            loadingStage = new Stage();
            loadingStage.initStyle(StageStyle.UNDECORATED);
            loadingStage.initModality(Modality.APPLICATION_MODAL);

            VBox vbox = new VBox(15);
            vbox.setAlignment(Pos.CENTER);
            vbox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); " +
                         "-fx-padding: 30; " +
                         "-fx-background-radius: 10; " +
                         "-fx-border-color: #1976D2; " +
                         "-fx-border-width: 2; " +
                         "-fx-border-radius: 10;");

            ProgressIndicator progressIndicator = new ProgressIndicator();
            progressIndicator.setPrefSize(60, 60);

            Label label = new Label(message);
            label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #333;");

            vbox.getChildren().addAll(progressIndicator, label);

            Scene scene = new Scene(vbox);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            loadingStage.setScene(scene);
            loadingStage.setAlwaysOnTop(true);

            loadingStage.show();
        });
    }

    public void hide() {
        Platform.runLater(() -> {
            if (loadingStage != null && loadingStage.isShowing()) {
                loadingStage.close();
                loadingStage = null;
            }
        });
    }

    public static void showLoading(String message) {
        getInstance().show(message);
    }

    public static void hideLoading() {
        getInstance().hide();
    }
}
