package com.example.roundrobintunier;

public class Team {
    private Spieler spieler1;
    private Spieler spieler2;

    public Team(Spieler spieler1, Spieler spieler2) {
        this.spieler1 = spieler1;
        this.spieler2 = spieler2;
    }

    public Spieler getSpieler1() {
        return spieler1;
    }

    public Spieler getSpieler2() {
        return spieler2;
    }

    @Override
    public String toString() {
        return spieler1.toString() + " & " + spieler2.toString();
    }
}
