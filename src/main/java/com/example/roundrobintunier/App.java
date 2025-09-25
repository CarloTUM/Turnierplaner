package com.example.roundrobintunier;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;

import javafx.print.PrinterJob;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

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

    // neu: Referenz auf die Stage (für Dialog-Owner & Drucken)
    private Stage primary;

    private LocalTime startTime;
    private int spieldauer;
    private int pausenlaenge;
    private List<List<Spieler>> pausenProRunde;

    private List<Spieler> spielerListe = new ArrayList<>();
    private Turnier aktuellesTurnier;

    // Score pro Spieler (für die Rangliste)
    private Map<Spieler, Integer> scoreboardMap = new HashMap<>();
    // Gewinner pro Match (optional)
    private Map<Match, Team> matchWinnerMap = new HashMap<>();

    // UI-Felder
    private TextField nameField;
    private ComboBox<String> geschlechtComboBox;
    private TextField spielstaerkeField; // Eingabe der Spielstärke
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
        this.primary = primaryStage;
        primaryStage.setTitle("Round Robin - Großzügiges Layout + Korrektes Punktesystem");

        // ---------------------------
        // Menüleiste + Datei-Menü mit Shortcuts
        // ---------------------------
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("menu-bar");

        Menu menuDatei = new Menu("Datei");

        MenuItem miSpeichernExcel = new MenuItem("Speichern als Excel…");
        miSpeichernExcel.setOnAction(e -> exportToExcel());
        miSpeichernExcel.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)); // Ctrl/Cmd+Shift+S

        MenuItem miSpeichernCSV = new MenuItem("Speichern als CSV…");
        miSpeichernCSV.setOnAction(e -> exportToCSV());
        miSpeichernCSV.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd+S

        MenuItem miDrucken = new MenuItem("Drucken…");
        miDrucken.setOnAction(e -> printPlan());
        miDrucken.setAccelerator(new KeyCodeCombination(KeyCode.P, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd+P

        MenuItem miBeenden = new MenuItem("Beenden");
        miBeenden.setOnAction(e -> primary.close());
        miBeenden.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd+Q

        menuDatei.getItems().addAll(miSpeichernExcel, miSpeichernCSV, new SeparatorMenuItem(), miDrucken, new SeparatorMenuItem(), miBeenden);

        Menu menuExtras = new Menu("Extras");
        MenuItem miZusammenfassung = new MenuItem("Zusammenfassung");
        miZusammenfassung.setOnAction(e -> zeigeZusammenfassung());
        menuExtras.getItems().add(miZusammenfassung);

        Menu menuHilfe = new Menu("Hilfe");
        MenuItem miInfo = new MenuItem("Über dieses Programm");
        miInfo.setOnAction(e -> zeigeInfo());
        menuHilfe.getItems().add(miInfo);

        menuBar.getMenus().addAll(menuDatei, menuExtras, menuHilfe);

        // sichtbare Toolbar-Buttons
        ToolBar toolBar = new ToolBar();
        Button btnSaveCSV = new Button("CSV speichern");
        btnSaveCSV.setOnAction(e -> exportToCSV());
        Button btnSaveExcel = new Button("Excel speichern");
        btnSaveExcel.setOnAction(e -> exportToExcel());
        Button btnPrint = new Button("Drucken");
        btnPrint.setOnAction(e -> printPlan());
        Button btnExit = new Button("Beenden");
        btnExit.setOnAction(e -> primary.close());
        toolBar.getItems().addAll(btnSaveCSV, btnSaveExcel, btnPrint, new Separator(), btnExit);

        // ---------------------------
        // Linke Sidebar
        // ---------------------------
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(20));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(300);

        Label lblAdd = new Label("Neuen Spieler hinzufügen");
        lblAdd.getStyleClass().add("label-title");

        nameField = new TextField();
        nameField.setPromptText("Spielername");
        nameField.getStyleClass().add("text-field");

        geschlechtComboBox = new ComboBox<>();
        geschlechtComboBox.getItems().addAll("M", "F");
        geschlechtComboBox.setPromptText("Geschlecht");
        geschlechtComboBox.getStyleClass().add("combo-box");

        spielstaerkeField = new TextField();
        spielstaerkeField.setPromptText("Spielstärke (1-10)");
        spielstaerkeField.getStyleClass().add("text-field");

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
                lblAdd, nameField, geschlechtComboBox, spielstaerkeField, btnAdd,
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
        spielplanGrid.setGridLinesVisible(true); // Debug

        ScrollPane planScroll = new ScrollPane(spielplanGrid);
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
        // Menü + Toolbar oben stapeln
        VBox topBox = new VBox(menuBar, toolBar);
        root.setTop(topBox);

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

        Scene scene = new Scene(root, 1300, 800);
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
        String spielstaerkeStr = spielstaerkeField.getText().trim();

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

        int spielstaerke = 0;
        try {
            spielstaerke = Integer.parseInt(spielstaerkeStr);
            if (spielstaerke < 1 || spielstaerke > 10) {
                throw new NumberFormatException();
            }
            spielstaerkeField.getStyleClass().removeAll("error");
        } catch (NumberFormatException e) {
            if (!spielstaerkeField.getStyleClass().contains("error")) {
                spielstaerkeField.getStyleClass().add("error");
            }
            shakeNode(spielstaerkeField);
            valid = false;
        }

        if (!valid) {
            showErrorAlert("Bitte alle Felder korrekt ausfüllen!\nSpielstärke: Zahl zwischen 1 und 10");
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
        HBox row = new HBox(5);
        row.getStyleClass().add("spieler-item");
        row.setAlignment(Pos.CENTER_LEFT);

        // Anzeige: Name, Geschlecht und Spielstärke
        Label lbl = new Label(sp.getName() + " (" + sp.getGeschlecht() + ", Stärke: " + sp.getSpielstaerke() + ")");
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

        // Startzeit einlesen und Klassenfeld setzen
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            this.startTime = LocalTime.parse(startzeitField.getText().trim(), fmt);
        } catch (DateTimeParseException ex) {
            showErrorAlert("Bitte Startzeit im Format HH:mm eingeben (z.B. 09:00)!");
            updateStatus("Fehler: Ungültiges Startzeit-Format.", "error-label");
            return;
        }

        // Spieldauer und Pausenlänge einlesen und Klassenfelder setzen
        try {
            this.spieldauer = Integer.parseInt(spieldauerField.getText().trim());
            this.pausenlaenge = Integer.parseInt(pausenlaengeField.getText().trim());
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

        // Reset der Spielerattribute
        for (Spieler s : spielerListe) {
            s.resetPausenAnzahl();
            s.resetSpielAnzahl();
            s.resetPartnerHistorie();
            s.resetGegnerHistorie();
        }

        // PausenManager erstellen und rundenPlan sowie pausenProRunde setzen
        PausenManager pm = new PausenManager(spielerListe, runden, plaetze);
        List<List<Spieler>> rundenPlan = pm.planeRunden();
        this.pausenProRunde = pm.getPausenProRunde();

        // Turnier erstellen und Solver anwenden
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

        // Neues Scoreboard initialisieren
        scoreboardMap.clear();
        for (Spieler sp : spielerListe) {
            scoreboardMap.put(sp, 0);
        }
        matchWinnerMap.clear();

        // Anzeige: Turnierplan im Grid, Rangliste und Status aktualisieren
        anzeigeTurnierPlanImGrid(turnier, this.startTime, this.spieldauer, this.pausenlaenge, this.pausenProRunde);
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
                cc.setMinWidth(80);
                cc.setPrefWidth(80);
            } else if (col <= anzahlPlaetze) {
                cc.setMinWidth(340);
                cc.setPrefWidth(340);
            } else {
                cc.setMinWidth(180);
                cc.setPrefWidth(180);
            }
            spielplanGrid.getColumnConstraints().add(cc);
        }

        Label timeHeader = new Label("Zeit");
        timeHeader.getStyleClass().add("label-time");
        timeHeader.setTooltip(new Tooltip("Startzeit jeder Runde"));
        spielplanGrid.add(timeHeader, 0, 0);

        for (int p = 1; p <= anzahlPlaetze; p++) {
            Label platzLabel = new Label("Platz " + p);
            platzLabel.getStyleClass().add("label-platz");
            platzLabel.setTooltip(new Tooltip("Spiele auf Platz " + p));
            spielplanGrid.add(platzLabel, p, 0);
        }

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

                    HBox scoreBox = new HBox(5);
                    TextField tfTeam1 = new TextField();
                    tfTeam1.setPromptText("Team1");
                    TextField tfTeam2 = new TextField();
                    tfTeam2.setPromptText("Team2");

                    MatchResult oldRes = m.getResult();
                    if (oldRes != null) {
                        tfTeam1.setText(String.valueOf(oldRes.getTeam1Score()));
                        tfTeam2.setText(String.valueOf(oldRes.getTeam2Score()));
                    }

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

    private void updateMatchResult(Match match, TextField tfTeam1, TextField tfTeam2) {
        int newScore1 = parseScore(tfTeam1.getText());
        int newScore2 = parseScore(tfTeam2.getText());

        MatchResult oldResult = match.getResult();
        if (oldResult != null) {
            scoreboardMap.put(
                    match.getTeam1().getSpieler1(),
                    scoreboardMap.getOrDefault(match.getTeam1().getSpieler1(), 0) - oldResult.getTeam1Score());
            scoreboardMap.put(
                    match.getTeam1().getSpieler2(),
                    scoreboardMap.getOrDefault(match.getTeam1().getSpieler2(), 0) - oldResult.getTeam1Score());
            scoreboardMap.put(
                    match.getTeam2().getSpieler1(),
                    scoreboardMap.getOrDefault(match.getTeam2().getSpieler1(), 0) - oldResult.getTeam2Score());
            scoreboardMap.put(
                    match.getTeam2().getSpieler2(),
                    scoreboardMap.getOrDefault(match.getTeam2().getSpieler2(), 0) - oldResult.getTeam2Score());
        }

        MatchResult result = new MatchResult(newScore1, newScore2);
        match.setResult(result);

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
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int rank = 1;
        for (Map.Entry<Spieler, Integer> e : sorted) {
            Spieler sp = e.getKey();
            int pts = e.getValue();

            HBox row = new HBox(5);
            row.getStyleClass().add("ranglisten-item");

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

            // Anzeige der Spielstärke in der Rangliste
            Label lblStrength = new Label("(Stärke: " + sp.getSpielstaerke() + ")");
            lblStrength.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12;");

            Label lblPoints = new Label("(" + pts + " Punkte)");
            lblPoints.setStyle("-fx-text-fill: #34495e; -fx-font-size: 12;");

            row.getChildren().addAll(lblRank, lblName, lblStrength, lblPoints);
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

    private void exportToCSV() {
        if (aktuellesTurnier == null) {
            showErrorAlert("Kein Turnier zum Export!");
            updateStatus("Fehler: Kein Turnier zum Exportieren.", "error-label");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("CSV speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Datei", "*.csv"));
        File f = fileChooser.showSaveDialog(primary);
        if (f == null) {
            updateStatus("Export abgebrochen.", "info-label");
            return;
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            int anzahlPlaetze = aktuellesTurnier.getAnzahlPlaetze();
            int rundenAnzahl = aktuellesTurnier.getRundenAnzahl();

            // Header
            StringBuilder header = new StringBuilder("Zeit");
            for (int p = 1; p <= anzahlPlaetze; p++) {
                header.append(";Platz ").append(p);
            }
            header.append(";Pausierende Spieler");
            pw.println(header);

            int startZeitMin = startTime.toSecondOfDay() / 60;
            for (int r = 0; r < rundenAnzahl; r++) {
                Runde ru = aktuellesTurnier.getRunden().get(r);
                List<Match> spiele = ru.getSpiele();

                String zeit = String.format("%02d:%02d", startZeitMin / 60, startZeitMin % 60);
                StringBuilder row = new StringBuilder(zeit);
                for (int p = 0; p < anzahlPlaetze; p++) {
                    row.append(";");
                    row.append(p < spiele.size() ? spiele.get(p).toString() : "-");
                }

                // Pausen-Spalte
                List<Spieler> paused = pausenProRunde.get(r);
                if (paused.isEmpty()) {
                    row.append(";-");
                } else {
                    String pausedNames = String.join(", ", paused.stream().map(Spieler::getName).toList());
                    row.append(";").append(pausedNames);
                }

                pw.println(row);

                // Zeit erhöhen
                startZeitMin += (pausenlaenge > 0 ? spieldauer + pausenlaenge : spieldauer);
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
        fileChooser.setTitle("Excel Turnierplan speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File f = fileChooser.showSaveDialog(primary);
        if (f == null) {
            updateStatus("Export abgebrochen.", "info-label");
            return;
        }
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Turnierplan");

            // Header-Style
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Zeit-Style
            CellStyle timeStyle = wb.createCellStyle();
            timeStyle.setAlignment(HorizontalAlignment.CENTER);
            timeStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            timeStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            timeStyle.setBorderBottom(BorderStyle.THIN);
            timeStyle.setBorderTop(BorderStyle.THIN);
            timeStyle.setBorderLeft(BorderStyle.THIN);
            timeStyle.setBorderRight(BorderStyle.THIN);

            // Match-Style
            CellStyle matchStyle = wb.createCellStyle();
            matchStyle.setAlignment(HorizontalAlignment.CENTER);
            matchStyle.setWrapText(true);
            matchStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            matchStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            matchStyle.setBorderBottom(BorderStyle.THIN);
            matchStyle.setBorderTop(BorderStyle.THIN);
            matchStyle.setBorderLeft(BorderStyle.THIN);
            matchStyle.setBorderRight(BorderStyle.THIN);

            // Pause-Style
            CellStyle pauseStyle = wb.createCellStyle();
            pauseStyle.setAlignment(HorizontalAlignment.CENTER);
            pauseStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            pauseStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            pauseStyle.setBorderBottom(BorderStyle.THIN);
            pauseStyle.setBorderTop(BorderStyle.THIN);
            pauseStyle.setBorderLeft(BorderStyle.THIN);
            pauseStyle.setBorderRight(BorderStyle.THIN);

            int anzahlPlaetze = aktuellesTurnier.getAnzahlPlaetze();
            int rundenAnzahl = aktuellesTurnier.getRundenAnzahl();

            // Kopfzeile: Zeit, Platz 1 .. Platz N, Pausierende Spieler
            Row headerRow = sheet.createRow(0);
            Cell cell = headerRow.createCell(0);
            cell.setCellValue("Zeit");
            cell.setCellStyle(headerStyle);
            for (int p = 1; p <= anzahlPlaetze; p++) {
                cell = headerRow.createCell(p);
                cell.setCellValue("Platz " + p);
                cell.setCellStyle(headerStyle);
            }
            cell = headerRow.createCell(anzahlPlaetze + 1);
            cell.setCellValue("Pausierende Spieler");
            cell.setCellStyle(headerStyle);

            int startZeitMin = startTime.toSecondOfDay() / 60;
            int rowIndex = 1;
            for (int r = 0; r < rundenAnzahl; r++) {
                // Zeile für Matches
                Row matchRow = sheet.createRow(rowIndex++);
                String zeit = String.format("%02d:%02d", startZeitMin / 60, startZeitMin % 60);
                cell = matchRow.createCell(0);
                cell.setCellValue(zeit);
                cell.setCellStyle(timeStyle);
                Runde ru = aktuellesTurnier.getRunden().get(r);
                List<Match> spiele = ru.getSpiele();
                for (int p = 0; p < anzahlPlaetze; p++) {
                    cell = matchRow.createCell(p + 1);
                    if (p < spiele.size()) {
                        cell.setCellValue(spiele.get(p).toString());
                    } else {
                        cell.setCellValue("-");
                    }
                    cell.setCellStyle(matchStyle);
                }
                // Letzte Spalte: Pausierende
                List<Spieler> paused = pausenProRunde.get(r);
                StringBuilder sb = new StringBuilder();
                for (Spieler sp : paused) {
                    sb.append(sp.getName()).append(", ");
                }
                if (sb.length() > 1) sb.setLength(sb.length() - 2);
                cell = matchRow.createCell(anzahlPlaetze + 1);
                cell.setCellValue(sb.toString());
                cell.setCellStyle(matchStyle);

                // Zeile für Pausenzeile optisch (optional)
                Row pauseRow = sheet.createRow(rowIndex++);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        pauseRow.getRowNum(), pauseRow.getRowNum(), 0, anzahlPlaetze + 1));
                cell = pauseRow.createCell(0);
                cell.setCellValue(pausenlaenge > 0 ? "Pause: " + pausenlaenge + " Min" : "—");
                cell.setCellStyle(pauseStyle);

                // Update der Startzeit
                startZeitMin += (pausenlaenge > 0 ? spieldauer + pausenlaenge : spieldauer);
            }
            for (int col = 0; col <= anzahlPlaetze + 1; col++) {
                sheet.autoSizeColumn(col);
            }
            try (FileOutputStream fos = new FileOutputStream(f)) {
                wb.write(fos);
            }
            Alert alert = new Alert(AlertType.INFORMATION,
                    "Excel Export erfolgreich:\n" + f.getAbsolutePath(),
                    ButtonType.OK);
            alert.showAndWait();
            updateStatus("Excel Export erfolgreich: " + f.getAbsolutePath(), "success-label");
        } catch (IOException ex) {
            showErrorAlert("Fehler beim Excel Export: " + ex.getMessage());
            updateStatus("Fehler beim Excel Export.", "error-label");
        }
    }

    private void exportTurnierergebnisseToExcel() {
        if (aktuellesTurnier == null) {
            showErrorAlert("Kein Turnier zum Export!");
            updateStatus("Fehler: Kein Turnier zum Exportieren.", "error-label");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excel Turnierergebnisse speichern");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File f = fileChooser.showSaveDialog(primary);
        if (f == null) {
            updateStatus("Export abgebrochen.", "info-label");
            return;
        }
        try (Workbook wb = new XSSFWorkbook()) {
            // Sheet 1: Rangliste
            Sheet sheetRanking = wb.createSheet("Rangliste");
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheetRanking.createRow(0);
            Cell cell = headerRow.createCell(0);
            cell.setCellValue("Rang");
            cell.setCellStyle(headerStyle);
            cell = headerRow.createCell(1);
            cell.setCellValue("Spieler");
            cell.setCellStyle(headerStyle);
            cell = headerRow.createCell(2);
            cell.setCellValue("Spielstärke");
            cell.setCellStyle(headerStyle);
            cell = headerRow.createCell(3);
            cell.setCellValue("Punkte");
            cell.setCellStyle(headerStyle);

            // Sortiere scoreboardMap absteigend nach Punkten
            List<Map.Entry<Spieler, Integer>> ranking = new ArrayList<>(scoreboardMap.entrySet());
            ranking.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            int rowIndex = 1;
            for (Map.Entry<Spieler, Integer> entry : ranking) {
                Row row = sheetRanking.createRow(rowIndex);
                row.createCell(0).setCellValue(rowIndex);
                row.createCell(1).setCellValue(entry.getKey().getName());
                row.createCell(2).setCellValue(entry.getKey().getSpielstaerke());
                row.createCell(3).setCellValue(entry.getValue());
                rowIndex++;
            }
            for (int i = 0; i < 4; i++) {
                sheetRanking.autoSizeColumn(i);
            }

            // Sheet 2: Statistiken
            Sheet sheetStats = wb.createSheet("Statistiken");
            int totalMatches = 0;
            for (Runde r : aktuellesTurnier.getRunden()) {
                totalMatches += r.getSpiele().size();
            }
            Row statRow1 = sheetStats.createRow(0);
            statRow1.createCell(0).setCellValue("Gesamtspieler");
            statRow1.createCell(1).setCellValue(spielerListe.size());
            Row statRow2 = sheetStats.createRow(1);
            statRow2.createCell(0).setCellValue("Gesamtrunden");
            statRow2.createCell(1).setCellValue(aktuellesTurnier.getRundenAnzahl());
            Row statRow3 = sheetStats.createRow(2);
            statRow3.createCell(0).setCellValue("Gesamtmatches");
            statRow3.createCell(1).setCellValue(totalMatches);

            sheetStats.autoSizeColumn(0);
            sheetStats.autoSizeColumn(1);

            try (FileOutputStream fos = new FileOutputStream(f)) {
                wb.write(fos);
            }
            Alert alert = new Alert(AlertType.INFORMATION,
                    "Excel Export (Turnierergebnisse) erfolgreich:\n" + f.getAbsolutePath(),
                    ButtonType.OK);
            alert.showAndWait();
            updateStatus("Excel Export (Turnierergebnisse) erfolgreich: " + f.getAbsolutePath(), "success-label");
        } catch (IOException ex) {
            showErrorAlert("Fehler beim Excel Export (Turnierergebnisse): " + ex.getMessage());
            updateStatus("Fehler beim Excel Export (Turnierergebnisse).", "error-label");
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

    // DRUCKEN
    private void printPlan() {
        if (spielplanGrid == null) {
            showErrorAlert("Nichts zu drucken.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showErrorAlert("Kein Druckerjob verfügbar.");
            return;
        }
        boolean proceed = job.showPrintDialog(primary);
        if (!proceed) {
            updateStatus("Druck abgebrochen.", "info-label");
            return;
        }
        // Optional leicht skalieren
        double oldX = spielplanGrid.getScaleX();
        double oldY = spielplanGrid.getScaleY();
        spielplanGrid.setScaleX(0.9);
        spielplanGrid.setScaleY(0.9);

        boolean success = job.printPage(spielplanGrid);

        // zurücksetzen
        spielplanGrid.setScaleX(oldX);
        spielplanGrid.setScaleY(oldY);

        if (success) {
            job.endJob();
            updateStatus("Druck erfolgreich.", "success-label");
        } else {
            showErrorAlert("Drucken fehlgeschlagen.");
            updateStatus("Drucken fehlgeschlagen.", "error-label");
        }
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
