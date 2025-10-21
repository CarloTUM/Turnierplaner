package com.example.roundrobintunier;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;

/**
 * Runde mit hoher (weicher) Priorität: keine Partner-Wiederholungen; dann Gegner; dann Skill; dann Mixed.
 * NEU: Option, um Mixed-Doppel zu erzwingen.
 */
public class TournamentSolver {

    public Runde solveRunde(int rundenNummer, List<Spieler> spielerInRunde, boolean forceMixed) {
        Model model = new Model("Runde " + rundenNummer);
        int numPlayers = spielerInRunde.size();

        if (numPlayers == 0 || numPlayers % 4 != 0) {
            System.out.println("[Solver] Spielerzahl ist 0 oder nicht durch 4 teilbar – Runde " + rundenNummer + " wird übersprungen.");
            return null;
        }

        int numMatches = numPlayers / 4;

        // Geschlechter: M = 0, F = 1 (alles außer "F" wird als 0 behandelt)
        int[] genderLevels = new int[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            String g = spielerInRunde.get(i).getGeschlecht();
            g = (g == null) ? "" : g.trim().toUpperCase();
            genderLevels[i] = "F".equals(g) ? 1 : 0;
        }

        // Variablen: matches[m][p] = Index des Spielers in spielerInRunde
        IntVar[][] matches = new IntVar[numMatches][4];
        for (int m = 0; m < numMatches; m++) {
            for (int p = 0; p < 4; p++) {
                matches[m][p] = model.intVar("M" + m + "_P" + p, 0, numPlayers - 1);
            }
        }

        // Jeder Spieler genau einmal
        List<IntVar> all = new ArrayList<>();
        for (IntVar[] row : matches) for (IntVar v : row) all.add(v);
        model.allDifferent(all.toArray(new IntVar[0])).post();

        // --- Symmetriebrecher ---
        for (int m = 0; m < numMatches; m++) {
            model.arithm(matches[m][0], "<", matches[m][1]).post();
            model.arithm(matches[m][2], "<", matches[m][3]).post();
        }
        for (int m = 1; m < numMatches; m++) {
            model.arithm(matches[m - 1][0], "<", matches[m][0]).post();
        }

        // --- Partner-Wiederholung ---
        List<int[]> forbiddenPairs = new ArrayList<>();
        Map<Spieler, Integer> idxOf = new HashMap<>();
        for (int i = 0; i < numPlayers; i++) idxOf.put(spielerInRunde.get(i), i);

        boolean[][] seenPartner = new boolean[numPlayers][numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            for (Spieler partner : spielerInRunde.get(i).getPartnerHistorie()) {
                Integer jObj = idxOf.get(partner);
                if (jObj == null) continue;
                int j = jObj;
                int a = Math.min(i, j), b = Math.max(i, j);
                if (a == b) continue;
                if (!seenPartner[a][b]) {
                    seenPartner[a][b] = true;
                    forbiddenPairs.add(new int[]{a, b});
                }
            }
        }

        List<BoolVar> teamRepeatViolations = new ArrayList<>();
        for (int m = 0; m < numMatches; m++) {
            IntVar p1 = matches[m][0];
            IntVar p2 = matches[m][1];
            IntVar p3 = matches[m][2];
            IntVar p4 = matches[m][3];

            for (int[] ab : forbiddenPairs) {
                int a = ab[0], b = ab[1];

                BoolVar t1rep = model.boolVar();
                model.and(model.arithm(p1, "=", a), model.arithm(p2, "=", b)).reifyWith(t1rep);
                teamRepeatViolations.add(t1rep);

                BoolVar t2rep = model.boolVar();
                model.and(model.arithm(p3, "=", a), model.arithm(p4, "=", b)).reifyWith(t2rep);
                teamRepeatViolations.add(t2rep);
            }
        }

        // --- Gegner-Wiederholungen (symmetrisch & dedupliziert) ---
        boolean[][] seenOppPair = new boolean[numPlayers][numPlayers];
        List<int[]> opponentPairs = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++) {
            for (Spieler gegner : spielerInRunde.get(i).getGegnerHistorie()) {
                Integer jObj = idxOf.get(gegner);
                if (jObj == null) continue;
                int j = jObj;
                if (i == j) continue;
                int a = Math.min(i, j), b = Math.max(i, j);
                if (!seenOppPair[a][b]) {
                    seenOppPair[a][b] = true;
                    opponentPairs.add(new int[]{a, b});
                }
            }
        }

