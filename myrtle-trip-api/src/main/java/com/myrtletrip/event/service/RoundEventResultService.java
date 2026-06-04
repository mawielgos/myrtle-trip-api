package com.myrtletrip.event.service;

import com.myrtletrip.event.dto.IndividualEventResult;
import com.myrtletrip.event.dto.RoundEventResultResponse;
import com.myrtletrip.event.dto.RoundEventSnapshotRow;
import com.myrtletrip.event.dto.RoundEventsResultResponse;
import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.games.service.RoundGameScoringService;
import com.myrtletrip.games.service.RoundScoringDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoundEventResultService {

    private final RoundEventService roundEventService;
    private final RoundScoringDataService roundScoringDataService;
    private final RoundGameScoringService roundGameScoringService;

    public RoundEventResultService(RoundEventService roundEventService,
                                   RoundScoringDataService roundScoringDataService,
                                   RoundGameScoringService roundGameScoringService) {
        this.roundEventService = roundEventService;
        this.roundScoringDataService = roundScoringDataService;
        this.roundGameScoringService = roundGameScoringService;
    }

    @Transactional(readOnly = true)
    public RoundEventsResultResponse getRoundEventResults(Long roundId) {
        RoundEventsResultResponse response = new RoundEventsResultResponse();
        response.setRoundId(roundId);

        List<RoundEvent> events = roundEventService.findActiveEventsForRound(roundId);
        RoundScoringData data = null;


        for (RoundEvent event : events) {
            RoundEventResultResponse eventResult;
            if (event.getEventType() != null && event.getEventType().isIndividualEvent()) {
                if (data == null) {
                    data = roundScoringDataService.build(event.getRound());
                }
                eventResult = scoreIndividualEvent(event, data);
            } else {
                RoundGameResult teamResult = roundGameScoringService.getRoundResult(roundId, event.getEventType());
                eventResult = scoreTeamEvent(event, teamResult);
            }

            response.getEvents().add(eventResult);
            RoundEventSnapshotRow snapshotRow = buildSnapshotRow(eventResult);
            if (snapshotRow != null) {
                response.getSnapshotRows().add(snapshotRow);
            }
        }

        return response;
    }

    private RoundEventResultResponse scoreIndividualEvent(RoundEvent event, RoundScoringData data) {
        RoundEventResultResponse response = baseResponse(event);
        response.setResultKind("INDIVIDUAL");

        List<IndividualEventResult> results = new ArrayList<>();
        for (TeamScoringData team : data.getTeams()) {
            for (PlayerScoringData player : team.getPlayers()) {
                IndividualEventResult result = buildIndividualResult(player);
                results.add(result);
            }
        }

        sortIndividualResults(event.getEventType(), results);
        assignRanks(event.getEventType(), results);
        response.setIndividualResults(results);
        return response;
    }

    private IndividualEventResult buildIndividualResult(PlayerScoringData player) {
        IndividualEventResult result = new IndividualEventResult();
        result.setPlayerId(player.getPlayerId());
        result.setPlayerName(player.getPlayerName());
        result.setGrossTotal(totalGross(player));
        result.setNetTotal(totalNet(player));
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

    private void sortIndividualResults(RoundEventType eventType, List<IndividualEventResult> results) {
        results.sort(new Comparator<IndividualEventResult>() {
            @Override
            public int compare(IndividualEventResult a, IndividualEventResult b) {
                Integer aScore = scoreForEvent(eventType, a);
                Integer bScore = scoreForEvent(eventType, b);

                if (aScore == null && bScore == null) {
                    return compareNames(a.getPlayerName(), b.getPlayerName());
                }
                if (aScore == null) {
                    return 1;
                }
                if (bScore == null) {
                    return -1;
                }

                int scoreCompare = Integer.compare(aScore, bScore);
                if (scoreCompare != 0) {
                    return scoreCompare;
                }

                int secondaryCompare = compareSecondaryScore(eventType, a, b);
                if (secondaryCompare != 0) {
                    return secondaryCompare;
                }

                return compareNames(a.getPlayerName(), b.getPlayerName());
            }
        });
    }

    private void assignRanks(RoundEventType eventType, List<IndividualEventResult> results) {
        Integer previousScore = null;
        int previousRank = 0;
        for (int i = 0; i < results.size(); i++) {
            IndividualEventResult result = results.get(i);
            Integer score = scoreForEvent(eventType, result);
            if (score == null) {
                result.setRank(null);
                continue;
            }
            int rank = i + 1;
            if (previousScore != null && previousScore.equals(score)) {
                rank = previousRank;
            }
            result.setRank(rank);
            previousScore = score;
            previousRank = rank;
        }
    }

    private Integer scoreForEvent(RoundEventType eventType, IndividualEventResult result) {
        if (eventType == RoundEventType.INDIVIDUAL_LOW_GROSS) {
            return result.getGrossTotal();
        }
        return result.getNetTotal();
    }

    private int compareSecondaryScore(RoundEventType eventType, IndividualEventResult a, IndividualEventResult b) {
        Integer aScore = eventType == RoundEventType.INDIVIDUAL_LOW_GROSS ? a.getNetTotal() : a.getGrossTotal();
        Integer bScore = eventType == RoundEventType.INDIVIDUAL_LOW_GROSS ? b.getNetTotal() : b.getGrossTotal();
        if (aScore == null && bScore == null) {
            return 0;
        }
        if (aScore == null) {
            return 1;
        }
        if (bScore == null) {
            return -1;
        }
        return Integer.compare(aScore, bScore);
    }

    private int compareNames(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareToIgnoreCase(b);
    }

    private RoundEventResultResponse scoreTeamEvent(RoundEvent event, RoundGameResult legacyTeamResult) {
        RoundEventResultResponse response = baseResponse(event);
        response.setResultKind("TEAM");
        response.setTeamResults(legacyTeamResult.getTeams());
        return response;
    }

    private RoundEventSnapshotRow buildSnapshotRow(RoundEventResultResponse eventResult) {
        if (eventResult == null) {
            return null;
        }

        RoundEventSnapshotRow row = new RoundEventSnapshotRow();
        row.setEventId(eventResult.getEventId());
        row.setEventType(eventResult.getEventType());
        row.setEventName(eventResult.getEventName());
        row.setResultKind(eventResult.getResultKind());

        if ("INDIVIDUAL".equals(eventResult.getResultKind())) {
            if (eventResult.getIndividualResults() == null || eventResult.getIndividualResults().isEmpty()) {
                return null;
            }
            IndividualEventResult winner = eventResult.getIndividualResults().get(0);
            row.setWinnerName(winner.getPlayerName());
            row.setWinningTotal(scoreForEvent(eventResult.getEventType(), winner));
            row.setRank(winner.getRank());
            row.setTied(hasIndividualTieForRank(winner, eventResult.getIndividualResults()));
            return row;
        }

        if (eventResult.getTeamResults() == null || eventResult.getTeamResults().isEmpty()) {
            return null;
        }
        TeamGameResult winner = eventResult.getTeamResults().get(0);
        row.setWinnerName(winner.getTeamName());
        row.setWinningTotal(scoreForTeamEvent(eventResult.getEventType(), winner));
        row.setRank(winner.getPlacement());
        row.setTied(hasTeamTieForRank(winner, eventResult.getTeamResults()));
        return row;
    }

    private Boolean hasIndividualTieForRank(IndividualEventResult winner, List<IndividualEventResult> results) {
        if (winner == null || winner.getRank() == null || results == null) {
            return false;
        }
        int count = 0;
        for (IndividualEventResult result : results) {
            if (winner.getRank().equals(result.getRank())) {
                count++;
            }
        }
        return count > 1;
    }

    private Boolean hasTeamTieForRank(TeamGameResult winner, List<TeamGameResult> results) {
        if (winner == null || winner.getPlacement() == null || results == null) {
            return false;
        }
        int count = 0;
        for (TeamGameResult result : results) {
            if (winner.getPlacement().equals(result.getPlacement())) {
                count++;
            }
        }
        return count > 1;
    }

    private Integer scoreForTeamEvent(RoundEventType eventType, TeamGameResult result) {
        if (result == null) {
            return null;
        }
        if (eventType == RoundEventType.TEAM_SCRAMBLE) {
            return result.getTotalGross() != null ? result.getTotalGross() : result.getTotalNet();
        }
        return result.getTotalNet() != null ? result.getTotalNet() : result.getTotalGross();
    }

    private RoundEventResultResponse baseResponse(RoundEvent event) {
        RoundEventResultResponse response = new RoundEventResultResponse();
        response.setEventId(event.getId());
        response.setRoundId(event.getRound() == null ? null : event.getRound().getId());
        response.setEventType(event.getEventType());
        response.setEventName(event.getEventName());
        response.setEventOrder(event.getEventOrder());
        return response;
    }
}
