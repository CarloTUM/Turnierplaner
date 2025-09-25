package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PausenManager {
    private final List<Spieler> spielerListe;
    private final int rundenAnzahl;
    private final int anzahlPlaetze;

    // Für jede Runde: die pausierenden Spieler
    private final List<List<Spieler>> pausenProRunde;

    public PausenManager(List<Spieler> spielerListe, int rundenAnzahl, int anzahlPlaetze) {
        this.spielerListe = new ArrayList<>(spielerListe);
        this.rundenAnzahl = rundenAnzahl;
        this.anzahlPlaetze = anzahlPlaetze;
        this.pausenProRunde = new ArrayList<>();
    }

    /**
     * Plant alle Runden und gibt je Runde die Liste der spielenden Spieler zurück.
     */
    public List<List<Spieler>> planeRunden() {
        List<List<Spieler>> rundenPlan = new ArrayList<>();

        for (int runde = 0; runde < rundenAnzahl; runde++) {
            // Fairness bei Gleichstand: zuerst durchmischen
            Collections.shuffle(spielerListe);

            // Nach bisherigen Pausen absteigend (mehr Pausen = eher dran mit Spielen)
            spielerListe.sort(Comparator.comparingInt(Spieler::getPausenAnzahl).reversed());

            int maxSpielerInRunde = Math.min(spielerListe.size(), anzahlPlaetze * 4);
            // Auf Vielfaches von 4 abrunden (je Platz ein Doppel-Match)
            maxSpielerInRunde -= (maxSpielerInRunde % 4);

            List<Spieler> spielerInRunde = new ArrayList<>();
            for (int i = 0; i < maxSpielerInRunde; i++) {
                spielerInRunde.add(spielerListe.get(i));
            }

            List<Spieler> spielerInPause = new ArrayList<>();
            for (int i = maxSpielerInRunde; i < spielerListe.size(); i++) {
                spielerInPause.add(spielerListe.get(i));
            }

            // Anti-Back-to-Back-Pause: niemand soll zwei Runden hintereinander pausieren
            if (runde > 0 && !spielerInPause.isEmpty()) {
                List<Spieler> vorherigePausen = pausenProRunde.get(runde - 1);
                List<Spieler> pauseKopie = new ArrayList<>(spielerInPause);
                for (Spieler p : pauseKopie) {
                    if (vorherigePausen.contains(p)) {
                        boolean getauscht = false;
                        for (int i = 0; i < spielerInRunde.size(); i++) {
                            Spieler kandidat = spielerInRunde.get(i);
                            if (!vorherigePausen.contains(kandidat)) {
                                // Tausch: p spielt, kandidat pausiert
                                spielerInRunde.set(i, p);
                                spielerInPause.remove(p);
                                spielerInPause.add(kandidat);
                                getauscht = true;
                                break;
                            }
                        }
                        // kein Tausch möglich -> p bleibt in Pause (ok)
                    }
                }
            }

            // Jetzt final: Pausen zählen + Spielanzahl für Aktive zählen
            for (Spieler s : spielerInPause) s.erhoehePausenAnzahl();
            for (Spieler s : spielerInRunde) s.incrementSpielAnzahl();

            rundenPlan.add(spielerInRunde);
            pausenProRunde.add(spielerInPause);
        }
        return rundenPlan;
    }

    /** Für Auswertungen: wer hat wann pausiert (Index 0 = Runde 1). */
    public List<List<Spieler>> getPausenProRunde() {
        return pausenProRunde;
    }
}