        List<BoolVar> opponentRepeatViolations = new ArrayList<>();
        for (int m = 0; m < numMatches; m++) {
            IntVar p1 = matches[m][0], p2 = matches[m][1], p3 = matches[m][2], p4 = matches[m][3];

            for (int[] ab : opponentPairs) {
                int i = ab[0], j = ab[1];
                BoolVar iLeft = model.boolVar();
                model.or(model.arithm(p1, "=", i), model.arithm(p2, "=", i)).reifyWith(iLeft);
                BoolVar jRight = model.boolVar();
                model.or(model.arithm(p3, "=", j), model.arithm(p4, "=", j)).reifyWith(jRight);
                BoolVar iRight = model.boolVar();
                model.or(model.arithm(p3, "=", i), model.arithm(p4, "=", i)).reifyWith(iRight);
                BoolVar jLeft = model.boolVar();
                model.or(model.arithm(p1, "=", j), model.arithm(p2, "=", j)).reifyWith(jLeft);
                BoolVar iL_and_jR = model.boolVar();
                model.and(iLeft, jRight).reifyWith(iL_and_jR);
                BoolVar iR_and_jL = model.boolVar();
                model.and(iRight, jLeft).reifyWith(iR_and_jL);
                BoolVar oppHere = model.boolVar();
                model.or(iL_and_jR, iR_and_jL).reifyWith(oppHere);
                opponentRepeatViolations.add(oppHere);
            }
        }

        // --- Mixed ---
        List<BoolVar> mixedViolations = new ArrayList<>();
        for (int m = 0; m < numMatches; m++) {
            IntVar p1 = matches[m][0], p2 = matches[m][1], p3 = matches[m][2], p4 = matches[m][3];

            IntVar g1 = model.intVar("g1_m" + m, 0, 1); model.element(g1, genderLevels, p1, 0).post();
            IntVar g2 = model.intVar("g2_m" + m, 0, 1); model.element(g2, genderLevels, p2, 0).post();

            // NEU: Logik für forceMixed
            if (forceMixed) {
                // Harte Bedingung: Geschlechter müssen unterschiedlich sein
                model.arithm(g1, "!=", g2).post();
            } else {
                // Weiche Bedingung wie bisher: Strafe für gleiche Geschlechter
                BoolVar sameGender1 = model.boolVar();
                model.arithm(g1, "=", g2).reifyWith(sameGender1);
                mixedViolations.add(sameGender1);
            }

            IntVar g3 = model.intVar("g3_m" + m, 0, 1); model.element(g3, genderLevels, p3, 0).post();
            IntVar g4 = model.intVar("g4_m" + m, 0, 1); model.element(g4, genderLevels, p4, 0).post();

            if (forceMixed) {
                model.arithm(g3, "!=", g4).post();
            } else {
                BoolVar sameGender2 = model.boolVar();
                model.arithm(g3, "=", g4).reifyWith(sameGender2);
                mixedViolations.add(sameGender2);
            }
        }


        // --- Skill-Balance ---
        int[] skillLevels = new int[numPlayers];
        for (int i = 0; i < numPlayers; i++) skillLevels[i] = spielerInRunde.get(i).getSpielstaerke();
        int minSkill = Arrays.stream(skillLevels).min().orElse(1);
        int maxSkill = Arrays.stream(skillLevels).max().orElse(10);

