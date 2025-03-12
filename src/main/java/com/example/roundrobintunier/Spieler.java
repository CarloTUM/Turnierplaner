package com.example.roundrobintunier;

import java.util.HashSet;
import java.util.Set;

public class Spieler {
    private String name;
    private String geschlecht;
    private int pausenAnzahl;
    private int spielAnzahl;
    private Set<Spieler> partnerHistorie;

    // NEU: Hier speichern wir Gegner, gegen die dieser Spieler schon gespielt hat:
    private Set<Spieler> gegnerHistorie;

    public Spieler(String name, String geschlecht) {
        this.name = name;
        this.geschlecht = geschlecht;
        this.pausenAnzahl = 0;
        this.spielAnzahl = 0;
        this.partnerHistorie = new HashSet<>();
        this.gegnerHistorie = new HashSet<>(); // NEU
    }

    public String getName() {
        return name;
    }

    public String getGeschlecht() {
        return geschlecht;
    }

    public int getPausenAnzahl() {
        return pausenAnzahl;
    }

    public int getSpielAnzahl() {
        return spielAnzahl;
    }

    public Set<Spieler> getPartnerHistorie() {
        return partnerHistorie;
    }

    public Set<Spieler> getGegnerHistorie() {
        return gegnerHistorie;
    }

    public void addGegner(Spieler s) {
        this.gegnerHistorie.add(s);
    }

    public void resetGegnerHistorie() {
        this.gegnerHistorie.clear();
    }

    public void erhoehePausenAnzahl() {
        pausenAnzahl++;
    }

    public void erhoeheSpielAnzahl() {
        spielAnzahl++;
    }

    public void resetPausenAnzahl() {
        pausenAnzahl = 0;
    }

    public void resetSpielAnzahl() {
        spielAnzahl = 0;
    }

    public void resetPartnerHistorie() {
        partnerHistorie.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Spieler that = (Spieler) o;
        return name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
