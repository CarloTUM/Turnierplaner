package com.example.roundrobintunier;

public class Match {
    private final Team team1;
    private final Team team2;
    private MatchResult result;

    public Match(Team team1, Team team2) {
        this.team1 = team1;
        this.team2 = team2;
        this.result = null;
    }

    public Team getTeam1() { return team1; }
    public Team getTeam2() { return team2; }
    public MatchResult getResult() { return result; }
    public void setResult(MatchResult result) { this.result = result; }

    @Override
    public String toString() {
        return team1 + " vs " + team2;
    }
}
