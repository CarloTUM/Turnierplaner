package com.example.roundrobintunier;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class TournamentSolver {

    public Runde solveRunde(int rundenNummer, List<Spieler> spielerInRunde) {
        Model model = new Model("Runde " + rundenNummer);
        int numPlayers = spielerInRunde.size();
        int numTeams = numPlayers / 4;

        // Variablen für Teams und Matches: matches[t][p] enthält den Index des Spielers in spielerInRunde
        IntVar[][] matches = new IntVar[numTeams][4];
        for (int t = 0; t < numTeams; t++) {
            for (int p = 0; p < 4; p++) {
                matches[t][p] = model.intVar("Team" + t + "_Player" + p, 0, numPlayers - 1);
            }
        }

        // Jeder Spieler darf nur einmal pro Runde auftreten
        List<IntVar> allPlayers = new ArrayList<>();
        for (int t = 0; t < numTeams; t++) {
            for (int p = 0; p < 4; p++) {
                allPlayers.add(matches[t][p]);
            }
        }
        model.allDifferent(allPlayers.toArray(new IntVar[0])).post();

        /*
         * SOFT CONSTRAINTS:
         * 1) Keine wiederholten Team-Partner (stärkere Gewichtung)
         * 2) Keine wiederholten Gegner (normale Gewichtung)
         * 3) Mixed Teams (z. B. Geschlechterverteilung – hier noch vorhanden, aber niedriger gewichtet)
         */
        List<BoolVar> teamRepeatViolations = new ArrayList<>();
        List<BoolVar> opponentRepeatViolations = new ArrayList<>();
        List<BoolVar> mixedViolations = new ArrayList<>();

        for (int t = 0; t < numTeams; t++) {
            IntVar p1 = matches[t][0];
            IntVar p2 = matches[t][1];
            IntVar p3 = matches[t][2];
            IntVar p4 = matches[t][3];

            for (int i = 0; i < numPlayers; i++) {
                Spieler spieler = spielerInRunde.get(i);

                // Wiederholte Team-Partner
                for (Spieler partner : spieler.getPartnerHistorie()) {
                    int partnerIndex = spielerInRunde.indexOf(partner);
                    if (partnerIndex != -1) {
                        BoolVar conflict1 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p1, "=", i),
                                        model.arithm(p2, "=", partnerIndex)),
                                model.arithm(conflict1, "=", 1)
                        );
                        teamRepeatViolations.add(conflict1);

                        BoolVar conflict2 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p2, "=", i),
                                        model.arithm(p1, "=", partnerIndex)),
                                model.arithm(conflict2, "=", 1)
                        );
                        teamRepeatViolations.add(conflict2);
                    }
                }

                // Wiederholte Gegner
                for (Spieler gegner : spieler.getGegnerHistorie()) {
                    int gegnerIndex = spielerInRunde.indexOf(gegner);
                    if (gegnerIndex != -1) {
                        BoolVar conflict1 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p1, "=", i),
                                        model.arithm(p3, "=", gegnerIndex)),
                                model.arithm(conflict1, "=", 1)
                        );
                        opponentRepeatViolations.add(conflict1);

                        BoolVar conflict2 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p1, "=", i),
                                        model.arithm(p4, "=", gegnerIndex)),
                                model.arithm(conflict2, "=", 1)
                        );
                        opponentRepeatViolations.add(conflict2);
                    }
                }
            }

            // Mixed Teams – Beispielhaft hier als Dummy-Constraint (z. B. könnte hier auch das Geschlecht einfließen)
            BoolVar mixed = model.boolVar();
            model.ifThen(
                    model.arithm(p1, "!=", p2),
                    model.arithm(mixed, "=", 1)
            );
            mixedViolations.add(mixed);
        }

        // Summen der Soft Constraint-Verletzungen
        IntVar teamRepeatSum = model.intVar("teamRepeatSum", 0, teamRepeatViolations.size());
        IntVar opponentRepeatSum = model.intVar("opponentRepeatSum", 0, opponentRepeatViolations.size());
        IntVar mixedSum = model.intVar("mixedSum", 0, mixedViolations.size());
        model.sum(teamRepeatViolations.toArray(new BoolVar[0]), "=", teamRepeatSum).post();
        model.sum(opponentRepeatViolations.toArray(new BoolVar[0]), "=", opponentRepeatSum).post();
        model.sum(mixedViolations.toArray(new BoolVar[0]), "=", mixedSum).post();

        /*
         * NEUER PART: Spielstärke-Balancierung
         * Ziel ist es, dass die Summe der Spielstärken der beiden Teams in einem Match möglichst gleich ist.
         */
        // Erstelle ein Array der Spielstärken für alle Spieler in dieser Runde
        int[] skillLevels = new int[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            skillLevels[i] = spielerInRunde.get(i).getSpielstaerke();
        }

        List<IntVar> skillDiffList = new ArrayList<>();
        for (int t = 0; t < numTeams; t++) {
            IntVar p1 = matches[t][0];
            IntVar p2 = matches[t][1];
            IntVar p3 = matches[t][2];
            IntVar p4 = matches[t][3];

            // Element-Constraints: Hole die Spielstärke der zugewiesenen Spieler
            IntVar p1Skill = model.intVar("p1Skill_t" + t, 1, 10);
            model.element(p1Skill, skillLevels, p1, 0).post();

            IntVar p2Skill = model.intVar("p2Skill_t" + t, 1, 10);
            model.element(p2Skill, skillLevels, p2, 0).post();

            IntVar p3Skill = model.intVar("p3Skill_t" + t, 1, 10);
            model.element(p3Skill, skillLevels, p3, 0).post();

            IntVar p4Skill = model.intVar("p4Skill_t" + t, 1, 10);
            model.element(p4Skill, skillLevels, p4, 0).post();

            // Berechne die Teamstärke (Team besteht aus 2 Spielern)
            IntVar team1Strength = model.intVar("team1Strength_t" + t, 2, 20);
            model.sum(new IntVar[]{p1Skill, p2Skill}, "=", team1Strength).post();

            IntVar team2Strength = model.intVar("team2Strength_t" + t, 2, 20);
            model.sum(new IntVar[]{p3Skill, p4Skill}, "=", team2Strength).post();

            // Berechne die absolute Differenz der Teamstärken
            IntVar diff = model.intVar("diff_t" + t, 0, 20);
            model.distance(team1Strength, team2Strength, "=", diff).post();
            skillDiffList.add(diff);
        }
        // Gesamtdifferenz über alle Matches
        IntVar skillDiffSum = model.intVar("skillDiffSum", 0, 20 * numTeams);
        model.sum(skillDiffList.toArray(new IntVar[0]), "=", skillDiffSum).post();

        /*
         * Gewichtung der einzelnen Soft Constraints:
         * - teamRepeatSum: starker Einfluss (z. B. Faktor 2)
         * - opponentRepeatSum: normaler Einfluss (Faktor 1)
         * - mixedSum: normaler Einfluss (Faktor 1)
         * - skillDiffSum: Einfluss der Spielstärke-Balance (z. B. Faktor 2)
         */
        int wTeamRepeat = 2;
        int wOpponent = 1;
        int wMixed = 1;
        int wSkillDiff = 2;

        // Gesamtziel: Minimierung der gewichteten Summe aller Verletzungen
        IntVar overallObjective = model.intVar("overallObjective", 0, 1000);
        model.scalar(
                new IntVar[]{teamRepeatSum, opponentRepeatSum, mixedSum, skillDiffSum},
                new int[]{wTeamRepeat, wOpponent, wMixed, wSkillDiff},
                "=",
                overallObjective
        ).post();

        model.setObjective(Model.MINIMIZE, overallObjective);

        // Solver starten
        Runde runde = new Runde(rundenNummer);
        if (model.getSolver().solve()) {
            for (int t = 0; t < numTeams; t++) {
                Spieler s1 = spielerInRunde.get(matches[t][0].getValue());
                Spieler s2 = spielerInRunde.get(matches[t][1].getValue());
                Spieler s3 = spielerInRunde.get(matches[t][2].getValue());
                Spieler s4 = spielerInRunde.get(matches[t][3].getValue());

                Team team1 = new Team(s1, s2);
                Team team2 = new Team(s3, s4);

                // Aktualisiere die Historie (Team-Partner und Gegner)
                s1.getPartnerHistorie().add(s2);
                s2.getPartnerHistorie().add(s1);
                s3.getPartnerHistorie().add(s4);
                s4.getPartnerHistorie().add(s3);

                s1.addGegner(s3);
                s1.addGegner(s4);
                s2.addGegner(s3);
                s2.addGegner(s4);
                s3.addGegner(s1);
                s3.addGegner(s2);
                s4.addGegner(s1);
                s4.addGegner(s2);

                Match match = new Match(team1, team2);
                runde.addMatch(match);
            }
        } else {
            System.out.println("Keine Lösung für Runde " + rundenNummer + " gefunden.");
        }

        return runde;
    }
}
