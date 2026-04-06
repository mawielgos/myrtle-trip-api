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

        int place = 1;
        for (TeamGameResult team : sorted) {
            team.setPlacement(place++);
        }
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
}
