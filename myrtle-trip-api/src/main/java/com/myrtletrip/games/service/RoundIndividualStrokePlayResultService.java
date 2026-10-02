package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.HoleGameResult;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoundIndividualStrokePlayResultService {

    public RoundGameResult createResult(RoundScoringData data) {
        RoundGameResult result = new RoundGameResult();
        result.setRoundId(data.getRoundId());
        result.setFormat(data.getFormat());

        List<TeamGameResult> playerResults = new ArrayList<>();
        for (TeamScoringData group : data.getTeams()) {
            for (PlayerScoringData player : group.getPlayers()) {
                TeamGameResult playerResult = new TeamGameResult();
                playerResult.setTeamId(player.getPlayerId());
                playerResult.setTeamName(player.getPlayerName());
                playerResult.setTotalGross(totalGross(player));
                playerResult.setTotalNet(totalNet(player));

                for (PlayerHoleScoringData playerHole : player.getHoles()) {
                    HoleGameResult holeResult = new HoleGameResult();
                    holeResult.setHoleNumber(playerHole.getHoleNumber());
                    holeResult.setGrossScore(playerHole.getGross());
                    holeResult.setNetScore(playerHole.getNet());
                    holeResult.setPoints(0);
                    playerResult.getHoleResults().add(holeResult);
                }

                playerResults.add(playerResult);
            }
        }

        playerResults.sort(new Comparator<TeamGameResult>() {
            @Override
            public int compare(TeamGameResult a, TeamGameResult b) {
                Integer aNet = a.getTotalNet();
                Integer bNet = b.getTotalNet();

                if (aNet == null && bNet == null) {
                    return compareNames(a.getTeamName(), b.getTeamName());
                }
                if (aNet == null) {
                    return 1;
                }
                if (bNet == null) {
                    return -1;
                }

                int netCompare = Integer.compare(aNet, bNet);
                if (netCompare != 0) {
                    return netCompare;
                }

                Integer aGross = a.getTotalGross();
                Integer bGross = b.getTotalGross();
                if (aGross == null && bGross == null) {
                    return compareNames(a.getTeamName(), b.getTeamName());
                }
                if (aGross == null) {
                    return 1;
                }
                if (bGross == null) {
                    return -1;
                }

                int grossCompare = Integer.compare(aGross, bGross);
                if (grossCompare != 0) {
                    return grossCompare;
                }

                return compareNames(a.getTeamName(), b.getTeamName());
            }
        });

        assignPlacements(playerResults);
        result.setTeams(playerResults);
        return result;
    }

    private Integer totalGross(PlayerScoringData player) {
        int total = 0;
        for (PlayerHoleScoringData hole : player.getHoles()) {
            if (hole.getGross() == null) {
                return null;
            }
            total += hole.getGross();
        }
        return total;
    }

    private Integer totalNet(PlayerScoringData player) {
        int total = 0;
        for (PlayerHoleScoringData hole : player.getHoles()) {
            if (hole.getNet() == null) {
                return null;
            }
            total += hole.getNet();
        }
        return total;
    }

    private void assignPlacements(List<TeamGameResult> playerResults) {
        Integer previousNet = null;
        int previousPlacement = 0;

        for (int i = 0; i < playerResults.size(); i++) {
            TeamGameResult playerResult = playerResults.get(i);
            Integer net = playerResult.getTotalNet();

            if (net == null) {
                playerResult.setPlacement(null);
                continue;
            }

            int placement = i + 1;
            if (previousNet != null && previousNet.equals(net)) {
                placement = previousPlacement;
            }

            playerResult.setPlacement(placement);
            previousNet = net;
            previousPlacement = placement;
        }
    }

    private int compareNames(String a, String b) {
        String left = a == null ? "" : a;
        String right = b == null ? "" : b;
        return String.CASE_INSENSITIVE_ORDER.compare(left, right);
    }
}
