package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.List;

public class Turnier {
    private List<Spieler> spielerListe;
    private int anzahlPlaetze;
    private int rundenAnzahl;
    private List<Runde> runden;

    public Turnier(List<Spieler> spielerListe, int anzahlPlaetze, int rundenAnzahl) {
        this.spielerListe = spielerListe;
        this.anzahlPlaetze = anzahlPlaetze;
        this.rundenAnzahl = rundenAnzahl;
        this.runden = new ArrayList<>();
    }

    public int getAnzahlPlaetze() {
        return this.anzahlPlaetze;
    }

    public int getRundenAnzahl() {
        return this.rundenAnzahl;
    }

    public List<Runde> getRunden() {
        return this.runden;
    }

    /**
     * Erstellen des Turnierplans mithilfe des PausenManagers und TournamentSolvers.
     */
    public void turnierPlanErstellen() {
        // Pausenlogik ausführen
        PausenManager pausenManager = new PausenManager(this.spielerListe, this.rundenAnzahl, this.anzahlPlaetze);
        List<List<Spieler>> rundenPlan = pausenManager.planeRunden();

        // Runde für Runde mit dem Solver optimieren
        TournamentSolver solver = new TournamentSolver();
        for (int rundeNummer = 0; rundeNummer < rundenPlan.size(); rundeNummer++) {
            List<Spieler> spielerInRunde = rundenPlan.get(rundeNummer);
            Runde optimierteRunde = solver.solveRunde(rundeNummer + 1, spielerInRunde);
            if (optimierteRunde != null) {
                this.runden.add(optimierteRunde);
            } else {
                System.out.println("Keine gültige Planung für Runde " + (rundeNummer + 1) + " gefunden.");
            }
        }
    }

    /**
     * Gibt den gesamten Turnierplan als String aus.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Runde runde : runden) {
            sb.append(runde.toString()).append("\n");
        }
        return sb.toString();
    }
}
