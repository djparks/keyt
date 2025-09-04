package org.openjfx.util;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.openjfx.SystemInfo;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() {}

    public static void showError(Stage owner, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.CLOSE);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
    }

    public static void showInfo(Stage owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.CLOSE);
        alert.setTitle(title);
        alert.setHeaderText(null);
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
    }

    public static Optional<char[]> promptForPassword(Stage owner, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Keystore Password");
        dialog.setHeaderText(message);
        dialog.setContentText("Password:");
        if (owner != null) dialog.initOwner(owner);
        Optional<String> result = dialog.showAndWait();
        return result.map(String::toCharArray);
    }

    public static Optional<Passwords> promptForKeystoreAndKeyPasswords(Stage owner) {
        Dialog<Passwords> dialog = new Dialog<>();
        dialog.setTitle("Keystore Credentials");
        dialog.setHeaderText("Enter keystore and key passwords");
        if (owner != null) dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        PasswordField ksField = new PasswordField();
        ksField.setPromptText("Keystore password (can be empty)");
        PasswordField keyField = new PasswordField();
        keyField.setPromptText("Key password (optional)");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Keystore password:"), 0, 0);
        grid.add(ksField, 1, 0);
        grid.add(new Label("Key password:"), 0, 1);
        grid.add(keyField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                Passwords p = new Passwords();
                String ksText = ksField.getText();
                String keyText = keyField.getText();
                p.keystorePassword = ksText == null ? new char[0] : ksText.toCharArray();
                p.keyPassword = keyText == null ? new char[0] : keyText.toCharArray();
                return p;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public static void showAboutDialog(Stage owner) {
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

    public static class Passwords {
        public char[] keystorePassword;
        public char[] keyPassword;
    }
}
