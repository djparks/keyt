package org.openjfx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        var javaVersion = SystemInfo.javaVersion();
        var javafxVersion = SystemInfo.javafxVersion();

        // Center content (will be placed below drop zone)
        var infoLabel = new Label("Hello, JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        var center = new StackPane(infoLabel);

        // Menu bar with Help -> About
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog(stage));
        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(aboutItem);
        MenuBar menuBar = new MenuBar(helpMenu);

        // Drag-and-drop zone just below the menu
        Label dropText = new Label("Drop files here");
        StackPane dropZone = new StackPane(dropText);
        dropZone.setStyle("-fx-border-color: #888; -fx-border-width: 2; -fx-border-style: dashed; -fx-background-color: rgba(0,0,0,0.02);");
        dropZone.setMinHeight(120);

        // DnD handlers
        dropZone.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db != null && db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db != null && db.hasFiles()) {
                List<File> files = db.getFiles();
                StringBuilder sb = new StringBuilder();
                sb.append("Dropped ").append(files.size()).append(" file(s):\n");
                int maxShow = Math.min(files.size(), 5);
                for (int i = 0; i < maxShow; i++) {
                    sb.append(files.get(i).getName()).append("\n");
                }
                if (files.size() > maxShow) {
                    sb.append("...");
                }
                dropText.setText(sb.toString());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Layout: Menu at top, then drop zone, then original center content
        VBox content = new VBox(10);
        content.getChildren().addAll(dropZone, center);
        VBox.setVgrow(dropZone, Priority.ALWAYS); // make drop zone resize with the window
        VBox.setVgrow(center, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(content);

        var scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    private void showAboutDialog(Stage owner) {
        String javaVersion = SystemInfo.javaVersion();
        String javafxVersion = SystemInfo.javafxVersion();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("About This Application");
        alert.setContentText("Java version: " + javaVersion + "\nJavaFX version: " + javafxVersion);
        alert.getButtonTypes().setAll(ButtonType.CLOSE);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch();
    }

}