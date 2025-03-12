package com.example.roundrobintunier;

public class Match {
    private Team team1;
    private Team team2;
    private MatchResult result; // Neues Feld für das Ergebnis

    public Match(Team team1, Team team2) {
        this.team1 = team1;
        this.team2 = team2;
        this.result = null; // Initial kein Ergebnis
    }

    public Team getTeam1() {
        return this.team1;
    }

    public Team getTeam2() {
        return this.team2;
    }

    public MatchResult getResult() {
        return this.result;
    }

    public void setResult(MatchResult result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return team1.toString() + " vs " + team2.toString();
    }
}