        List<IntVar> skillDiffs = new ArrayList<>();
        for (int m = 0; m < numMatches; m++) {
            IntVar p1 = matches[m][0], p2 = matches[m][1], p3 = matches[m][2], p4 = matches[m][3];
            IntVar s1 = model.intVar(minSkill, maxSkill); model.element(s1, skillLevels, p1, 0).post();
            IntVar s2 = model.intVar(minSkill, maxSkill); model.element(s2, skillLevels, p2, 0).post();
            IntVar s3 = model.intVar(minSkill, maxSkill); model.element(s3, skillLevels, p3, 0).post();
            IntVar s4 = model.intVar(minSkill, maxSkill); model.element(s4, skillLevels, p4, 0).post();
            IntVar t1 = model.intVar(2 * minSkill, 2 * maxSkill);
            IntVar t2 = model.intVar(2 * minSkill, 2 * maxSkill);
            model.sum(new IntVar[]{s1, s2}, "=", t1).post();
            model.sum(new IntVar[]{s3, s4}, "=", t2).post();
            IntVar diff = model.intVar(0, 2 * (maxSkill - minSkill));
            model.distance(t1, t2, "=", diff).post();
            skillDiffs.add(diff);
        }

        // --- Summen ---
        IntVar teamRepeatSum = model.intVar(0, teamRepeatViolations.size());
        if (!teamRepeatViolations.isEmpty()) model.sum(teamRepeatViolations.toArray(new BoolVar[0]), "=", teamRepeatSum).post();
        IntVar opponentRepeatSum = model.intVar(0, opponentRepeatViolations.size());
        if (!opponentRepeatViolations.isEmpty()) model.sum(opponentRepeatViolations.toArray(new BoolVar[0]), "=", opponentRepeatSum).post();
        IntVar mixedSum = model.intVar(0, mixedViolations.size());
        if (!mixedViolations.isEmpty()) model.sum(mixedViolations.toArray(new BoolVar[0]), "=", mixedSum).post();
        int maxSkillDiffPerMatch = 2 * (maxSkill - minSkill);
        IntVar skillDiffSum = model.intVar(0, maxSkillDiffPerMatch * numMatches);
        if (!skillDiffs.isEmpty()) model.sum(skillDiffs.toArray(new IntVar[0]), "=", skillDiffSum).post();

        // --- Gewichte ---
        int wTeamRepeat = 10_000;
        int wOpponent   = 500;
        int wMixed      = forceMixed ? 0 : 50; // Wenn Mixed erzwungen wird, hat die Strafe kein Gewicht
        int wSkillDiff  = 200;

        // --- Zielfunktion & dynamische Obergrenze ---
        long maxObjLong =
                1L * teamRepeatViolations.size() * wTeamRepeat +
                        1L * opponentRepeatViolations.size() * wOpponent +
                        1L * mixedViolations.size() * wMixed +
                        1L * (maxSkillDiffPerMatch * numMatches) * wSkillDiff;
        int maxObj = (maxObjLong > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) maxObjLong;
        IntVar objective = model.intVar("objective", 0, Math.max(0, maxObj));
        model.scalar(
                new IntVar[]{teamRepeatSum, opponentRepeatSum, mixedSum, skillDiffSum},
                new int[]   {wTeamRepeat,   wOpponent,        wMixed,   wSkillDiff},
                "=", objective
        ).post();
        model.setObjective(Model.MINIMIZE, objective);

        // --- Lösen ---
        Runde runde = new Runde(rundenNummer);
        if (model.getSolver().solve()) {
            for (int m = 0; m < numMatches; m++) {
                Spieler s1 = spielerInRunde.get(matches[m][0].getValue());
                Spieler s2 = spielerInRunde.get(matches[m][1].getValue());
                Spieler s3 = spielerInRunde.get(matches[m][2].getValue());
                Spieler s4 = spielerInRunde.get(matches[m][3].getValue());
                Team team1 = new Team(s1, s2);
                Team team2 = new Team(s3, s4);

                s1.getPartnerHistorie().add(s2); s2.getPartnerHistorie().add(s1);
                s3.getPartnerHistorie().add(s4); s4.getPartnerHistorie().add(s3);
                s1.addGegner(s3); s1.addGegner(s4);
                s2.addGegner(s3); s2.addGegner(s4);
                s3.addGegner(s1); s3.addGegner(s2);
                s4.addGegner(s1); s4.addGegner(s2);

                runde.addMatch(new Match(team1, team2));
            }
        } else {
            System.out.println("Keine Lösung für Runde " + rundenNummer + " gefunden.");
            return null;
        }

        return runde;
    }
}