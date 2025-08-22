package org.openjfx;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;

/**
 * JavaFX App
 */
public class App extends Application {

    private KeyStore currentKeyStore = null;
    private boolean keystoreLoaded = false;
    private String currentKeystoreType = null; // "JKS" or "PKCS12"
    private char[] currentKeystorePassword = null; // as entered when loading
    private char[] currentKeyPassword = null; // optional, may be empty

    private final ObservableList<TableRowData> tableData = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) {
        // Set window title and icons (including macOS Dock/task bar)
        try {
            var iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                // JavaFX window icon
                try {
                    javafx.scene.image.Image fxImage = new javafx.scene.image.Image(iconUrl.toExternalForm());
                    stage.getIcons().add(fxImage);
                } catch (Throwable ignored) { }

                // macOS Dock/task bar icon via AWT Taskbar (if supported)
                try {
                    if (java.awt.Taskbar.isTaskbarSupported()) {
                        java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                        try (var is = iconUrl.openStream()) {
                            java.awt.Image awtImage = javax.imageio.ImageIO.read(is);
                            if (awtImage != null) {
                                taskbar.setIconImage(awtImage);
                            }
                        }
                    }
                } catch (Throwable ignored) { }
            }
        } catch (Throwable ignored) { }

        // Menu bar with File -> Export, Convert to PKS and Help -> About
        MenuItem exportItem = new MenuItem("Export");
        exportItem.setDisable(true);
        MenuItem convertItem = new MenuItem("Convert to PKS");
        convertItem.setDisable(true);
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(exportItem, convertItem);

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog(stage));
        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(aboutItem);
        MenuBar menuBar = new MenuBar(fileMenu, helpMenu);

        // Drag-and-drop zone just below the menu
        Label dropText = new Label("Drop a JKS, PKS/P12, CERT/CRT/PEM/DER file here");
        StackPane dropZone = new StackPane(dropText);
        dropZone.setStyle("-fx-border-color: #888; -fx-border-width: 2; -fx-border-style: dashed; -fx-background-color: #f5f5f5;");
        dropZone.setMinHeight(50);

        // TableView setup
        TableView<TableRowData> tableView = new TableView<>(tableData);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<TableRowData, String> aliasCol = new TableColumn<>("Alias Name");
        aliasCol.setCellValueFactory(cell -> cell.getValue().aliasProperty());

        TableColumn<TableRowData, String> entryTypeCol = new TableColumn<>("Entry Type");
        entryTypeCol.setCellValueFactory(cell -> cell.getValue().entryTypeProperty());

        TableColumn<TableRowData, String> validFromCol = new TableColumn<>("Valid From");
        validFromCol.setCellValueFactory(cell -> cell.getValue().validFromProperty());

        TableColumn<TableRowData, String> validUntilCol = new TableColumn<>("Valid Until");
        validUntilCol.setCellValueFactory(cell -> cell.getValue().validUntilProperty());

        TableColumn<TableRowData, String> sigAlgCol = new TableColumn<>("Signature Algorithm");
        sigAlgCol.setCellValueFactory(cell -> cell.getValue().signatureAlgorithmProperty());

        TableColumn<TableRowData, String> serialCol = new TableColumn<>("Serial Number");
        serialCol.setCellValueFactory(cell -> cell.getValue().serialNumberProperty());

        tableView.getColumns().addAll(aliasCol, entryTypeCol, validFromCol, validUntilCol, sigAlgCol, serialCol);

                // Enable/disable menu items based on state
                Runnable updateMenuEnabled = () -> {
                    boolean oneSelected = tableView.getSelectionModel().getSelectedItems().size() == 1;
                    exportItem.setDisable(!(keystoreLoaded && oneSelected));
                    boolean canConvert = keystoreLoaded && "JKS".equals(currentKeystoreType);
                    convertItem.setDisable(!canConvert);
                };
                tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> updateMenuEnabled.run());

                // Export action: export selected entry's certificate to PEM or DER
                exportItem.setOnAction(e -> {
                    TableRowData row = tableView.getSelectionModel().getSelectedItem();
                    if (row == null || !keystoreLoaded || currentKeyStore == null) {
                        return;
                    }
                    try {
                        String alias = row.aliasProperty().get();
                        Certificate cert = currentKeyStore.getCertificate(alias);
                        if (cert == null) {
                            showError(stage, "No certificate found for alias: " + alias);
                            return;
                        }
                        FileChooser chooser = new FileChooser();
                        chooser.setTitle("Export Certificate");
                        chooser.getExtensionFilters().addAll(
                                new FileChooser.ExtensionFilter("PEM (*.pem)", "*.pem"),
                                new FileChooser.ExtensionFilter("DER (*.der)", "*.der")
                        );
                        chooser.setInitialFileName(alias + ".pem");
                        File out = chooser.showSaveDialog(stage);
                        if (out == null) return;
                        String name = out.getName().toLowerCase(Locale.ROOT);
                        if (name.endsWith(".der")) {
                            Files.write(out.toPath(), cert.getEncoded());
                        } else {
                            String pem = "-----BEGIN CERTIFICATE-----\n" +
                                    Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(cert.getEncoded()) +
                                    "\n-----END CERTIFICATE-----\n";
                            Files.writeString(out.toPath(), pem, StandardCharsets.US_ASCII);
                        }
                    } catch (Exception ex) {
                        showError(stage, "Failed to export: " + ex.getMessage());
                    }
                });

                // Convert to PKS action
                convertItem.setOnAction(e -> {
                    if (!keystoreLoaded || currentKeyStore == null || !"JKS".equals(currentKeystoreType)) {
                        return;
                    }
                    try {
                        // Prepare destination file
                        FileChooser chooser = new FileChooser();
                        chooser.setTitle("Save PKS Keystore");
                        chooser.getExtensionFilters().addAll(
                                new FileChooser.ExtensionFilter("PKS (*.pks)", "*.pks"),
                                new FileChooser.ExtensionFilter("PKCS12 (*.p12)", "*.p12")
                        );
                        chooser.setInitialFileName("keystore.pks");
                        File out = chooser.showSaveDialog(stage);
                        if (out == null) return;

                        // Create PKCS12 keystore
                        KeyStore p12 = KeyStore.getInstance("PKCS12");
                        p12.load(null, null);

                        char[] ksPwd = (currentKeystorePassword != null) ? currentKeystorePassword : new char[0];
                        char[] keyPwd = (currentKeyPassword != null && currentKeyPassword.length > 0) ? currentKeyPassword : ksPwd;

                        for (Enumeration<String> ealiases = currentKeyStore.aliases(); ealiases.hasMoreElements(); ) {
                            String alias = ealiases.nextElement();
                            if (currentKeyStore.isKeyEntry(alias)) {
                                Key key = currentKeyStore.getKey(alias, keyPwd);
                                Certificate[] chain = currentKeyStore.getCertificateChain(alias);
                                if (chain == null) {
                                    Certificate c = currentKeyStore.getCertificate(alias);
                                    if (c != null) {
                                        chain = new Certificate[]{c};
                                    }
                                }
                                if (chain == null || key == null) {
                                    throw new Exception("Missing key or certificate chain for alias: " + alias);
                                }
                                p12.setKeyEntry(alias, key, keyPwd, chain);
                            } else if (currentKeyStore.isCertificateEntry(alias)) {
                                Certificate cert = currentKeyStore.getCertificate(alias);
                                if (cert != null) {
                                    p12.setCertificateEntry(alias, cert);
                                }
                            }
                        }

                        try (FileOutputStream fos = new FileOutputStream(out)) {
                            p12.store(fos, ksPwd);
                        }
                    } catch (Exception ex) {
                        showError(stage, "Failed to convert to PKS: " + ex.getMessage());
                    }
                });

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
                File ksFile = db.getFiles().stream()
                        .filter(f -> {
                            String name = f.getName().toLowerCase(Locale.ROOT);
                            return name.endsWith(".jks") || name.endsWith(".pks") || name.endsWith(".p12") || name.endsWith(".cert") || name.endsWith(".crt") || name.endsWith(".pem")  || name.endsWith(".der");
                        })
                        .findFirst().orElse(null);
                if (ksFile != null) {
                    dropText.setText("Loading: " + ksFile.getName());
                    String lower = ksFile.getName().toLowerCase(Locale.ROOT);
                    if (lower.endsWith(".jks") || lower.endsWith(".pks") || lower.endsWith(".p12")) {
                        loadKeystoreIntoTable(ksFile, stage);
                    } else if (lower.endsWith(".cert") || lower.endsWith(".crt") || lower.endsWith(".pem")  || lower.endsWith(".der")) {
                        loadCertificatesIntoTable(ksFile, stage);
                    }
                    // refresh menu enables after load
                    // ensure UI reflects current state (selection-independent items like Convert)
                    // We are inside start() where updateMenuEnabled is in scope
                    try {
                        // Run later to ensure state flags are updated
                        updateMenuEnabled.run();
                    } catch (Exception ignore) {}
                    success = true;
                } else {
                    showError(stage, "Please drop a .jks, .pks, .p12, .cert, .crt, .der or .pem file.");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Layout: Menu at top, then drop zone, then table
        VBox content = new VBox(10);
        content.getChildren().addAll(dropZone, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(content);

        var scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();

        // If a file path was provided as a command-line argument, try to load it
        try {
            java.util.List<String> args = getParameters().getRaw();
            if (args != null && !args.isEmpty()) {
                File cliFile = new File(args.get(0));
                if (cliFile.exists() && cliFile.isFile()) {
                    String lower = cliFile.getName().toLowerCase(Locale.ROOT);
                    if (lower.endsWith(".jks") || lower.endsWith(".pks") || lower.endsWith(".p12")) {
                        loadKeystoreIntoTable(cliFile, stage);
                    } else if (lower.endsWith(".cert") || lower.endsWith(".crt") || lower.endsWith(".pem") || lower.endsWith(".der")) {
                        loadCertificatesIntoTable(cliFile, stage);
                    } else {
                        showError(stage, "Unsupported file type: " + cliFile.getName());
                    }
                } else {
                    showError(stage, "File not found: " + cliFile.getPath());
                }
            }
        } catch (Throwable ignored) { }
    }

    private void loadKeystoreIntoTable(File ksFile, Stage owner) {
        tableData.clear();
        // Ask for keystore and key password immediately when a file is dropped
        Optional<Passwords> pwOpt = promptForKeystoreAndKeyPasswords(owner);
        if (pwOpt.isEmpty()) {
            return; // user cancelled
        }
        Passwords pw = pwOpt.get();
        String name = ksFile.getName().toLowerCase(Locale.ROOT);
        String type = (name.endsWith(".pks") || name.endsWith(".p12")) ? "PKCS12" : "JKS";
        try (FileInputStream fis = new FileInputStream(ksFile)) {
            KeyStore ks = KeyStore.getInstance(type);
            char[] ksPwd = (pw.keystorePassword != null && pw.keystorePassword.length > 0) ? pw.keystorePassword : null;
            ks.load(fis, ksPwd);
            populateTableFromKeyStore(ks);
            this.currentKeyStore = ks;
            this.keystoreLoaded = true;
            this.currentKeystoreType = type;
            // store copies of passwords for later operations (e.g., convert)
            this.currentKeystorePassword = pw.keystorePassword == null ? null : pw.keystorePassword.clone();
            this.currentKeyPassword = pw.keyPassword == null ? null : pw.keyPassword.clone();
        } catch (Exception ex) {
            showError(owner, "Failed to load keystore: " + ex.getMessage());
        } finally {
            // Clear password arrays for security
            if (pw.keystorePassword != null) java.util.Arrays.fill(pw.keystorePassword, '\0');
            if (pw.keyPassword != null) java.util.Arrays.fill(pw.keyPassword, '\0');
        }
    }

    private void populateTableFromKeyStore(KeyStore ks) throws Exception {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            String entryType = ks.isKeyEntry(alias) ? "Private Key" : (ks.isCertificateEntry(alias) ? "Trusted Certificate" : "Unknown");

            Certificate cert = ks.getCertificate(alias);
            String validFrom = "";
            String validUntil = "";
            String sigAlg = "";
            String serial = "";
            if (cert instanceof X509Certificate x509) {
                Date notBefore = x509.getNotBefore();
                Date notAfter = x509.getNotAfter();
                validFrom = fmt.format(notBefore);
                validUntil = fmt.format(notAfter);
                sigAlg = x509.getSigAlgName();
                serial = x509.getSerialNumber() != null ? x509.getSerialNumber().toString(16).toUpperCase(Locale.ROOT) : "";
            }
            tableData.add(new TableRowData(alias, entryType, validFrom, validUntil, sigAlg, serial));
        }
    }

    private void loadCertificatesIntoTable(File certFile, Stage owner) {
        tableData.clear();
        this.currentKeyStore = null;
        this.keystoreLoaded = false;
        this.currentKeystoreType = null;
        this.currentKeystorePassword = null;
        this.currentKeyPassword = null;
        try (FileInputStream fis = new FileInputStream(certFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certs = cf.generateCertificates(fis);
            populateTableFromCertificates(certs, certFile.getName());
        } catch (Exception ex) {
            showError(owner, "Failed to load certificate: " + ex.getMessage());
        }
    }

    private void populateTableFromCertificates(Collection<? extends Certificate> certs, String fileName) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm z");
        int idx = 1;
        for (Certificate cert : certs) {
            if (cert instanceof X509Certificate x509) {
                String alias = x509.getSubjectX500Principal() != null ? x509.getSubjectX500Principal().getName() : (fileName + "#" + idx);
                String validFrom = fmt.format(x509.getNotBefore());
                String validUntil = fmt.format(x509.getNotAfter());
                String sigAlg = x509.getSigAlgName();
                String serial = x509.getSerialNumber() != null ? x509.getSerialNumber().toString(16).toUpperCase(Locale.ROOT) : "";
                tableData.add(new TableRowData(alias, "Certificate", validFrom, validUntil, sigAlg, serial));
                idx++;
            }
        }
    }

    private Optional<char[]> promptForPassword(Stage owner, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Keystore Password");
        dialog.setHeaderText(message);
        dialog.setContentText("Password:");
        if (owner != null) dialog.initOwner(owner);
        Optional<String> result = dialog.showAndWait();
        return result.map(String::toCharArray);
    }

    private Optional<Passwords> promptForKeystoreAndKeyPasswords(Stage owner) {
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

    private static class Passwords {
        char[] keystorePassword;
        char[] keyPassword;
    }

    private void showError(Stage owner, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.CLOSE);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
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

    // Simple data holder for the table
    public static class TableRowData {
        private final javafx.beans.property.SimpleStringProperty alias = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty entryType = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty validFrom = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty validUntil = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty signatureAlgorithm = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.SimpleStringProperty serialNumber = new javafx.beans.property.SimpleStringProperty();

        public TableRowData(String alias, String entryType, String validFrom, String validUntil, String signatureAlgorithm, String serialNumber) {
            this.alias.set(alias);
            this.entryType.set(entryType);
            this.validFrom.set(validFrom);
            this.validUntil.set(validUntil);
            this.signatureAlgorithm.set(signatureAlgorithm);
            this.serialNumber.set(serialNumber);
        }
        public javafx.beans.property.SimpleStringProperty aliasProperty() { return alias; }
        public javafx.beans.property.SimpleStringProperty entryTypeProperty() { return entryType; }
        public javafx.beans.property.SimpleStringProperty validFromProperty() { return validFrom; }
        public javafx.beans.property.SimpleStringProperty validUntilProperty() { return validUntil; }
        public javafx.beans.property.SimpleStringProperty signatureAlgorithmProperty() { return signatureAlgorithm; }
        public javafx.beans.property.SimpleStringProperty serialNumberProperty() { return serialNumber; }
    }
}