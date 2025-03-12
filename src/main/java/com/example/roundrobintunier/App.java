package com.example.roundrobintunier;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class App extends Application {

    private List<Spieler> spielerListe = new ArrayList<>();
    private Turnier aktuellesTurnier;

    // Score pro Spieler (für die Rangliste)
    private Map<Spieler, Integer> scoreboardMap = new HashMap<>();
    // Gewinner pro Match (optional, falls du die Funktion nutzen willst)
    private Map<Match, Team> matchWinnerMap = new HashMap<>();

    // UI-Felder
    private TextField nameField;
    private ComboBox<String> geschlechtComboBox;
    private TextField plaetzeField;
    private TextField rundenField;
    private TextField startzeitField;
    private TextField spieldauerField;
    private TextField pausenlaengeField;

    private VBox spielerListeVBox;
    private GridPane spielplanGrid;

    private VBox ranglistenVBox;
    private Tab tabRangliste;

    // Status-Label für Feedback
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Round Robin - Großzügiges Layout + Korrektes Punktesystem");

        // ---------------------------
        // Menüleiste
        // ---------------------------
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");

        Menu menuDatei = new Menu("Datei");
        MenuItem miExit = new MenuItem("Beenden");
        miExit.setOnAction(e -> primaryStage.close());
        menuDatei.getItems().add(miExit);

        Menu menuExport = new Menu("Export");
        MenuItem miExportExcel = new MenuItem("Export Excel");
        miExportExcel.setOnAction(e -> exportToExcel());
        MenuItem miExportCsv = new MenuItem("Export CSV");
        miExportCsv.setOnAction(e -> exportToCSV());
        menuExport.getItems().addAll(miExportExcel, miExportCsv);

        Menu menuExtras = new Menu("Extras");
        MenuItem miZusammenfassung = new MenuItem("Zusammenfassung");
        miZusammenfassung.setOnAction(e -> zeigeZusammenfassung());
        menuExtras.getItems().add(miZusammenfassung);

        Menu menuHilfe = new Menu("Hilfe");
        MenuItem miInfo = new MenuItem("Über dieses Programm");
        miInfo.setOnAction(e -> zeigeInfo());
        menuHilfe.getItems().add(miInfo);

        menuBar.getMenus().addAll(menuDatei, menuExport, menuExtras, menuHilfe);

        // ---------------------------
        // Linke Sidebar
        // ---------------------------
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(20));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(300); // etwas größer

        Label lblAdd = new Label("Neuen Spieler hinzufügen");
        lblAdd.getStyleClass().add("label-title");

        nameField = new TextField();
        nameField.setPromptText("Spielername");
        nameField.getStyleClass().add("text-field");

        geschlechtComboBox = new ComboBox<>();
        geschlechtComboBox.getItems().addAll("M", "F");
        geschlechtComboBox.setPromptText("Geschlecht");
        geschlechtComboBox.getStyleClass().add("combo-box");

        Button btnAdd = new Button("Hinzufügen");
        btnAdd.getStyleClass().add("button");
        btnAdd.setOnAction(e -> addSpieler());
        btnAdd.setTooltip(new Tooltip("Spieler zur Liste hinzufügen"));

        Label lblListe = new Label("Aktuelle Spieler");
        lblListe.getStyleClass().add("label-title");

        spielerListeVBox = new VBox(5);
        ScrollPane scrollSpieler = new ScrollPane(spielerListeVBox);
        scrollSpieler.setFitToWidth(true);
        scrollSpieler.setPrefHeight(400);
        scrollSpieler.setPrefWidth(280);
        scrollSpieler.setStyle("-fx-background: transparent;");

        // Dark Mode Toggle
        ToggleButton darkModeToggle = new ToggleButton("Dark Mode");
        darkModeToggle.getStyleClass().add("button");
        darkModeToggle.setSelected(false);
        darkModeToggle.setOnAction(e -> toggleDarkMode(darkModeToggle.isSelected()));
        darkModeToggle.setTooltip(new Tooltip("Umschalten zwischen Light und Dark Mode"));

        sidebar.getChildren().addAll(
                lblAdd, nameField, geschlechtComboBox, btnAdd,
                new Separator(), lblListe, scrollSpieler,
                new Separator(), darkModeToggle
        );

        // ---------------------------
        // Tabs: Parameter, Turnierplan, Rangliste
        // ---------------------------
        TabPane centerTabPane = new TabPane();
        centerTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        centerTabPane.getStyleClass().add("center-tab-pane");

        // Tab 1: Parameter
        Tab tabParam = new Tab("Parameter");
        VBox paramBox = new VBox(15);
        paramBox.setPadding(new Insets(20));

        Label lblParam = new Label("Turnierparameter");
        lblParam.getStyleClass().add("label-title");

        plaetzeField = new TextField();
        plaetzeField.setPromptText("Anzahl Plätze");
        plaetzeField.getStyleClass().add("text-field");

        rundenField = new TextField();
        rundenField.setPromptText("Anzahl Runden");
        rundenField.getStyleClass().add("text-field");

        startzeitField = new TextField();
        startzeitField.setPromptText("Startzeit (HH:MM)");
        startzeitField.setText("09:00");
        startzeitField.getStyleClass().add("text-field");

        spieldauerField = new TextField();
        spieldauerField.setPromptText("Spieldauer (Minuten)");
        spieldauerField.setText("35");
        spieldauerField.getStyleClass().add("text-field");

        pausenlaengeField = new TextField();
        pausenlaengeField.setPromptText("Pausenlänge (Minuten)");
        pausenlaengeField.setText("10");
        pausenlaengeField.getStyleClass().add("text-field");

        Button btnErstellen = new Button("Plan erstellen");
        btnErstellen.getStyleClass().add("button");
        btnErstellen.setOnAction(e -> turnierPlanErstellen());
        btnErstellen.setTooltip(new Tooltip("Turnierplan erstellen"));

        Button btnAnimationDemo = new Button("Animation-Demo");
        btnAnimationDemo.getStyleClass().add("button");
        btnAnimationDemo.setOnAction(e -> demoFadeEffect(btnAnimationDemo));
        btnAnimationDemo.setTooltip(new Tooltip("Demo der Animationen"));

        paramBox.getChildren().addAll(
                lblParam,
                plaetzeField,
                rundenField,
                startzeitField,
                spieldauerField,
                pausenlaengeField,
                btnErstellen,
                btnAnimationDemo
        );
        tabParam.setContent(paramBox);

        // Tab 2: Turnierplan
        Tab tabPlan = new Tab("Turnierplan");
        spielplanGrid = new GridPane();
        spielplanGrid.getStyleClass().add("grid-pane");
        spielplanGrid.setGridLinesVisible(true); // Debug: Zeige Gitternetz

        ScrollPane planScroll = new ScrollPane(spielplanGrid);
        // (NEU) Größeres Standardfenster
        planScroll.setFitToWidth(false);
        planScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        planScroll.setPrefHeight(600);
        planScroll.setPrefWidth(1200);
        planScroll.setStyle("-fx-background: transparent;");
        tabPlan.setContent(planScroll);

        // Tab 3: Rangliste
        tabRangliste = new Tab("Rangliste");
        VBox ranglistenContainer = new VBox(10);
        ranglistenContainer.setPadding(new Insets(20));

        Label ranglistenBanner = new Label("Rangliste");
        ranglistenBanner.getStyleClass().add("ranglisten-banner");

        ranglistenVBox = new VBox(5);
        ranglistenVBox.getStyleClass().add("ranglistenVBox");
        ScrollPane rangScroll = new ScrollPane(ranglistenVBox);
        rangScroll.setFitToWidth(true);
        rangScroll.setPrefHeight(350);
        rangScroll.setPrefWidth(400);
        rangScroll.setStyle("-fx-background: transparent;");

        ranglistenContainer.getChildren().addAll(ranglistenBanner, rangScroll);
        tabRangliste.setContent(ranglistenContainer);

        centerTabPane.getTabs().addAll(tabParam, tabPlan, tabRangliste);

        // ---------------------------
        // Status-Label unten rechts
        // ---------------------------
        statusLabel = new Label("Bereit");
        statusLabel.getStyleClass().add("status-label");
        statusLabel.setAlignment(Pos.CENTER_RIGHT);

        // ---------------------------
        // Hauptlayout
        // ---------------------------
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setLeft(sidebar);
        root.setCenter(centerTabPane);
        BorderPane.setMargin(centerTabPane, new Insets(10));

        Label madeByLabel = new Label("Made by Carlo Deutschmann");
        madeByLabel.getStyleClass().add("made-by-label");

        HBox bottomBox = new HBox(10, madeByLabel, statusLabel);
        bottomBox.setPadding(new Insets(5));
        bottomBox.setAlignment(Pos.BOTTOM_LEFT);
        bottomBox.setStyle("-fx-background-color: transparent;");

        root.setBottom(bottomBox);

        // Szene
        Scene scene = new Scene(root, 1300, 800); // größerer Start
        String lightCssPath = "/com/example/roundrobintunier/styles.css";
        URL lightCssUrl = getClass().getResource(lightCssPath);
        if (lightCssUrl != null) {
            scene.getStylesheets().add(lightCssUrl.toExternalForm());
        } else {
            System.err.println("Light Mode CSS nicht gefunden: " + lightCssPath);
        }

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    // SPIELER
    private void addSpieler() {
        String name = nameField.getText().trim();
        String geschlecht = geschlechtComboBox.getValue();

        boolean valid = true;
        if (name.isEmpty()) {
            if (!nameField.getStyleClass().contains("error")) {
                nameField.getStyleClass().add("error");
            }
            shakeNode(nameField);
            valid = false;
        } else {
            nameField.getStyleClass().removeAll("error");
        }

        if (geschlecht == null) {
            if (!geschlechtComboBox.getStyleClass().contains("error")) {
                geschlechtComboBox.getStyleClass().add("error");
            }
            shakeNode(geschlechtComboBox);
            valid = false;
        } else {
            geschlechtComboBox.getStyleClass().removeAll("error");
        }

        if (!valid) {
            showErrorAlert("Bitte Name und Geschlecht eingeben!");
            return;
        }

        // Duplicate-Check
        for (Spieler s : spielerListe) {
            if (s.getName().equalsIgnoreCase(name)) {
                showErrorAlert("Diesen Spieler gibt es bereits!");
                shakeNode(nameField);
                return;
            }
        }

        Spieler neu = new Spieler(name, geschlecht);
        spielerListe.add(neu);

        nameField.clear();
        geschlechtComboBox.setValue(null);

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
        HBox row = new HBox(5);
        row.getStyleClass().add("spieler-item");
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(sp.getName() + " (" + sp.getGeschlecht() + ")");
        lbl.getStyleClass().add("spieler-item-label");

        Button btnRemove = new Button("Entf.");
        btnRemove.getStyleClass().add("remove-button");
        btnRemove.setTooltip(new Tooltip("Spieler entfernen"));

        btnRemove.setOnAction(e -> {
            spielerListe.remove(sp);
            aktualisiereSpielerListe();
            updateStatus("Spieler entfernt: " + sp.getName(), "info-label");
        });

        row.getChildren().addAll(lbl, btnRemove);

        // Drag-and-Drop-Funktionalität
        row.setOnDragDetected(event -> {
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(sp.getName());
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
                String draggedName = db.getString();
                Spieler draggedSpieler = findSpielerByName(draggedName);
                if (draggedSpieler != null) {
                    spielerListe.remove(draggedSpieler);
                    int thisIndex = spielerListe.indexOf(sp);
                    spielerListe.add(thisIndex, draggedSpieler);
                    aktualisiereSpielerListe();
                    success = true;
                    updateStatus("Spieler verschoben: " + draggedName, "info-label");
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        // Animation
        row.setTranslateX(-50);
        row.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), row);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition translate = new TranslateTransition(Duration.millis(200), row);
        translate.setFromX(-50);
        translate.setToX(0);

        ParallelTransition parallel = new ParallelTransition(fadeIn, translate);
        parallel.play();

        return row;
    }

    private Spieler findSpielerByName(String name) {
        for (Spieler sp : spielerListe) {
            if (sp.getName().equals(name)) {
                return sp;
            }
        }
        return null;
    }

    // TURNIER
    private void turnierPlanErstellen() {
        int plaetze, runden;
        try {
            plaetze = Integer.parseInt(plaetzeField.getText());
            runden = Integer.parseInt(rundenField.getText());
        } catch (NumberFormatException e) {
            showErrorAlert("Bitte gültige Zahlen für Plätze und Runden eingeben!");
            updateStatus("Fehler: Ungültige Eingaben für Plätze oder Runden.", "error-label");
            return;
        }
        if (plaetze <= 0 || runden <= 0) {
            showErrorAlert("Plätze und Runden müssen > 0 sein!");
            updateStatus("Fehler: Plätze und Runden müssen größer als 0 sein.", "error-label");
            return;
        }
        if (spielerListe.size() < 4) {
            showErrorAlert("Mindestens 4 Spieler benötigt!");
            updateStatus("Fehler: Mindestens 4 Spieler sind erforderlich.", "error-label");
            return;
        }

        String startStr = startzeitField.getText().trim();
        LocalTime startTime;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            startTime = LocalTime.parse(startStr, fmt);
        } catch (DateTimeParseException ex) {
            showErrorAlert("Bitte Startzeit im Format HH:mm eingeben (z.B. 09:00)!");
            updateStatus("Fehler: Ungültiges Startzeit-Format.", "error-label");
            return;
        }

        int spieldauer, pausenlaenge;
        try {
            spieldauer = Integer.parseInt(spieldauerField.getText().trim());
            pausenlaenge = Integer.parseInt(pausenlaengeField.getText().trim());
        } catch (NumberFormatException ex) {
            showErrorAlert("Bitte gültige Zahlen für Spieldauer und Pausenlänge eingeben!");
            updateStatus("Fehler: Ungültige Eingaben für Spieldauer oder Pausenlänge.", "error-label");
            return;
        }
        if (spieldauer <= 0 || pausenlaenge < 0) {
            showErrorAlert("Spieldauer muss > 0 und Pausenlänge >= 0 sein!");
            updateStatus("Fehler: Spieldauer muss größer als 0 und Pausenlänge mindestens 0 sein.", "error-label");
            return;
        }

        // Reset
        for (Spieler s : spielerListe) {
            s.resetPausenAnzahl();
            s.resetSpielAnzahl();
            s.resetPartnerHistorie();
            s.resetGegnerHistorie();
        }

        // PausenManager
        PausenManager pm = new PausenManager(spielerListe, runden, plaetze);
        List<List<Spieler>> rundenPlan = pm.planeRunden();
        List<List<Spieler>> pausenProRunde = pm.getPausenProRunde();

        // Turnier + Solver
        Turnier turnier = new Turnier(spielerListe, plaetze, runden);
        TournamentSolver solver = new TournamentSolver();

        for (int rundeNummer = 1; rundeNummer <= rundenPlan.size(); rundeNummer++) {
            List<Spieler> spielerInRunde = rundenPlan.get(rundeNummer - 1);
            Runde optimierteRunde = solver.solveRunde(rundeNummer, spielerInRunde);
            if (optimierteRunde != null) {
                turnier.getRunden().add(optimierteRunde);
            }
        }

        this.aktuellesTurnier = turnier;

        // Neues Scoreboard
        scoreboardMap.clear();
        for (Spieler sp : spielerListe) {
            scoreboardMap.put(sp, 0);
        }
        matchWinnerMap.clear();

        // Anzeige
        anzeigeTurnierPlanImGrid(turnier, startTime, spieldauer, pausenlaenge, pausenProRunde);
        updateRanglisteUI();
        updateStatus("Turnierplan erstellt.", "success-label");
    }

    /**
     * Zeigt den Turnierplan zusammen mit den pausierenden Spielern.
     */
    private void anzeigeTurnierPlanImGrid(Turnier turnier,
                                          LocalTime startTime,
                                          int spieldauer,
                                          int pausenlaenge,
                                          List<List<Spieler>> pausenProRunde) {

        spielplanGrid.getChildren().clear();
        spielplanGrid.getColumnConstraints().clear();
        spielplanGrid.getRowConstraints().clear();

        int anzahlPlaetze = turnier.getAnzahlPlaetze();
        int rundenAnzahl = turnier.getRundenAnzahl();

        // Spalte 0 (Zeit) + N x Matchspalten + 1 x Pausenspalte
        for (int col = 0; col < anzahlPlaetze + 2; col++) {
            ColumnConstraints cc = new ColumnConstraints();
            if (col == 0) {
                // Zeitspalte
                cc.setMinWidth(80);
                cc.setPrefWidth(80);
            } else if (col <= anzahlPlaetze) {
                // Matchspalten (passen zu den ~320 px aus CSS)
                cc.setMinWidth(340);
                cc.setPrefWidth(340);
            } else {
                // Pausenspalte (passen zu den ~160 px aus CSS, mit Reserve)
                cc.setMinWidth(180);
                cc.setPrefWidth(180);
            }
            spielplanGrid.getColumnConstraints().add(cc);
        }

        // Kopfzeile
        Label timeHeader = new Label("Zeit");
        timeHeader.getStyleClass().add("label-time");
        timeHeader.setTooltip(new Tooltip("Startzeit jeder Runde"));
        spielplanGrid.add(timeHeader, 0, 0);

        // Platz i
        for (int p = 1; p <= anzahlPlaetze; p++) {
            Label platzLabel = new Label("Platz " + p);
            platzLabel.getStyleClass().add("label-platz");
            platzLabel.setTooltip(new Tooltip("Spiele auf Platz " + p));
            spielplanGrid.add(platzLabel, p, 0);
        }

        // Spalte für "Pause"
        Label pauseColHeader = new Label("Pausierende\nSpieler");
        pauseColHeader.getStyleClass().add("label-platz");
        pauseColHeader.setTooltip(new Tooltip("Spieler, die diese Runde nicht spielen"));
        spielplanGrid.add(pauseColHeader, anzahlPlaetze + 1, 0);

        int startZeitMin = startTime.toSecondOfDay() / 60;

        for (int rundeNummer = 1; rundeNummer <= rundenAnzahl; rundeNummer++) {

            int hh = startZeitMin / 60;
            int mm = startZeitMin % 60;
            String zeit = String.format("%02d:%02d", hh, mm);

            int zeilenIndex = (rundeNummer * 2) - 1;
            Label zeitLabel = new Label(zeit);
            zeitLabel.getStyleClass().add("label-time");
            zeitLabel.setTooltip(new Tooltip("Runde " + rundeNummer + " startet um " + zeit));
            spielplanGrid.add(zeitLabel, 0, zeilenIndex);

            Runde runde = turnier.getRunden().get(rundeNummer - 1);
            List<Match> spiele = runde.getSpiele();

            // Matches
            for (int platz = 1; platz <= anzahlPlaetze; platz++) {
                int matchIndex = platz - 1;
                if (matchIndex < spiele.size()) {
                    Match m = spiele.get(matchIndex);

                    VBox matchCard = new VBox(8);
                    matchCard.getStyleClass().add("match-card");

                    Label lblMatch = new Label(m.toString());
                    lblMatch.getStyleClass().add("match-card-text");
                    lblMatch.setWrapText(true);
                    lblMatch.setTooltip(new Tooltip(m.toString()));

                    // Score-Eingabefelder
                    HBox scoreBox = new HBox(5);
                    TextField tfTeam1 = new TextField();
                    tfTeam1.setPromptText("Team1");
                    TextField tfTeam2 = new TextField();
                    tfTeam2.setPromptText("Team2");

                    // Wenn schon Ergebnis existiert
                    MatchResult oldRes = m.getResult();
                    if (oldRes != null) {
                        tfTeam1.setText(String.valueOf(oldRes.getTeam1Score()));
                        tfTeam2.setText(String.valueOf(oldRes.getTeam2Score()));
                    }

                    // Listener -> updateMatchResult
                    tfTeam1.textProperty().addListener((obs, oldVal, newVal) -> {
                        updateMatchResult(m, tfTeam1, tfTeam2);
                    });
                    tfTeam2.textProperty().addListener((obs, oldVal, newVal) -> {
                        updateMatchResult(m, tfTeam1, tfTeam2);
                    });

                    scoreBox.getChildren().addAll(tfTeam1, new Label(":"), tfTeam2);
                    matchCard.getChildren().addAll(lblMatch, scoreBox);
                    spielplanGrid.add(matchCard, platz, zeilenIndex);

                } else {
                    Label lblLeer = new Label("—");
                    lblLeer.getStyleClass().add("match-empty");
                    spielplanGrid.add(lblLeer, platz, zeilenIndex);
                }
            }

            // Pausierende Spieler
            List<Spieler> pausierendeSpieler = pausenProRunde.get(rundeNummer - 1);
            StringBuilder sb = new StringBuilder("Pause:\n");
            for (Spieler sp : pausierendeSpieler) {
                sb.append(sp.getName()).append("\n");
            }
            Label lblPausiert = new Label(sb.toString());
            lblPausiert.getStyleClass().add("pause-players-label");
            lblPausiert.setWrapText(true);
            lblPausiert.setTooltip(new Tooltip(sb.toString()));

            spielplanGrid.add(lblPausiert, anzahlPlaetze + 1, zeilenIndex);

            if (pausenlaenge > 0) {
                Label pauseLabel = new Label("Pause (" + pausenlaenge + " Min)");
                pauseLabel.getStyleClass().add("pause-label");
                spielplanGrid.add(pauseLabel, 0, rundeNummer * 2, anzahlPlaetze + 2, 1);
                startZeitMin += (spieldauer + pausenlaenge);
            } else {
                startZeitMin += spieldauer;
            }
        }
    }

    /**
     * Neues Punktesystem: Wir berücksichtigen alte Ergebnisse, subtrahieren sie vom Scoreboard,
     * und addieren danach den neuen Score.
     */
    private void updateMatchResult(Match match, TextField tfTeam1, TextField tfTeam2) {
        // Neue Eingaben (nicht parsebar => 0)
        int newScore1 = parseScore(tfTeam1.getText());
        int newScore2 = parseScore(tfTeam2.getText());

        // Altes Ergebnis => abziehen
        MatchResult oldResult = match.getResult();
        if (oldResult != null) {
            // Team1
            scoreboardMap.put(
                    match.getTeam1().getSpieler1(),
                    scoreboardMap.getOrDefault(match.getTeam1().getSpieler1(), 0) - oldResult.getTeam1Score());
            scoreboardMap.put(
                    match.getTeam1().getSpieler2(),
                    scoreboardMap.getOrDefault(match.getTeam1().getSpieler2(), 0) - oldResult.getTeam1Score());
            // Team2
            scoreboardMap.put(
                    match.getTeam2().getSpieler1(),
                    scoreboardMap.getOrDefault(match.getTeam2().getSpieler1(), 0) - oldResult.getTeam2Score());
            scoreboardMap.put(
                    match.getTeam2().getSpieler2(),
                    scoreboardMap.getOrDefault(match.getTeam2().getSpieler2(), 0) - oldResult.getTeam2Score());
        }

        // Neues Ergebnis speichern
        MatchResult result = new MatchResult(newScore1, newScore2);
        match.setResult(result);

        // Neue Punkte addieren
        scoreboardMap.put(
                match.getTeam1().getSpieler1(),
                scoreboardMap.getOrDefault(match.getTeam1().getSpieler1(), 0) + newScore1);
        scoreboardMap.put(
                match.getTeam1().getSpieler2(),
                scoreboardMap.getOrDefault(match.getTeam1().getSpieler2(), 0) + newScore1);
        scoreboardMap.put(
                match.getTeam2().getSpieler1(),
                scoreboardMap.getOrDefault(match.getTeam2().getSpieler1(), 0) + newScore2);
        scoreboardMap.put(
                match.getTeam2().getSpieler2(),
                scoreboardMap.getOrDefault(match.getTeam2().getSpieler2(), 0) + newScore2);

        updateRanglisteUI();
        updateStatus("Match-Ergebnis aktualisiert: " + match.toString(), "info-label");
    }

    private int parseScore(String txt) {
        try {
            return Integer.parseInt(txt.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // RANGLISTE
    private void updateRanglisteUI() {
        if (ranglistenVBox == null) return;
        ranglistenVBox.getChildren().clear();

        List<Map.Entry<Spieler, Integer>> sorted = new ArrayList<>(scoreboardMap.entrySet());
        // Absteigend nach Punkten
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int rank = 1;
        for (Map.Entry<Spieler, Integer> e : sorted) {
            Spieler sp = e.getKey();
            int pts = e.getValue();

            HBox row = new HBox(5);
            row.getStyleClass().add("ranglisten-item");

            // NEU: Klassen für Gold/Silber/Bronze (1,2,3)
            if (rank == 1) {
                row.getStyleClass().add("ranglisten-item-first");
            } else if (rank == 2) {
                row.getStyleClass().add("ranglisten-item-second");
            } else if (rank == 3) {
                row.getStyleClass().add("ranglisten-item-third");
            }

            row.setOpacity(0);

            Label lblRank = new Label(rank + ".");
            lblRank.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label lblName = new Label(sp.getName());
            lblName.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14; -fx-font-weight: bold;");

            Label lblPoints = new Label("(" + pts + " Punkte)");
            lblPoints.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12;");

            row.getChildren().addAll(lblRank, lblName, lblPoints);
            ranglistenVBox.getChildren().add(row);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), row);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), row);
            slideIn.setFromX(-50);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition parallel = new ParallelTransition(fadeIn, slideIn);
            parallel.play();

            rank++;
        }
    }

    // CSV / EXCEL - Export
    private void exportToCSV() {
        if (aktuellesTurnier == null) {
            showErrorAlert("Kein Turnier zum Export!");
            updateStatus("Fehler: Kein Turnier zum Exportieren.", "error-label");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CSV speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Datei", "*.csv"));
        File f = fileChooser.showSaveDialog(null);
        if (f == null) {
            updateStatus("Export abgebrochen.", "info-label");
            return;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            int anzahlPlaetze = aktuellesTurnier.getAnzahlPlaetze();
            int rundenAnzahl = aktuellesTurnier.getRundenAnzahl();

            StringBuilder sb = new StringBuilder("Zeit");
            for (int p = 1; p <= anzahlPlaetze; p++) {
                sb.append(";Platz ").append(p);
            }
            pw.println(sb);

            int startZeitMin = 540; // 09:00
            for (int r = 0; r < rundenAnzahl; r++) {
                Runde ru = aktuellesTurnier.getRunden().get(r);
                String zeit = String.format("%02d:%02d", startZeitMin / 60, startZeitMin % 60);
                startZeitMin += 35;

                List<Match> spiele = ru.getSpiele();
                StringBuilder sbRow = new StringBuilder(zeit);
                for (int p = 0; p < anzahlPlaetze; p++) {
                    sbRow.append(";");
                    if (p < spiele.size()) {
                        sbRow.append(spiele.get(p).toString());
                    } else {
                        sbRow.append("-");
                    }
                }
                pw.println(sbRow);
            }

            pw.flush();
            Alert ok = new Alert(AlertType.INFORMATION,
                    "CSV Export erfolgreich:\n" + f.getAbsolutePath(),
                    ButtonType.OK);
            ok.showAndWait();

            updateStatus("CSV Export erfolgreich: " + f.getAbsolutePath(), "success-label");
        } catch (IOException ex) {
            showErrorAlert("Fehler beim CSV Export: " + ex.getMessage());
            updateStatus("Fehler beim CSV Export.", "error-label");
        }
    }

    private void exportToExcel() {
        if (aktuellesTurnier == null) {
            showErrorAlert("Kein Turnier zum Export!");
            updateStatus("Fehler: Kein Turnier zum Exportieren.", "error-label");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excel (XLSX) speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File f = fileChooser.showSaveDialog(null);
        if (f == null) {
            updateStatus("Export abgebrochen.", "info-label");
            return;
        }

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Turnierplan");

            int anzahlPlaetze = aktuellesTurnier.getAnzahlPlaetze();
            int rundenAnzahl = aktuellesTurnier.getRundenAnzahl();

            Row headRow = sheet.createRow(0);
            headRow.createCell(0).setCellValue("Zeit");
            for (int p = 1; p <= anzahlPlaetze; p++) {
                headRow.createCell(p).setCellValue("Platz " + p);
            }

            int startZeitMin = 540; // 09:00
            for (int r = 0; r < rundenAnzahl; r++) {
                Runde ru = aktuellesTurnier.getRunden().get(r);
                Row row = sheet.createRow(r + 1);

                String zeit = String.format("%02d:%02d", startZeitMin / 60, startZeitMin % 60);
                startZeitMin += 35;
                row.createCell(0).setCellValue(zeit);

                List<Match> spiele = ru.getSpiele();
                for (int p = 0; p < anzahlPlaetze; p++) {
                    Cell c = row.createCell(p + 1);
                    if (p < spiele.size()) {
                        c.setCellValue(spiele.get(p).toString());
                    } else {
                        c.setCellValue("-");
                    }
                }
            }

            for (int col = 0; col <= anzahlPlaetze; col++) {
                sheet.autoSizeColumn(col);
            }

            try (FileOutputStream fos = new FileOutputStream(f)) {
                wb.write(fos);
            }

            Alert ok = new Alert(AlertType.INFORMATION,
                    "Excel Export erfolgreich:\n" + f.getAbsolutePath(),
                    ButtonType.OK);
            ok.showAndWait();

            updateStatus("Excel Export erfolgreich: " + f.getAbsolutePath(), "success-label");
        } catch (IOException ex) {
            showErrorAlert("Fehler beim Excel Export: " + ex.getMessage());
            updateStatus("Fehler beim Excel Export.", "error-label");
        }
    }

    // EXTRAS
    private void zeigeZusammenfassung() {
        if (aktuellesTurnier == null) {
            showErrorAlert("Es wurde noch kein Turnier erstellt!");
            updateStatus("Fehler: Kein Turnier erstellt.", "error-label");
            return;
        }
        int spielerCount = spielerListe.size();
        int rundenCount = aktuellesTurnier.getRundenAnzahl();
        int plaetzeCount = aktuellesTurnier.getAnzahlPlaetze();

        int matchesTotal = 0;
        for (Runde r : aktuellesTurnier.getRunden()) {
            matchesTotal += r.getSpiele().size();
        }

        Alert info = new Alert(AlertType.INFORMATION,
                "Spieleranzahl: " + spielerCount + "\n"
                        + "Plätze: " + plaetzeCount + "\n"
                        + "Runden: " + rundenCount + "\n"
                        + "Gesamt-Matches: " + matchesTotal,
                ButtonType.OK);
        info.setHeaderText("Turnier-Übersicht");
        info.setTitle("Zusammenfassung");

        URL cssUrl = getClass().getResource("/com/example/roundrobintunier/styles.css");
        if (cssUrl != null) {
            info.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            info.getDialogPane().getStyleClass().add("fancy-dialog-pane");
        }
        info.showAndWait();
        updateStatus("Zusammenfassung angezeigt.", "info-label");
    }

    private void zeigeInfo() {
        Alert info = new Alert(AlertType.INFORMATION,
                "Round Robin Planer – Großzügiges Layout, Korrektes Punktesystem.\n"
                        + "Keine überlappenden Punkte mehr, High-Level UI.\n"
                        + "Rangliste + Export.\n© 2025 Carlo Deutschmann",
                ButtonType.OK);
        info.setHeaderText("Info");
        info.setTitle("Hilfe");

        URL cssUrl = getClass().getResource("/com/example/roundrobintunier/styles.css");
        if (cssUrl != null) {
            info.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
            info.getDialogPane().getStyleClass().add("fancy-dialog-pane");
        }
        info.showAndWait();
    }

    // ANIMATIONEN
    private void demoFadeEffect(Button node) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(2);
        ft.setAutoReverse(true);
        ft.play();
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    // DARK MODE
    private void toggleDarkMode(boolean isDark) {
        Scene scene = nameField.getScene();
        if (scene == null) return;

        scene.getStylesheets().clear();
        if (isDark) {
            scene.getStylesheets().add(getClass().getResource("/com/example/roundrobintunier/styles_dark.css").toExternalForm());
            updateStatus("Dark Mode aktiviert.", "info-label");
        } else {
            scene.getStylesheets().add(getClass().getResource("/com/example/roundrobintunier/styles.css").toExternalForm());
            updateStatus("Light Mode aktiviert.", "info-label");
        }
    }

    // HILFSMETHODEN
    private void showErrorAlert(String msg) {
        Alert alert = new Alert(AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Fehler");
        alert.setTitle("Fehler");
        alert.showAndWait();
    }

    private void updateStatus(String message, String styleClass) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("error-label", "info-label", "success-label");
        statusLabel.getStyleClass().add(styleClass);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
