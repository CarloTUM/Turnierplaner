package com.example.roundrobintunier;

import java.util.HashSet;
import java.util.Set;

public class Team {
    private Spieler spieler1;
    private Spieler spieler2;
    private Set<Team> gegnerHistorie;

    public Team(Spieler spieler1, Spieler spieler2) {
        this.spieler1 = spieler1;
        this.spieler2 = spieler2;
        this.gegnerHistorie = new HashSet<>();
    }

    public Spieler getSpieler1() {
        return this.spieler1;
    }

    public Spieler getSpieler2() {
        return this.spieler2;
    }

    public void addGespielterGegner(Team gegnerTeam) {
        this.gegnerHistorie.add(gegnerTeam);
    }

    public boolean hatGegenTeamGespielt(Team gegnerTeam) {
        return this.gegnerHistorie.contains(gegnerTeam);
    }

    @Override
    public String toString() {
        return spieler1.getName() + " & " + spieler2.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Team team = (Team) o;
        return (spieler1.equals(team.spieler1) && spieler2.equals(team.spieler2)) ||
                (spieler1.equals(team.spieler2) && spieler2.equals(team.spieler1));
    }

    @Override
    public int hashCode() {
        return spieler1.hashCode() + spieler2.hashCode();
    }
}
