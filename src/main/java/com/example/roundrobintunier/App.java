package com.example.roundrobintunier;

import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashMap;

public class App extends Application {

    private Stage primaryStage;
    private boolean isDarkMode = false;
    private Scene scene;

    // Model
    private List<Spieler> spielerListe = new ArrayList<>();
    private Turnier aktuellesTurnier;
    private List<List<Spieler>> pausenProRunde;
    private Map<Spieler, Integer> scoreboardMap = new HashMap<>();

    // UI Components
    private TextField nameField, spielstaerkeField, plaetzeField, rundenField;
    private TextField startzeitField, spieldauerField, pausenlaengeField;
    private ComboBox<String> geschlechtComboBox;
    private CheckBox mixedCheckBox; // NEU
    private VBox spielerListeVBox;
    private GridPane spielplanGrid;
    private VBox ranglistenVBox;
    private Label statusLabel;
    private SVGPath iconRemove;


    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Round Robin Turnierplaner");

        // --- ICONS (SVG Paths for a modern look) ---
        iconRemove = createIcon("M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z");
        SVGPath iconAdd = createIcon("M8 2a.5.5 0 0 1 .5.5v5h5a.5.5 0 0 1 0 1h-5v5a.5.5 0 0 1-1 0v-5h-5a.5.5 0 0 1 0-1h5v-5A.5.5 0 0 1 8 2Z");
        SVGPath iconCreate = createIcon("M10.5 8a2.5 2.5 0 1 1-5 0 2.5 2.5 0 0 1 5 0z M8 1a7 7 0 1 0 0 14A7 7 0 0 0 8 1z M5 8a3 3 0 1 1 6 0 3 3 0 0 1-6 0z");
        SVGPath iconSave = createIcon("M.5 9.9a.5.5 0 0 1 .5.5v2.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-2.5a.5.5 0 0 1 1 0v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5a.5.5 0 0 1 .5-.5z M7.646 11.854a.5.5 0 0 0 .708 0l3-3a.5.5 0 0 0-.708-.708L8.5 10.293V1.5a.5.5 0 0 0-1 0v8.793L6.354 8.146a.5.5 0 0 0-.708.708l2 2z");
        SVGPath iconUpload = createIcon("M.5 9.9a.5.5 0 0 1 .5-.5h2.5a.5.5 0 0 1 0 1H1v1.5a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1V10.5h-2a.5.5 0 0 1 0-1H15a.5.5 0 0 1 .5.5v2.5a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2v-2.5zm6.354-7.854a.5.5 0 0 1 .708 0l3 3a.5.5 0 0 1-.708.708L8.5 3.707V11.5a.5.5 0 0 1-1 0V3.707L6.354 5.854a.5.5 0 1 1-.708-.708l2-2z");
        SVGPath iconPrint = createIcon("M5 1a2 2 0 0 0-2 2v2H2a2 2 0 0 0-2 2v3a2 2 0 0 0 2 2h1v1a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2v-1h1a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-1V3a2 2 0 0 0-2-2H5zM4 3a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2H4V3zm1 5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V8zm2 2h2a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1v-2a1 1 0 0 1 1-1z");
        SVGPath iconDarkMode = createIcon("M6 .278a.768.768 0 0 1 .08.858 7.208 7.208 0 0 0-.878 3.46c0 4.021 3.278 7.277 7.318 7.277.527 0 1.04-.055 1.533-.16a.787.787 0 0 1 .81.316.733.733 0 0 1-.031.893A8.349 8.349 0 0 1 8.344 16C3.734 16 0 12.286 0 7.71 0 4.266 2.114 1.312 5.124.06A.752.752 0 0 1 6 .278z");

