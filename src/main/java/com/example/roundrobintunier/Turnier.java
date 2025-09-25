package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.List;

public class Turnier {
    private final List<Spieler> spielerListe;
    private final int anzahlPlaetze;
    private final int rundenAnzahl;
    private final List<Runde> runden;

    public Turnier(List<Spieler> spielerListe, int anzahlPlaetze, int rundenAnzahl) {
        this.spielerListe = new ArrayList<>(spielerListe);
        this.anzahlPlaetze = anzahlPlaetze;
        this.rundenAnzahl = rundenAnzahl;
        this.runden = new ArrayList<>();
    }

    public List<Spieler> getSpielerListe() { return spielerListe; }
    public int getAnzahlPlaetze() { return anzahlPlaetze; }
    public int getRundenAnzahl() { return rundenAnzahl; }
    public List<Runde> getRunden() { return runden; }

    /** Erzeugt den Turnierplan. */
    public void turnierPlanErstellen() {
        PausenManager pausenManager = new PausenManager(this.spielerListe, this.rundenAnzahl, this.anzahlPlaetze);
        List<List<Spieler>> rundenPlan = pausenManager.planeRunden();

        TournamentSolver solver = new TournamentSolver();
        for (int i = 0; i < rundenPlan.size(); i++) {
            List<Spieler> spielerInRunde = rundenPlan.get(i);
            Runde optimiert = solver.solveRunde(i + 1, spielerInRunde);
            if (optimiert != null) {
                this.runden.add(optimiert);
            } else {
                System.out.println("Keine gültige Planung für Runde " + (i + 1) + " gefunden.");
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Runde r : runden) sb.append(r).append("\n");
        return sb.toString();
    }
}
