package com.example.roundrobintunier;

public class MatchResult {
    private int team1Score;
    private int team2Score;

    public MatchResult(int team1Score, int team2Score) {
        this.team1Score = team1Score;
        this.team2Score = team2Score;
    }

    public int getTeam1Score() {
        return team1Score;
    }

    public void setTeam1Score(int team1Score) {
        this.team1Score = team1Score;
    }

    public int getTeam2Score() {
        return team2Score;
    }

    public void setTeam2Score(int team2Score) {
        this.team2Score = team2Score;
    }

    @Override
    public String toString() {
        return team1Score + " : " + team2Score;
    }
}
