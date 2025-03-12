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

        // Variablen für Teams und Matches
        IntVar[][] matches = new IntVar[numTeams][4];
        for (int t = 0; t < numTeams; t++) {
            for (int p = 0; p < 4; p++) {
                matches[t][p] = model.intVar("Team" + t + "_Player" + p, 0, numPlayers - 1);
            }
        }

        // Hard Constraint: Jeder Spieler darf nur einmal pro Runde auftreten
        List<IntVar> allPlayers = new ArrayList<>();
        for (int t = 0; t < numTeams; t++) {
            for (int p = 0; p < 4; p++) {
                allPlayers.add(matches[t][p]);
            }
        }
        model.allDifferent(allPlayers.toArray(new IntVar[0])).post();

        /*
         * SOFT CONSTRAINTS
         *
         * 1) Nicht-wiederholte Team-Partner
         * 2) Nicht-wiederholte Gegner
         * 3) Mixed Teams
         *
         * -> Wir trennen die BoolVars in unterschiedliche Listen, um sie unterschiedlich zu gewichten
         */

        // Listen für Soft Constraint-Verletzungen
        List<BoolVar> teamRepeatViolations = new ArrayList<>();      // stärkere Gewichtung
        List<BoolVar> opponentRepeatViolations = new ArrayList<>();  // normale Gewichtung
        List<BoolVar> mixedViolations = new ArrayList<>();           // normale Gewichtung

        for (int t = 0; t < numTeams; t++) {
            IntVar p1 = matches[t][0];
            IntVar p2 = matches[t][1];
            IntVar p3 = matches[t][2];
            IntVar p4 = matches[t][3];

            for (int i = 0; i < numPlayers; i++) {
                Spieler spieler = spielerInRunde.get(i);

                // Keine wiederholten Team-Partner => stärkere Gewichtung
                for (Spieler partner : spieler.getPartnerHistorie()) {
                    int partnerIndex = spielerInRunde.indexOf(partner);
                    if (partnerIndex != -1) {
                        // Falls p1 == i und p2 == partnerIndex (oder umgekehrt),
                        // wollen wir ein BoolVar conflict = 1 erzeugen.
                        // Beispielhaft nur für p1, p2 implementiert, ggf. musst du
                        // p3, p4 ebenfalls prüfen, je nachdem wie du Teams zuordnest.

                        // conflict1: p1 und p2 bilden ein "altes" Team
                        BoolVar conflict1 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p1, "=", i),
                                        model.arithm(p2, "=", partnerIndex)),
                                model.arithm(conflict1, "=", 1)
                        );
                        teamRepeatViolations.add(conflict1);

                        // conflict2: p2 und p1 bilden ein "altes" Team
                        BoolVar conflict2 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p2, "=", i),
                                        model.arithm(p1, "=", partnerIndex)),
                                model.arithm(conflict2, "=", 1)
                        );
                        teamRepeatViolations.add(conflict2);

                        // Analog für p3, p4, etc. je nach Bedarf
                    }
                }

                // Keine wiederholten Gegner => normale Gewichtung
                for (Spieler gegner : spieler.getGegnerHistorie()) {
                    int gegnerIndex = spielerInRunde.indexOf(gegner);
                    if (gegnerIndex != -1) {
                        // Vergleichbar wie oben, aber packen wir in opponentRepeatViolations
                        // conflict1: p1 == i und p3 == gegnerIndex
                        BoolVar conflict1 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p1, "=", i),
                                        model.arithm(p3, "=", gegnerIndex)),
                                model.arithm(conflict1, "=", 1)
                        );
                        opponentRepeatViolations.add(conflict1);

                        // conflict2: p1 == i und p4 == gegnerIndex
                        BoolVar conflict2 = model.boolVar();
                        model.ifThen(
                                model.and(model.arithm(p1, "=", i),
                                        model.arithm(p4, "=", gegnerIndex)),
                                model.arithm(conflict2, "=", 1)
                        );
                        opponentRepeatViolations.add(conflict2);
                        // usw.
                    }
                }
            }

            // Mixed Teams => normale Gewichtung
            // Beispiel: wir interpretieren "Mixed" so, dass p1 != p2 (evtl. Geschlecht o.Ä.),
            // hier nur symbolisch:
            BoolVar mixed = model.boolVar();
            model.ifThen(
                    model.arithm(p1, "!=", p2),
                    model.arithm(mixed, "=", 1)
            );
            mixedViolations.add(mixed);
        }

        // *** Gewichtete Summe erstellen ***

        // 1) Summiere jede Liste einzeln
        IntVar teamRepeatSum = model.intVar("teamRepeatSum", 0, teamRepeatViolations.size());
        IntVar opponentRepeatSum = model.intVar("opponentRepeatSum", 0, opponentRepeatViolations.size());
        IntVar mixedSum = model.intVar("mixedSum", 0, mixedViolations.size());

        model.sum(teamRepeatViolations.toArray(new BoolVar[0]), "=", teamRepeatSum).post();
        model.sum(opponentRepeatViolations.toArray(new BoolVar[0]), "=", opponentRepeatSum).post();
        model.sum(mixedViolations.toArray(new BoolVar[0]), "=", mixedSum).post();

        /*
         * 2) Lege Gewichte fest:
         *    teamRepeatSum soll stärker ins Gewicht fallen, z.B. Faktor 2 (oder 3).
         */
        int wTeamRepeat = 2;  // Beispielgewicht
        int wOpponent = 1;    // normales Gewicht
        int wMixed = 1;       // normales Gewicht

        // Erstelle eine Variable totalViolations, indem wir die gewichteten Summen zusammenrechnen.
        // -> teamRepeat * 2 + opponentRepeat * 1 + mixed * 1
        IntVar totalViolations = model.intVar("totalViolations", 0, 9999);
        model.scalar(
                new IntVar[]{teamRepeatSum, opponentRepeatSum, mixedSum},
                new int[]{wTeamRepeat, wOpponent, wMixed},
                "=",
                totalViolations
        ).post();

        // Als letztes objektivieren wir diese totalViolations
        model.setObjective(Model.MINIMIZE, totalViolations);

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

                // Historie aktualisieren
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
