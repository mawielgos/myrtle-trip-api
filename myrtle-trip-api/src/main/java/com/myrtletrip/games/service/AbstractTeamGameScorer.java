package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.HoleGameResult;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamHoleScoringData;
import com.myrtletrip.games.model.TeamScoringData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class AbstractTeamGameScorer implements RoundGameScorer {

    protected RoundGameResult createBaseResult(RoundScoringData data) {
        RoundGameResult result = new RoundGameResult();
        result.setRoundId(data.getRoundId());
        result.setFormat(data.getFormat());

        List<TeamGameResult> teams = new ArrayList<>();
        for (TeamScoringData team : data.getTeams()) {
            TeamGameResult tr = new TeamGameResult();
            tr.setTeamId(team.getTeamId());
            tr.setTeamName(team.getTeamName());
            teams.add(tr);
        }
        result.setTeams(teams);
        return result;
    }

    protected void assignPlacementsByPointsThenNet(RoundGameResult result) {
        List<TeamGameResult> sorted = new ArrayList<>(result.getTeams());
        sorted.sort(
                Comparator.comparing(TeamGameResult::getTotalPoints, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TeamGameResult::getTotalNet, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TeamGameResult::getTeamName, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        Integer previousPoints = null;
        Integer previousNet = null;
        Integer previousPlacement = null;

        for (int i = 0; i < sorted.size(); i++) {
            TeamGameResult team = sorted.get(i);
            int currentPlacement = i + 1;

            if (previousPoints != null
                    && previousNet != null
                    && Objects.equals(previousPoints, team.getTotalPoints())
                    && Objects.equals(previousNet, team.getTotalNet())) {
                team.setPlacement(previousPlacement);
            } else {
                team.setPlacement(currentPlacement);
                previousPlacement = currentPlacement;
                previousPoints = team.getTotalPoints();
                previousNet = team.getTotalNet();
            }
        }
    }

    protected void assignPlacementsByLowNet(RoundGameResult result) {
        List<TeamGameResult> sorted = new ArrayList<>(result.getTeams());
        sorted.sort(
                Comparator.comparing(TeamGameResult::getTotalNet, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TeamGameResult::getTeamName, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        Integer previousNet = null;
        Integer previousPlacement = null;

        for (int i = 0; i < sorted.size(); i++) {
            TeamGameResult team = sorted.get(i);
            int currentPlacement = i + 1;

            if (previousNet != null && Objects.equals(previousNet, team.getTotalNet())) {
                team.setPlacement(previousPlacement);
            } else {
                team.setPlacement(currentPlacement);
                previousPlacement = currentPlacement;
                previousNet = team.getTotalNet();
            }
        }
    }

    protected void assignPlacementsByLowGross(RoundGameResult result) {
        List<TeamGameResult> sorted = new ArrayList<>(result.getTeams());
        sorted.sort(
                Comparator.comparing(TeamGameResult::getTotalGross, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TeamGameResult::getTeamName, Comparator.nullsLast(String::compareToIgnoreCase))
        );

        Integer previousGross = null;
        Integer previousPlacement = null;

        for (int i = 0; i < sorted.size(); i++) {
            TeamGameResult team = sorted.get(i);
            int currentPlacement = i + 1;

            if (previousGross != null && Objects.equals(previousGross, team.getTotalGross())) {
                team.setPlacement(previousPlacement);
            } else {
                team.setPlacement(currentPlacement);
                previousPlacement = currentPlacement;
                previousGross = team.getTotalGross();
            }
        }
    }

    protected void assignMatchPointsTwoTeam(RoundGameResult result) {
        if (result.getTeams().size() != 2) {
            return;
        }

        TeamGameResult a = result.getTeams().get(0);
        TeamGameResult b = result.getTeams().get(1);

        int aPoints = 0;
        int bPoints = 0;

        for (int i = 0; i < a.getHoleResults().size(); i++) {
            HoleGameResult ah = a.getHoleResults().get(i);
            HoleGameResult bh = b.getHoleResults().get(i);

            if (ah.getNetScore() < bh.getNetScore()) {
                ah.setPoints(1);
                bh.setPoints(0);
                aPoints++;
            } else if (bh.getNetScore() < ah.getNetScore()) {
                ah.setPoints(0);
                bh.setPoints(1);
                bPoints++;
            } else {
                ah.setPoints(0);
                bh.setPoints(0);
            }
        }

        a.setTotalPoints(aPoints);
        b.setTotalPoints(bPoints);
    }

    protected PlayerHoleScoringData requirePlayerHole(PlayerScoringData player, int holeNumber) {
        return player.getHoles().stream()
                .filter(h -> h.getHoleNumber() == holeNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing player hole score for playerId=" + player.getPlayerId() + ", hole=" + holeNumber));
    }

    protected TeamHoleScoringData requireTeamHole(TeamScoringData team, int holeNumber) {
        return team.getScrambleHoleScores().stream()
                .filter(h -> h.getHoleNumber() == holeNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing team hole score for teamId=" + team.getTeamId() + ", hole=" + holeNumber));
    }

    protected TeamGameResult findTeamResult(RoundGameResult result, Long teamId) {
        Optional<TeamGameResult> team = result.getTeams().stream()
                .filter(t -> t.getTeamId().equals(teamId))
                .findFirst();

        return team.orElseThrow(() -> new IllegalStateException("Missing TeamGameResult for teamId=" + teamId));
    }

    protected HoleGameResult addHoleResult(TeamGameResult teamResult, int holeNumber, int gross, int net, int points) {
        HoleGameResult holeResult = new HoleGameResult();
        holeResult.setHoleNumber(holeNumber);
        holeResult.setGrossScore(gross);
        holeResult.setNetScore(net);
        holeResult.setPoints(points);

        teamResult.getHoleResults().add(holeResult);
        teamResult.setTotalGross(teamResult.getTotalGross() + gross);
        teamResult.setTotalNet(teamResult.getTotalNet() + net);
        teamResult.setTotalPoints(teamResult.getTotalPoints() + points);

        return holeResult;
    }

    protected List<Integer> sortedHoleNets(TeamScoringData team, int holeNumber) {
        return team.getPlayers().stream()
                .map(p -> requirePlayerHole(p, holeNumber).getNet())
                .sorted()
                .toList();
    }

    protected List<Integer> sortedHoleGrosses(TeamScoringData team, int holeNumber) {
        return team.getPlayers().stream()
                .map(p -> requirePlayerHole(p, holeNumber).getGross())
                .sorted()
                .toList();
    }

    protected int sumLowest(List<Integer> values, int count) {
        if (values.size() < count) {
            throw new IllegalStateException("Not enough scores to sum lowest " + count);
        }
        return values.stream().limit(count).mapToInt(Integer::intValue).sum();
    }

    protected void requireExactPlayerCount(TeamScoringData team, int expected, String gameName) {
        if (team.getPlayers().size() != expected) {
            throw new IllegalStateException(
                    gameName + " requires exactly " + expected + " players per team. "
                            + "teamId=" + team.getTeamId()
                            + ", actual=" + team.getPlayers().size()
            );
        }
    }

    protected void requireMinimumPlayerCount(TeamScoringData team, int minimum, String gameName) {
        if (team.getPlayers().size() < minimum) {
            throw new IllegalStateException(
                    gameName + " requires at least " + minimum + " players per team. "
                            + "teamId=" + team.getTeamId()
                            + ", actual=" + team.getPlayers().size()
            );
        }
    }
}