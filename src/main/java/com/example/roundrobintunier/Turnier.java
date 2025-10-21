package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine reine Datenklasse, die ein erstelltes Turnier repräsentiert.
 * Die Logik zur Erstellung des Plans befindet sich in der App-Klasse.
 */
public class Turnier {
    private final List<Spieler> spielerListe;
    private final int anzahlPlaetze;
    private final int rundenAnzahl;
    private final List<Runde> runden;

    public Turnier(List<Spieler> spielerListe, int anzahlPlaetze, int rundenAnzahl) {
        this.spielerListe = new ArrayList<>(spielerListe);
        this.anzahlPlaetze = anzahlPlaetze;
        this.rundenAnzahl = rundenAnzahl;
        this.runden = new ArrayList<>(); // Runden werden von außen hinzugefügt
    }

    // Getter-Methoden bleiben unverändert
    public List<Spieler> getSpielerListe() {
        return spielerListe;
    }

    public int getAnzahlPlaetze() {
        return anzahlPlaetze;
    }

    public int getRundenAnzahl() {
        return rundenAnzahl;
    }

    public List<Runde> getRunden() {
        return runden;
    }
}