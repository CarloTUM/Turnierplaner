package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.List;

public class Spieler {
    private String name;
    private String geschlecht;  // z.B. "M" oder "F"
    private int spielstaerke;   // Werte von 1 bis 10
    private int pausenAnzahl;
    private int spielAnzahl;
    private List<Spieler> partnerHistorie;
    private List<Spieler> gegnerHistorie;

    public Spieler(String name, String geschlecht, int spielstaerke) {
        this.name = name;
        this.geschlecht = geschlecht;
        this.spielstaerke = spielstaerke;
        this.pausenAnzahl = 0;
        this.spielAnzahl = 0;
        this.partnerHistorie = new ArrayList<>();
        this.gegnerHistorie = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getGeschlecht() {
        return geschlecht;
    }

    public int getSpielstaerke() {
        return spielstaerke;
    }

    public int getPausenAnzahl() {
        return pausenAnzahl;
    }

    public int getSpielAnzahl() {
        return spielAnzahl;
    }

    public void erhoehePausenAnzahl() {
        this.pausenAnzahl++;
    }

    public void resetPausenAnzahl() {
        this.pausenAnzahl = 0;
    }

    public void incrementSpielAnzahl() {
        this.spielAnzahl++;
    }

    public void resetSpielAnzahl() {
        this.spielAnzahl = 0;
    }

    public List<Spieler> getPartnerHistorie() {
        return partnerHistorie;
    }

    public List<Spieler> getGegnerHistorie() {
        return gegnerHistorie;
    }

    public void addGegner(Spieler gegner) {
        if (!gegnerHistorie.contains(gegner)) {
            gegnerHistorie.add(gegner);
        }
    }

    public void resetPartnerHistorie() {
        this.partnerHistorie.clear();
    }

    public void resetGegnerHistorie() {
        this.gegnerHistorie.clear();
    }

    @Override
    public String toString() {
        return name + " (Stärke: " + spielstaerke + ")";
    }
}
