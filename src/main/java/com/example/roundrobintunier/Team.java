package com.example.roundrobintunier;

public class Team {
    private final Spieler spieler1;
    private final Spieler spieler2;

    public Team(Spieler spieler1, Spieler spieler2) {
        this.spieler1 = spieler1;
        this.spieler2 = spieler2;
    }

    public Spieler getSpieler1() { return spieler1; }
    public Spieler getSpieler2() { return spieler2; }

    @Override
    public String toString() {
        return spieler1 + " & " + spieler2;
    }
}