        // --- HEADER ---
        Button btnUpload = createIconButton("Importieren", iconUpload);
        btnUpload.setOnAction(e -> importFromExcel());
        Button btnSave = createIconButton("Speichern", iconSave);
        btnSave.setOnAction(e -> exportToExcel());
        Button btnPrint = createIconButton("Drucken", iconPrint);
        btnPrint.setOnAction(e -> printPlan());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        ToggleButton darkModeToggle = new ToggleButton();
        darkModeToggle.setGraphic(iconDarkMode);
        darkModeToggle.getStyleClass().add("icon-button");
        darkModeToggle.setOnAction(e -> toggleDarkMode(darkModeToggle.isSelected()));
        HBox header = new HBox(10, btnUpload, btnSave, btnPrint, spacer, darkModeToggle);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // --- LEFT COLUMN: PLAYER MANAGEMENT ---
        Label lblAdd = new Label("Neuen Spieler hinzufügen");
        lblAdd.getStyleClass().add("h2");
        nameField = createStyledTextField("Spielername");
        geschlechtComboBox = new ComboBox<>();
        geschlechtComboBox.getItems().addAll("M", "F");
        geschlechtComboBox.setPromptText("Geschlecht");
        spielstaerkeField = createStyledTextField("Spielstärke (1-10)");
        Button btnAdd = createIconButton("Hinzufügen", iconAdd);
        btnAdd.setOnAction(e -> addSpieler());
        VBox addPlayerBox = new VBox(15, lblAdd, nameField, geschlechtComboBox, spielstaerkeField, btnAdd);
        addPlayerBox.getStyleClass().add("card");
        Label lblListe = new Label("Aktuelle Spieler");
        lblListe.getStyleClass().add("h2");
        spielerListeVBox = new VBox(5);
        ScrollPane scrollSpieler = new ScrollPane(spielerListeVBox);
        scrollSpieler.setFitToWidth(true);
        VBox leftColumn = new VBox(20, addPlayerBox, lblListe, scrollSpieler);
        leftColumn.setPadding(new Insets(10));

        // --- RIGHT COLUMN: TABS ---
        TabPane centerTabPane = new TabPane();
        centerTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Label lblParam = new Label("Turnierparameter");
        lblParam.getStyleClass().add("h2");
        plaetzeField = createStyledTextField("Anzahl Plätze");
        rundenField = createStyledTextField("Anzahl Runden");
        startzeitField = createStyledTextField("Startzeit (HH:MM)", "09:00");
        spieldauerField = createStyledTextField("Spieldauer (Minuten)", "35");
        pausenlaengeField = createStyledTextField("Pausenlänge (Minuten)", "10");

        // NEU: Checkbox für Mixed-Modus
        mixedCheckBox = new CheckBox("Mixed-Doppel erzwingen");

        Button btnErstellen = createIconButton("Turnierplan erstellen", iconCreate);
        btnErstellen.setOnAction(e -> turnierPlanErstellen());
        VBox paramBox = new VBox(15, lblParam, plaetzeField, rundenField, startzeitField, spieldauerField, pausenlaengeField, mixedCheckBox, btnErstellen);
        paramBox.setPadding(new Insets(20));
        Tab tabParam = new Tab("Einstellungen", paramBox);
        spielplanGrid = new GridPane();
        spielplanGrid.setHgap(10);
        spielplanGrid.setVgap(10);
        ScrollPane planScroll = new ScrollPane(spielplanGrid);
        planScroll.setFitToWidth(false);
        Tab tabPlan = new Tab("Turnierplan", planScroll);
        ranglistenVBox = new VBox(5);
        ScrollPane rangScroll = new ScrollPane(ranglistenVBox);
        rangScroll.setFitToWidth(true);
        ranglistenVBox.getStyleClass().add("card");
        Tab tabRangliste = new Tab("Rangliste", rangScroll);
        centerTabPane.getTabs().addAll(tabParam, tabPlan, tabRangliste);

        // --- MAIN LAYOUT ---
        GridPane mainGrid = new GridPane();
        mainGrid.setHgap(20);
        mainGrid.add(leftColumn, 0, 0);
        mainGrid.add(centerTabPane, 1, 0);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(35);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(65);
        mainGrid.getColumnConstraints().addAll(col1, col2);

