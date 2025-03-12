package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.List;

public class Runde {
    private int rundenNummer;
    private List<Match> matches;

    public Runde(int rundenNummer) {
        this.rundenNummer = rundenNummer;
        this.matches = new ArrayList<>();
    }

    public int getRundenNummer() {
        return rundenNummer;
    }

    public List<Match> getMatches() {
        return matches;
    }

    // Getter für Spiele (Alias für Matches)
    public List<Match> getSpiele() {
        return matches;
    }

    public void addMatch(Match match) {
        matches.add(match);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Runde " + rundenNummer + ":\n");
        for (Match match : matches) {
            sb.append(match.toString()).append("\n");
        }
        return sb.toString();
    }
}
