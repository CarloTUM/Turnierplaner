package com.example.roundrobintunier;

import java.util.ArrayList;
import java.util.List;

public class Runde {
    private int rundenNummer;
    private List<Match> spiele;

    public Runde(int rundenNummer) {
        this.rundenNummer = rundenNummer;
        this.spiele = new ArrayList<>();
    }

    public int getRundenNummer() {
        return rundenNummer;
    }

    public List<Match> getSpiele() {
        return spiele;
    }

    public void addMatch(Match match) {
        this.spiele.add(match);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Runde ").append(rundenNummer).append(":\n");
        for (Match match : spiele) {
            sb.append(match.toString()).append("\n");
        }
        return sb.toString();
    }
}