        // --- STATUS BAR ---
        statusLabel = new Label("Bereit.");
        statusLabel.getStyleClass().add("status-label");
        HBox statusBar = new HBox(statusLabel);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setAlignment(Pos.CENTER_RIGHT);

        // --- ROOT PANE ---
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(mainGrid);
        root.setBottom(statusBar);

        // --- SCENE & STAGE SETUP ---
        this.scene = new Scene(root, 1400, 900);
        this.scene.getStylesheets().add(getClass().getResource("/com/example/roundrobintunier/modern-light.css").toExternalForm());

        primaryStage.setScene(this.scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    private void importFromExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excel Turnierplan importieren");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file == null) {
            updateStatus("Import abgebrochen.", "info-label");
            return;
        }
        try (Workbook wb = new XSSFWorkbook(new FileInputStream(file))) {
            Sheet paramSheet = wb.getSheet("Parameter");
            if (paramSheet == null) throw new IOException("Blatt 'Parameter' nicht gefunden.");
            plaetzeField.setText(paramSheet.getRow(0).getCell(1).getStringCellValue());
            rundenField.setText(paramSheet.getRow(1).getCell(1).getStringCellValue());
            startzeitField.setText(paramSheet.getRow(2).getCell(1).getStringCellValue());
            spieldauerField.setText(paramSheet.getRow(3).getCell(1).getStringCellValue());
            pausenlaengeField.setText(paramSheet.getRow(4).getCell(1).getStringCellValue());

            Sheet spielerSheet = wb.getSheet("Spieler");
            if (spielerSheet == null) throw new IOException("Blatt 'Spieler' nicht gefunden.");
            spielerListe.clear();
            for (int i = 1; i <= spielerSheet.getLastRowNum(); i++) {
                Row row = spielerSheet.getRow(i);
                if (row == null || row.getCell(0) == null) continue;
                String name = row.getCell(0).getStringCellValue();
                String geschlecht = row.getCell(1).getStringCellValue();
                int staerke = (int) row.getCell(2).getNumericCellValue();
                spielerListe.add(new Spieler(name, geschlecht, staerke));
            }
            aktualisiereSpielerListe();
            turnierPlanErstellen();
            updateStatus("Turnierplan erfolgreich aus '" + file.getName() + "' importiert.", "success-label");
        } catch (Exception e) {
            showErrorAlert("Fehler beim Importieren der Excel-Datei: \n" + e.getMessage());
            updateStatus("Import fehlgeschlagen.", "error-label");
        }
    }

    private SVGPath createIcon(String path) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("icon");
        return svg;
    }

    private Button createIconButton(String text, SVGPath icon) {
        Button btn = new Button(text, icon);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(8);
        return btn;
    }

    private TextField createStyledTextField(String prompt) {
        return createStyledTextField(prompt, "");
    }

    private TextField createStyledTextField(String prompt, String defaultValue) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        if (!defaultValue.isEmpty()) {
            tf.setText(defaultValue);
        }
        return tf;
    }

    private void toggleDarkMode(boolean enable) {
        isDarkMode = enable;
        scene.getStylesheets().clear();
        String css = isDarkMode ? "/com/example/roundrobintunier/modern-dark.css" : "/com/example/roundrobintunier/modern-light.css";
        scene.getStylesheets().add(getClass().getResource(css).toExternalForm());
        updateStatus(isDarkMode ? "Dark Mode aktiviert." : "Light Mode aktiviert.", "info-label");
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0);
        tt.setByX(8);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    private void addSpieler() {
        String name = nameField.getText().trim();
        String geschlecht = geschlechtComboBox.getValue();
        String spielstaerkeStr = spielstaerkeField.getText().trim();
        if (name.isEmpty() || geschlecht == null || spielstaerkeStr.isEmpty()) {
            showErrorAlert("Bitte alle Felder ausfüllen.");
            shakeNode(nameField);
            shakeNode(geschlechtComboBox);
            shakeNode(spielstaerkeField);
            return;
        }
        int spielstaerke;
        try {
            spielstaerke = Integer.parseInt(spielstaerkeStr);
            if (spielstaerke < 1 || spielstaerke > 10) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showErrorAlert("Spielstärke muss eine Zahl zwischen 1 und 10 sein.");
            shakeNode(spielstaerkeField);
            return;
        }
        if (spielerListe.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name))) {
            showErrorAlert("Ein Spieler mit diesem Namen existiert bereits.");
            shakeNode(nameField);
            return;
        }
        Spieler neu = new Spieler(name, geschlecht, spielstaerke);
        spielerListe.add(neu);
        nameField.clear();
        geschlechtComboBox.setValue(null);
        spielstaerkeField.clear();
        aktualisiereSpielerListe();
        updateStatus("Spieler hinzugefügt: " + name, "success-label");
    }

    private void aktualisiereSpielerListe() {
        spielerListeVBox.getChildren().clear();
        for (Spieler sp : spielerListe) {
            HBox row = createSpielerRow(sp);
            spielerListeVBox.getChildren().add(row);
        }
    }

    private HBox createSpielerRow(Spieler sp) {
        Label lbl = new Label(sp.getName() + " (" + sp.getGeschlecht() + ", Stärke: " + sp.getSpielstaerke() + ")");
        lbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        Button btnRemove = new Button();
        btnRemove.setGraphic(iconRemove);
        btnRemove.getStyleClass().add("remove-button");
        btnRemove.setOnAction(e -> {
            spielerListe.remove(sp);
            aktualisiereSpielerListe();
            updateStatus("Spieler entfernt: " + sp.getName(), "info-label");
        });
        HBox row = new HBox(10, lbl, btnRemove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("spieler-item");

        row.setOnDragDetected(event -> {
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(spielerListe.indexOf(sp)));
            db.setContent(content);
            event.consume();
        });
        row.setOnDragOver(event -> {
            if (event.getGestureSource() != row && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        row.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                int draggedIndex = Integer.parseInt(db.getString());
                Spieler draggedSpieler = spielerListe.get(draggedIndex);
                int thisIndex = spielerListe.indexOf(sp);
                spielerListe.remove(draggedIndex);
                if (draggedIndex < thisIndex) {
                    spielerListe.add(thisIndex - 1, draggedSpieler);
                } else {
                    spielerListe.add(thisIndex, draggedSpieler);
                }
                aktualisiereSpielerListe();
                success = true;
                updateStatus("Reihenfolge geändert.", "info-label");
            }
            event.setDropCompleted(success);
            event.consume();
        });
        return row;
    }

    private void turnierPlanErstellen() {
        int plaetze, runden, spieldauer, pausenlaenge;
        LocalTime startTime;
        boolean forceMixed = mixedCheckBox.isSelected();

        try {
            plaetze = Integer.parseInt(plaetzeField.getText());
            runden = Integer.parseInt(rundenField.getText());
            spieldauer = Integer.parseInt(spieldauerField.getText());
            pausenlaenge = Integer.parseInt(pausenlaengeField.getText());
            startTime = LocalTime.parse(startzeitField.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
            if (plaetze <= 0 || runden <= 0 || spieldauer <= 0 || pausenlaenge < 0) {
                throw new NumberFormatException("Werte müssen positiv sein.");
            }
        } catch (Exception e) {
            showErrorAlert("Bitte gültige Zahlen für alle Turnierparameter eingeben.\nStartzeit im Format HH:MM.");
            return;
        }
        if (spielerListe.size() < 4) {
            showErrorAlert("Mindestens 4 Spieler werden für ein Turnier benötigt.");
            return;
        }

        spielerListe.forEach(Spieler::resetStats);

        PausenManager pm = new PausenManager(spielerListe, runden, plaetze);
        List<List<Spieler>> rundenPlan = pm.planeRunden();
        this.pausenProRunde = pm.getPausenProRunde();

        Turnier turnier = new Turnier(spielerListe, plaetze, runden);
        TournamentSolver solver = new TournamentSolver();

        for (int i = 0; i < rundenPlan.size(); i++) {
            List<Spieler> spielerInRunde = rundenPlan.get(i);

            // NEU: Prüfung für Mixed-Modus
            if(forceMixed) {
                long maleCount = spielerInRunde.stream().filter(s -> "M".equals(s.getGeschlecht())).count();
                long femaleCount = spielerInRunde.stream().filter(s -> "F".equals(s.getGeschlecht())).count();
                if (maleCount != femaleCount) {
                    showErrorAlert("Fehler in Runde " + (i + 1) + ": Für den Mixed-Modus muss die Anzahl der männlichen (" + maleCount + ") und weiblichen (" + femaleCount + ") Spieler in der Runde gleich sein.");
                    return; // Breche die Erstellung ab
                }
            }

            Runde optimierteRunde = solver.solveRunde(i + 1, spielerInRunde, forceMixed);
            if (optimierteRunde != null) {
                turnier.getRunden().add(optimierteRunde);
            } else {
                // Optional: Abbruch, wenn eine Runde nicht gelöst werden kann
                showErrorAlert("Fehler: Für Runde " + (i+1) + " konnte keine gültige Paarung gefunden werden. Überprüfen Sie die Spieler und den Mixed-Modus.");
                return;
            }
        }
        this.aktuellesTurnier = turnier;

        scoreboardMap.clear();
        spielerListe.forEach(sp -> scoreboardMap.put(sp, 0));

        anzeigeTurnierPlanImGrid(startTime, spieldauer, pausenlaenge);
        updateRanglisteUI();
        updateStatus("Turnierplan erfolgreich erstellt.", "success-label");
    }

    private void anzeigeTurnierPlanImGrid(LocalTime startTime, int spieldauer, int pausenlaenge) {
        spielplanGrid.getChildren().clear();
        spielplanGrid.getColumnConstraints().clear();
        spielplanGrid.getRowConstraints().clear();

        int anzahlPlaetze = aktuellesTurnier.getAnzahlPlaetze();

        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setPrefWidth(70);
        spielplanGrid.getColumnConstraints().add(timeCol);

        for (int i = 0; i < anzahlPlaetze; i++) {
            ColumnConstraints platzCol = new ColumnConstraints();
            platzCol.setHgrow(Priority.ALWAYS);
            platzCol.setMinWidth(250);
            spielplanGrid.getColumnConstraints().add(platzCol);
        }

        ColumnConstraints pauseCol = new ColumnConstraints();
        pauseCol.setPrefWidth(150);
        spielplanGrid.getColumnConstraints().add(pauseCol);

        spielplanGrid.add(createHeaderLabel("Zeit"), 0, 0);
        for (int p = 1; p <= anzahlPlaetze; p++) {
            spielplanGrid.add(createHeaderLabel("Platz " + p), p, 0);
        }
        spielplanGrid.add(createHeaderLabel("Pausen"), anzahlPlaetze + 1, 0);

        LocalTime rundenZeit = startTime;
        for (int i = 0; i < aktuellesTurnier.getRundenAnzahl(); i++) {
            int rowIdx = i + 1;
            Label zeitLabel = new Label(rundenZeit.format(DateTimeFormatter.ofPattern("HH:mm")));
            zeitLabel.getStyleClass().add("time-label");
            spielplanGrid.add(zeitLabel, 0, rowIdx);

            Runde runde = aktuellesTurnier.getRunden().get(i);
            for (int p = 0; p < anzahlPlaetze; p++) {
                if (p < runde.getSpiele().size()) {
                    Match match = runde.getSpiele().get(p);
                    spielplanGrid.add(createMatchCard(match), p + 1, rowIdx);
                } else {
                    Label emptyLabel = new Label("—");
                    GridPane.setFillWidth(emptyLabel, true);
                    emptyLabel.setMaxWidth(Double.MAX_VALUE);
                    emptyLabel.setAlignment(Pos.CENTER);
                    spielplanGrid.add(emptyLabel, p + 1, rowIdx);
                }
            }

            List<Spieler> pausierer = pausenProRunde.get(i);
            VBox pausenBox = new VBox(2);
            pausenBox.getStyleClass().add("pausen-box");
            if (pausierer.isEmpty()) {
                pausenBox.getChildren().add(new Label("-"));
            } else {
                for (Spieler s : pausierer) {
                    pausenBox.getChildren().add(new Label(s.getName()));
                }
            }
            spielplanGrid.add(pausenBox, anzahlPlaetze + 1, rowIdx);

            rundenZeit = rundenZeit.plusMinutes((long) spieldauer + pausenlaenge);
        }
    }

    private VBox createMatchCard(Match match) {
        VBox team1Box = createTeamBox(match.getTeam1(), Pos.CENTER_LEFT);
        VBox team2Box = createTeamBox(match.getTeam2(), Pos.CENTER_RIGHT);
        Label vsLabel = new Label("vs");
        vsLabel.getStyleClass().add("vs-label");
        HBox teamsRow = new HBox(0, team1Box, vsLabel, team2Box);
        teamsRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(team1Box, Priority.ALWAYS);
        HBox.setHgrow(team2Box, Priority.ALWAYS);

        TextField tfTeam1 = new TextField("0");
        tfTeam1.setPrefWidth(50);
        TextField tfTeam2 = new TextField("0");
        tfTeam2.setPrefWidth(50);
        AtomicReference<MatchResult> oldResultRef = new AtomicReference<>(match.getResult());
        Runnable scoreUpdater = () -> {
            if (oldResultRef.get() != null) {
                updateScore(match.getTeam1(), -oldResultRef.get().getTeam1Score());
                updateScore(match.getTeam2(), -oldResultRef.get().getTeam2Score());
            }
            int newScore1 = parseScore(tfTeam1.getText());
            int newScore2 = parseScore(tfTeam2.getText());
            MatchResult newResult = new MatchResult(newScore1, newScore2);
            match.setResult(newResult);
            oldResultRef.set(newResult);
            updateScore(match.getTeam1(), newScore1);
            updateScore(match.getTeam2(), newScore2);
            updateRanglisteUI();
        };
        tfTeam1.textProperty().addListener((obs, ov, nv) -> scoreUpdater.run());
        tfTeam2.textProperty().addListener((obs, ov, nv) -> scoreUpdater.run());
        HBox scoreBox = new HBox(5, tfTeam1, new Label(":"), tfTeam2);
        scoreBox.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, teamsRow, scoreBox);
        card.getStyleClass().add("match-card");
        return card;
    }

    private VBox createTeamBox(Team team, Pos alignment) {
        Node p1 = createPlayerLabel(team.getSpieler1(), alignment);
        Node p2 = createPlayerLabel(team.getSpieler2(), alignment);
        VBox teamBox = new VBox(2, p1, p2);
        teamBox.setAlignment(alignment);
        teamBox.getStyleClass().add("team-box");
        return teamBox;
    }

    private VBox createPlayerLabel(Spieler spieler, Pos alignment) {
        Label nameLabel = new Label(spieler.getName());
        nameLabel.getStyleClass().add("player-name-label");
        nameLabel.setWrapText(true);

        Label skillLabel = new Label("Stärke: " + spieler.getSpielstaerke());
        skillLabel.getStyleClass().add("player-skill-label");

        VBox playerBox = new VBox(0, nameLabel, skillLabel);
        if (alignment == Pos.CENTER_RIGHT) {
            playerBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            playerBox.setAlignment(Pos.CENTER_LEFT);
        }
        return playerBox;
    }

    private void updateScore(Team team, int points) {
        scoreboardMap.merge(team.getSpieler1(), points, Integer::sum);
        scoreboardMap.merge(team.getSpieler2(), points, Integer::sum);
    }

    private int parseScore(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Label createHeaderLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("grid-header");
        return lbl;
    }

    private void updateRanglisteUI() {
        if (ranglistenVBox == null) return;
        ranglistenVBox.getChildren().clear();
        List<Map.Entry<Spieler, Integer>> sortedList = new ArrayList<>(scoreboardMap.entrySet());
        sortedList.sort(Map.Entry.<Spieler, Integer>comparingByValue().reversed());
        int rank = 1;
        for (Map.Entry<Spieler, Integer> entry : sortedList) {
            Label rankLabel = new Label(rank + ".");
            rankLabel.getStyleClass().add("rank-label");
            Label nameLabel = new Label(entry.getKey().getName());
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            Label pointsLabel = new Label(entry.getValue() + " Pkt.");
            pointsLabel.getStyleClass().add("points-label");
            HBox row = new HBox(10, rankLabel, nameLabel, pointsLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("ranglisten-item");
            if (rank == 1) row.getStyleClass().add("rank-first");
            else if (rank == 2) row.getStyleClass().add("rank-second");
            else if (rank == 3) row.getStyleClass().add("rank-third");
            ranglistenVBox.getChildren().add(row);
            rank++;
        }
    }

    private void exportToExcel() {
        if (aktuellesTurnier == null) {
            showErrorAlert("Es gibt keinen Turnierplan zum Exportieren.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excel Turnierplan speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File file = fileChooser.showSaveDialog(primaryStage);
        if (file == null) {
            updateStatus("Export abgebrochen.", "info-label");
            return;
        }
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet paramSheet = wb.createSheet("Parameter");
            paramSheet.createRow(0).createCell(0).setCellValue("Anzahl Plätze");
            paramSheet.getRow(0).createCell(1).setCellValue(plaetzeField.getText());
            paramSheet.createRow(1).createCell(0).setCellValue("Anzahl Runden");
            paramSheet.getRow(1).createCell(1).setCellValue(rundenField.getText());
            paramSheet.createRow(2).createCell(0).setCellValue("Startzeit");
            paramSheet.getRow(2).createCell(1).setCellValue(startzeitField.getText());
            paramSheet.createRow(3).createCell(0).setCellValue("Spieldauer");
            paramSheet.getRow(3).createCell(1).setCellValue(spieldauerField.getText());
            paramSheet.createRow(4).createCell(0).setCellValue("Pausenlänge");
            paramSheet.getRow(4).createCell(1).setCellValue(pausenlaengeField.getText());

            Sheet spielerSheet = wb.createSheet("Spieler");
            Row spielerHeader = spielerSheet.createRow(0);
            spielerHeader.createCell(0).setCellValue("Name");
            spielerHeader.createCell(1).setCellValue("Geschlecht");
            spielerHeader.createCell(2).setCellValue("Spielstärke");
            for (int i = 0; i < spielerListe.size(); i++) {
                Row row = spielerSheet.createRow(i + 1);
                Spieler s = spielerListe.get(i);
                row.createCell(0).setCellValue(s.getName());
                row.createCell(1).setCellValue(s.getGeschlecht());
                row.createCell(2).setCellValue(s.getSpielstaerke());
            }

            wb.createSheet("Turnierplan");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
            updateStatus("Turnier erfolgreich nach '" + file.getName() + "' exportiert.", "success-label");
        } catch (IOException e) {
            showErrorAlert("Fehler beim Exportieren der Excel-Datei: " + e.getMessage());
            updateStatus("Export fehlgeschlagen.", "error-label");
        }
    }

    private void printPlan() {
        if (spielplanGrid.getChildren().isEmpty()) {
            showErrorAlert("Kein Turnierplan zum Drucken vorhanden.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job != null && job.showPrintDialog(primaryStage)) {
            boolean success = job.printPage(spielplanGrid);
            if (success) {
                job.endJob();
                updateStatus("Druckauftrag gesendet.", "success-label");
            } else {
                updateStatus("Drucken fehlgeschlagen.", "error-label");
            }
        } else {
            updateStatus("Drucken abgebrochen.", "info-label");
        }
    }

    private void showErrorAlert(String msg) {
        Alert alert = new Alert(AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Fehler");
        alert.showAndWait();
    }

    private void updateStatus(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll("status-label", styleClass);
    }

    public static void main(String[] args) {
        launch(args);
    }
}