package org.openjfx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openjfx.model.CertificateInfo;
import org.openjfx.service.CertificateService;
import org.openjfx.service.ExportService;
import org.openjfx.service.KeystoreService;
import org.openjfx.service.ServiceExceptions.CertificateLoadException;
import org.openjfx.service.ServiceExceptions.ExportException;
import org.openjfx.service.ServiceExceptions.KeystoreLoadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Taskbar;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * JavaFX App
 */
public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private ProgressIndicator progressIndicator;
    private Label statusLabel;

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
                    if (Taskbar.isTaskbarSupported()) {
                        Taskbar taskbar = Taskbar.getTaskbar();
                        try (var is = iconUrl.openStream()) {
                            Image awtImage = ImageIO.read(is);
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
        MenuItem convertItem = new MenuItem("Convert to PKCS12");
        convertItem.setDisable(true);
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(exportItem, convertItem);

        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog(stage));
        Menu helpMenu = new Menu("Help");
        helpMenu.getItems().add(aboutItem);
        MenuBar menuBar = new MenuBar(fileMenu, helpMenu);

        // Drag-and-drop zone just below the menu
        Label dropText = new Label("Drop a JKS or PKCS12 (.p12), or CERT/CRT/PEM/DER file here");
        StackPane dropZone = new StackPane(dropText);
        dropZone.setStyle("-fx-border-color: #888; -fx-border-width: 2; -fx-border-style: dashed; -fx-background-color: #f5f5f5;");
        dropZone.setMinHeight(50);

        // TableView setup
        TableView<TableRowData> tableView = new TableView<>(tableData);
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

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

        // Set preferred widths so the table can overflow horizontally and show a scrollbar when needed
        aliasCol.setPrefWidth(200);
        entryTypeCol.setPrefWidth(140);
        validFromCol.setPrefWidth(170);
        validUntilCol.setPrefWidth(170);
        sigAlgCol.setPrefWidth(240);
        serialCol.setPrefWidth(240);

        tableView.getColumns().addAll(aliasCol, entryTypeCol, validFromCol, validUntilCol, sigAlgCol, serialCol);

        // Context menu on rows for export
        tableView.setRowFactory(tv -> {
            TableRow<TableRowData> row = new TableRow<>();
            MenuItem exportCtx = new MenuItem("Export Certificate…");
            exportCtx.setOnAction(e -> exportItem.fire());
            ContextMenu ctx = new ContextMenu(exportCtx);
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null)
                    .otherwise(ctx));
            // Double-click to show details
            row.setOnMouseClicked(me -> {
                if (me.getClickCount() == 2 && !row.isEmpty()) {
                    TableRowData data = row.getItem();
                    showCertificateDetails(stage, data);
                }
            });
            return row;
        });

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
                    String alias = row.aliasProperty().get();
                    try {
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

                        Task<Void> task = new Task<>() {
                            @Override
                            protected Void call() throws Exception {
                                String name = out.getName().toLowerCase(Locale.ROOT);
                                if (name.endsWith(".der")) {
                                    exportService.exportCertificateDer(cert, out.toPath());
                                } else {
                                    exportService.exportCertificatePem(cert, out.toPath());
                                }
                                return null;
                            }
                        };
                        task.setOnFailed(ev -> Platform.runLater(() -> showException(stage, "Failed to export certificate", task.getException())));
                        showProgressWhile(task);
                        new Thread(task, "export-cert").start();
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
                        chooser.setTitle("Save PKCS12 Keystore");
                        chooser.getExtensionFilters().addAll(
                                new FileChooser.ExtensionFilter("PKCS12 (*.p12)", "*.p12")
                        );
                        chooser.setInitialFileName("keystore.p12");
                        File out = chooser.showSaveDialog(stage);
                        if (out == null) return;

                        char[] ksPwd = (currentKeystorePassword != null) ? currentKeystorePassword : new char[0];
                        char[] keyPwd = (currentKeyPassword != null && currentKeyPassword.length > 0) ? currentKeyPassword : ksPwd;

                        Task<Void> task = new Task<>() {
                            @Override
                            protected Void call() throws Exception {
                                KeyStore p12 = keystoreService.convertToPkcs12(currentKeyStore, ksPwd, keyPwd);
                                try (FileOutputStream fos = new FileOutputStream(out)) {
                                    p12.store(fos, ksPwd);
                                }
                                return null;
                            }
                        };
                        task.setOnFailed(ev -> Platform.runLater(() -> showException(stage, "Failed to convert to PKCS12", task.getException())));
                        showProgressWhile(task);
                        new Thread(task, "convert-keystore").start();
                    } catch (Exception ex) {
                        showError(stage, "Failed to convert to PKCS12: " + ex.getMessage());
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
                    } else if (lower.endsWith(".cert") || lower.endsWith(".crt") || lower.endsWith(".pem")  || lower.endsWith(".der") || lower.endsWith(".p7b") || lower.endsWith(".p7c") || lower.endsWith(".spc")) {
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
                    showError(stage, "Please drop a .jks, .p12, .cert, .crt, .der, .pem, .p7b, .p7c or .spc file.");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Layout: Menu at top, then drop zone, then table with progress indicator overlay
        VBox content = new VBox(10);
        content.getChildren().addAll(dropZone, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(90, 90);
        progressIndicator.setVisible(false);

        StackPane centerPane = new StackPane(content, progressIndicator);

        // Status bar at bottom
        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-padding: 4 8 4 8; -fx-font-size: 11px; -fx-text-fill: #555;");

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(centerPane);
        root.setBottom(statusLabel);

        var scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.show();

        // Helper to update status text
        BiConsumer<String,String> setStatus = (fileName, ksType) -> {
            if (fileName == null && ksType == null) {
                statusLabel.setText("Ready");
            } else if (ksType == null) {
                statusLabel.setText("File: " + fileName);
            } else {
                statusLabel.setText("File: " + fileName + " • Type: " + ksType);
            }
        };

        // If a file path was provided as a command-line argument, try to load it
        try {
            List<String> args = getParameters().getRaw();
            if (args != null && !args.isEmpty()) {
                File cliFile = new File(args.get(0));
                if (cliFile.exists() && cliFile.isFile()) {
                    String lower = cliFile.getName().toLowerCase(Locale.ROOT);
                    if (lower.endsWith(".jks") || lower.endsWith(".pks") || lower.endsWith(".p12")) {
                        loadKeystoreIntoTable(cliFile, stage);
                    } else if (lower.endsWith(".cert") || lower.endsWith(".crt") || lower.endsWith(".pem") || lower.endsWith(".der") || lower.endsWith(".p7b") || lower.endsWith(".p7c") || lower.endsWith(".spc")) {
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

    private final KeystoreService keystoreService = new KeystoreService();
    private final CertificateService certificateService = new CertificateService();
    private final ExportService exportService = new ExportService();

    private void loadKeystoreIntoTable(File ksFile, Stage owner) {
        // Run background task for IO to keep UI responsive
        tableData.clear();
        // Ask for keystore and key password immediately when a file is dropped
        Optional<Passwords> pwOpt = promptForKeystoreAndKeyPasswords(owner);
        if (pwOpt.isEmpty()) {
            return; // user cancelled
        }
        Passwords pw = pwOpt.get();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                KeyStore ks = keystoreService.load(ksFile, pw.keystorePassword);
                List<CertificateInfo> infos = keystoreService.listEntries(ks);
                Platform.runLater(() -> {
                    tableData.clear();
                    for (CertificateInfo ci : infos) {
                        tableData.add(new TableRowData(ci.getAlias(), ci.getEntryType(), ci.getValidFrom(), ci.getValidUntil(), ci.getSignatureAlgorithm(), ci.getSerialNumber()));
                    }
                    currentKeyStore = ks;
                    keystoreLoaded = true;
                    String name = ksFile.getName().toLowerCase(Locale.ROOT);
                    currentKeystoreType = (name.endsWith(".pks") || name.endsWith(".p12")) ? "PKCS12" : "JKS";
                    currentKeystorePassword = pw.keystorePassword == null ? null : pw.keystorePassword.clone();
                    currentKeyPassword = pw.keyPassword == null ? null : pw.keyPassword.clone();
                });
                return null;
            }
        };
        task.setOnFailed(ev -> Platform.runLater(() -> showException(owner, "Failed to load keystore", task.getException())));
        task.setOnSucceeded(ev -> Platform.runLater(() -> {
            if (ksFile != null) {
                statusLabel.setText("File: " + ksFile.getName() + " • Type: " + currentKeystoreType);
            }
        }));
        showProgressWhile(task);
        new Thread(task, "load-keystore").start();
        // clear entered passwords promptly
        if (pw.keystorePassword != null) Arrays.fill(pw.keystorePassword, '\0');
        if (pw.keyPassword != null) Arrays.fill(pw.keyPassword, '\0');
    }

    private void showProgressWhile(Task<?> task) {
        Platform.runLater(() -> progressIndicator.setVisible(true));
        task.setOnSucceeded(e -> Platform.runLater(() -> progressIndicator.setVisible(false)));
        task.setOnFailed(e -> Platform.runLater(() -> progressIndicator.setVisible(false)));
        task.setOnCancelled(e -> Platform.runLater(() -> progressIndicator.setVisible(false)));
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
        Task<List<CertificateInfo>> task = new Task<>() {
            @Override
            protected List<CertificateInfo> call() throws Exception {
                return certificateService.loadCertificates(certFile);
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            tableData.clear();
            for (CertificateInfo ci : task.getValue()) {
                tableData.add(new TableRowData(ci.getAlias(), ci.getEntryType(), ci.getValidFrom(), ci.getValidUntil(), ci.getSignatureAlgorithm(), ci.getSerialNumber()));
            }
            if (certFile != null) {
                statusLabel.setText("File: " + certFile.getName() + " • Type: Certificates");
            }
        }));
        task.setOnFailed(e -> Platform.runLater(() -> showException(owner, "Failed to load certificate", task.getException())));
        showProgressWhile(task);
        new Thread(task, "load-certificates").start();
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

    private void showCertificateDetails(Stage owner, TableRowData data) {
        String alias = data.aliasProperty().get();
        String entryType = data.entryTypeProperty().get();
        String validFrom = data.validFromProperty().get();
        String validUntil = data.validUntilProperty().get();
        String sigAlg = data.signatureAlgorithmProperty().get();
        String serial = data.serialNumberProperty().get();

        X509Certificate x509 = null;
        try {
            if (keystoreLoaded && currentKeyStore != null) {
                Certificate c = currentKeyStore.getCertificate(alias);
                if (c instanceof X509Certificate xc) {
                    x509 = xc;
                }
            }
        } catch (Exception ignored) { }

        String subject = x509 != null && x509.getSubjectX500Principal() != null ? x509.getSubjectX500Principal().getName() : "";
        String issuer = x509 != null && x509.getIssuerX500Principal() != null ? x509.getIssuerX500Principal().getName() : "";

        // Compute SANs, key usages, fingerprints when possible
        String sans = "";
        String keyUsage = "";
        String extKeyUsage = "";
        String basicConstraints = "";
        String sha1 = "";
        String sha256 = "";
        String md5 = "";
        if (x509 != null) {
            try {
                Collection<List<?>> altNames = x509.getSubjectAlternativeNames();
                if (altNames != null) {
                    List<String> parts = new ArrayList<>();
                    for (List<?> item : altNames) {
                        if (item.size() >= 2) {
                            Object type = item.get(0);
                            Object value = item.get(1);
                            parts.add(String.valueOf(value));
                        }
                    }
                    sans = String.join(", ", parts);
                }
            } catch (Exception ignored) { }
            boolean[] ku = x509.getKeyUsage();
            if (ku != null) {
                String[] names = new String[]{"digitalSignature","nonRepudiation","keyEncipherment","dataEncipherment","keyAgreement","keyCertSign","cRLSign","encipherOnly","decipherOnly"};
                List<String> set = new ArrayList<>();
                for (int i=0;i<ku.length && i<names.length;i++) if (ku[i]) set.add(names[i]);
                keyUsage = String.join(", ", set);
            }
            try {
                List<String> eku = x509.getExtendedKeyUsage();
                if (eku != null) {
                    extKeyUsage = String.join(", ", eku);
                }
            } catch (Exception ignored) { }
            int bc = x509.getBasicConstraints();
            if (bc >= 0) basicConstraints = "CA: true, pathLen=" + bc; else basicConstraints = "CA: false";
            try {
                java.security.MessageDigest md1 = java.security.MessageDigest.getInstance("SHA-1");
                java.security.MessageDigest md256 = java.security.MessageDigest.getInstance("SHA-256");
                java.security.MessageDigest md5d = java.security.MessageDigest.getInstance("MD5");
                byte[] enc = x509.getEncoded();
                sha1 = toColonHex(md1.digest(enc));
                sha256 = toColonHex(md256.digest(enc));
                md5 = toColonHex(md5d.digest(enc));
            } catch (Exception ignored) { }
        }

        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.setTitle("Certificate Details");
        dlg.setHeaderText(alias);
        if (owner != null) dlg.initOwner(owner);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        int r = 0;
        grid.add(new Label("Entry Type:"), 0, r); grid.add(new Label(entryType), 1, r++);
        if (!subject.isEmpty()) { grid.add(new Label("Subject:"), 0, r); grid.add(new Label(subject), 1, r++); }
        if (!issuer.isEmpty()) { grid.add(new Label("Issuer:"), 0, r); grid.add(new Label(issuer), 1, r++); }
        grid.add(new Label("Valid From:"), 0, r); grid.add(new Label(validFrom), 1, r++);
        grid.add(new Label("Valid Until:"), 0, r); grid.add(new Label(validUntil), 1, r++);
        grid.add(new Label("Signature Alg:"), 0, r); grid.add(new Label(sigAlg), 1, r++);
        grid.add(new Label("Serial #:"), 0, r); grid.add(new Label(serial), 1, r++);
        if (!sans.isEmpty()) { grid.add(new Label("SANs:"), 0, r); grid.add(new Label(sans), 1, r++); }
        if (!keyUsage.isEmpty()) { grid.add(new Label("Key Usage:"), 0, r); grid.add(new Label(keyUsage), 1, r++); }
        if (!extKeyUsage.isEmpty()) { grid.add(new Label("Ext Key Usage:"), 0, r); grid.add(new Label(extKeyUsage), 1, r++); }
        if (!basicConstraints.isEmpty()) { grid.add(new Label("Basic Constraints:"), 0, r); grid.add(new Label(basicConstraints), 1, r++); }
        if (!md5.isEmpty()) { final String v = md5; grid.add(new Label("MD5:"), 0, r); grid.add(new Label(v), 1, r); Button b = new Button("Copy"); b.setOnAction(ev -> copyToClipboard(v)); grid.add(b, 2, r++); }
        if (!sha1.isEmpty()) { final String v = sha1; grid.add(new Label("SHA-1:"), 0, r); grid.add(new Label(v), 1, r); Button b = new Button("Copy"); b.setOnAction(ev -> copyToClipboard(v)); grid.add(b, 2, r++); }
        if (!sha256.isEmpty()) { final String v = sha256; grid.add(new Label("SHA-256:"), 0, r); grid.add(new Label(v), 1, r); Button b = new Button("Copy"); b.setOnAction(ev -> copyToClipboard(v)); grid.add(b, 2, r++); }
        dlg.getDialogPane().setContent(grid);
        dlg.getButtonTypes().setAll(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length*2);
        for (byte value : b) sb.append(String.format(java.util.Locale.ROOT, "%02X", value));
        return sb.toString();
    }

    // Format bytes like keytool: colon-separated uppercase hex pairs
    private static String toColonHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length*3 - 1);
        for (int i = 0; i < b.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format(java.util.Locale.ROOT, "%02X", b[i]));
        }
        return sb.toString();
    }

    private static void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
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

    private void showException(Stage owner, String context, Throwable t) {
        // Log full stack at debug for diagnostics; concise message to user
        if (t != null) {
            log.debug("{} failed", context, t);
        }
        String userMsg = context;
        Throwable root = t;
        while (root != null && root.getCause() != null) root = root.getCause();
        if (t instanceof KeystoreLoadException) {
            userMsg = context + ": Unable to open keystore. Check the password and file type.";
        } else if (t instanceof CertificateLoadException) {
            userMsg = context + ": Unable to parse certificate file. It may be unsupported or corrupted.";
        } else if (t instanceof ExportException) {
            userMsg = context + ": Unable to write file. Verify destination and permissions.";
        } else if (root != null && root.getMessage() != null && root.getMessage().toLowerCase(java.util.Locale.ROOT).contains("password")) {
            userMsg = context + ": Incorrect password.";
        }
        Alert alert = new Alert(Alert.AlertType.ERROR, userMsg, ButtonType.CLOSE);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        if (owner != null) alert.initOwner(owner);
        alert.showAndWait();
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