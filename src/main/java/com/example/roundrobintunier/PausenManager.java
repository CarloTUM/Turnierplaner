package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PausenManager {
    private List<Spieler> spielerListe;
    private int rundenAnzahl;
    private int anzahlPlaetze;

    // Neue Liste, in der für jede Runde die pausierenden Spieler gespeichert werden
    private List<List<Spieler>> pausenProRunde;

    public PausenManager(List<Spieler> spielerListe, int rundenAnzahl, int anzahlPlaetze) {
        this.spielerListe = spielerListe;
        this.rundenAnzahl = rundenAnzahl;
        this.anzahlPlaetze = anzahlPlaetze;

        // Initialisieren der neuen Datenstruktur
        this.pausenProRunde = new ArrayList<>();
    }

    /**
     * Plane die Spieler für alle Runden. Gibt eine Liste zurück,
     * deren Einträge jeweils die in dieser Runde "spielenden" Spieler sind.
     */
    public List<List<Spieler>> planeRunden() {
        List<List<Spieler>> rundenPlan = new ArrayList<>();

        for (int runde = 0; runde < rundenAnzahl; runde++) {
            // Sortiere Spieler nach Anzahl der bisherigen Pausen (absteigend)
            spielerListe.sort(Comparator.comparingInt(Spieler::getPausenAnzahl).reversed());

            // Wähle die ersten (anzahlPlaetze * 4) Spieler aus, die in dieser Runde spielen sollen
            // Beispiel-Logik: 4 Spieler pro Platz
            // Debug-Ausgabe
            System.out.println("===== Runde " + (runde+1) + " =====");
            System.out.println("Vor der Auswahl sortierte SpielerListe (nach Pausen, absteigend):");
            for (Spieler s : spielerListe) {
                System.out.println("  " + s.getName() + " (Pausen=" + s.getPausenAnzahl() + ")");
            }

            List<Spieler> spielerInRunde = new ArrayList<>();
            int maxSpielerInRunde = anzahlPlaetze * 4;
            for (int i = 0; i < maxSpielerInRunde && i < spielerListe.size(); i++) {
                spielerInRunde.add(spielerListe.get(i));
            }

            // Spieler, die nicht in dieser Runde spielen -> PausenAnzahl++
            List<Spieler> spielerInPause = new ArrayList<>();
            for (Spieler spieler : spielerListe) {
                if (!spielerInRunde.contains(spieler)) {
                    spieler.erhoehePausenAnzahl();
                    spielerInPause.add(spieler);
                }
            }

            // Debug-Ausgabe
            System.out.println("Runde " + (runde+1) + ": spielerInRunde.size()=" + spielerInRunde.size());
            System.out.println("Runde " + (runde+1) + ": spielerInPause.size()=" + spielerInPause.size());

            // Speichere die spielenden Spieler in den Rundenplan
            rundenPlan.add(spielerInRunde);

            // Speichere die pausierenden Spieler separat
            pausenProRunde.add(spielerInPause);
        }

        // Debug-Ausgabe der gesamten pausenProRunde
        System.out.println("===== pausenProRunde (nach planeRunden) =====");
        for (int i = 0; i < pausenProRunde.size(); i++) {
            System.out.println("Runde " + (i+1) + " pausierend: " + pausenProRunde.get(i));
        }

        return rundenPlan;
    }

    /**
     * Gibt die Liste zurück, in der für jede Runde die pausierenden Spieler abgelegt sind.
     * Index 0 entspricht Runde 1, Index 1 entspricht Runde 2 usw.
     */
    public List<List<Spieler>> getPausenProRunde() {
        return pausenProRunde;
    }
}
