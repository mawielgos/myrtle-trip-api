package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundPlayerStatusResponse;
import com.myrtletrip.round.dto.RoundScorecardSummaryResponse;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundQueryService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;

    public RoundQueryService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository
    ) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
    }

    @Transactional(readOnly = true)
    public RoundStatusResponse getRoundStatus(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        RoundStatusResponse dto = new RoundStatusResponse();
        dto.setRoundId(round.getId());
        dto.setTripId(round.getTrip() != null ? round.getTrip().getId() : null);
        dto.setCourseName(resolveCourseName(round));
        dto.setTeeName(resolveStandardTeeName(round));
        dto.setAlternateTeeName(resolveAlternateTeeName(round));
        dto.setFormat(round.getFormat() != null ? round.getFormat().name() : null);
        dto.setRoundDate(round.getRoundDate());
        dto.setFinalized(round.getFinalized());
        
        List<RoundPlayerStatusResponse> playerStatuses = new java.util.ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            playerStatuses.add(toPlayerStatus(scorecard));
        }
        dto.setPlayers(playerStatuses);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<RoundScorecardSummaryResponse> getRoundScorecards(Long roundId) {
        roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        List<RoundTeamPlayer> roundTeamPlayers =
                roundTeamPlayerRepository.findByRoundTeam_Round_IdOrderByRoundTeam_TeamNumberAscPlayerOrderAsc(roundId);

        Map<Long, Integer> teamNumberByPlayerId = new HashMap<>();
        Map<Long, Integer> playerOrderByPlayerId = new HashMap<>();

        for (RoundTeamPlayer roundTeamPlayer : roundTeamPlayers) {
            Long playerId = roundTeamPlayer.getPlayer().getId();
            teamNumberByPlayerId.put(playerId, roundTeamPlayer.getRoundTeam().getTeamNumber());
            playerOrderByPlayerId.put(playerId, roundTeamPlayer.getPlayerOrder());
        }

        scorecards.sort((a, b) -> {
            Integer aTeam = teamNumberByPlayerId.getOrDefault(a.getPlayer().getId(), Integer.MAX_VALUE);
            Integer bTeam = teamNumberByPlayerId.getOrDefault(b.getPlayer().getId(), Integer.MAX_VALUE);

            int teamCompare = aTeam.compareTo(bTeam);
            if (teamCompare != 0) {
                return teamCompare;
            }

            Integer aOrder = playerOrderByPlayerId.getOrDefault(a.getPlayer().getId(), Integer.MAX_VALUE);
            Integer bOrder = playerOrderByPlayerId.getOrDefault(b.getPlayer().getId(), Integer.MAX_VALUE);

            int orderCompare = aOrder.compareTo(bOrder);
            if (orderCompare != 0) {
                return orderCompare;
            }

            return a.getPlayer().getDisplayName()
                    .compareToIgnoreCase(b.getPlayer().getDisplayName());
        });

        List<RoundScorecardSummaryResponse> results = new java.util.ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            results.add(toRoundScorecardSummary(scorecard));
        }

        return results;
    }

    private RoundPlayerStatusResponse toPlayerStatus(Scorecard scorecard) {
        RoundPlayerStatusResponse dto = new RoundPlayerStatusResponse();
        dto.setScorecardId(scorecard.getId());
        dto.setPlayerId(scorecard.getPlayer().getId());
        dto.setPlayerName(scorecard.getPlayer().getDisplayName());
        dto.setUseAlternateTee(isUsingAlternateTee(scorecard));
        dto.setCourseHandicap(scorecard.getCourseHandicap());
        dto.setPlayingHandicap(scorecard.getPlayingHandicap());
        return dto;
    }

    private RoundScorecardSummaryResponse toRoundScorecardSummary(Scorecard scorecard) {
        RoundScorecardSummaryResponse dto = new RoundScorecardSummaryResponse();
        dto.setScorecardId(scorecard.getId());
        dto.setPlayerId(scorecard.getPlayer().getId());
        dto.setPlayerName(scorecard.getPlayer().getDisplayName());

        if (scorecard.getTeam() != null) {
            dto.setTeamId(scorecard.getTeam().getId());
            dto.setTeamName(scorecard.getTeam().getTeamName());
        }

        dto.setCourseHandicap(scorecard.getCourseHandicap());
        dto.setPlayingHandicap(scorecard.getPlayingHandicap());
        dto.setGrossScore(scorecard.getGrossScore());
        dto.setAdjustedGrossScore(scorecard.getAdjustedGrossScore());
        dto.setNetScore(scorecard.getNetScore());
        dto.setTeeName(resolveStandardTeeName(scorecard.getRound()));
        dto.setAlternateTeeName(resolveAlternateTeeName(scorecard.getRound()));
        dto.setUseAlternateTee(isUsingAlternateTee(scorecard));
        dto.setCurrentTeeName(resolveCurrentTeeName(scorecard));

        return dto;
    }

    private boolean isUsingAlternateTee(Scorecard scorecard) {
        if (scorecard == null || scorecard.getRound() == null) {
            return false;
        }

        RoundTee currentRoundTee = scorecard.getRoundTee();
        RoundTee alternateRoundTee = scorecard.getRound().getAlternateRoundTee();

        if (currentRoundTee == null || alternateRoundTee == null) {
            return false;
        }

        if (currentRoundTee.getId() == null || alternateRoundTee.getId() == null) {
            return false;
        }

        return currentRoundTee.getId().equals(alternateRoundTee.getId());
    }

    private String resolveCourseName(Round round) {
        if (round == null) {
            return null;
        }

        if (round.getCourse() != null && round.getCourse().getName() != null) {
            return round.getCourse().getName();
        }

        RoundTee standardRoundTee = round.getStandardRoundTee();
        if (standardRoundTee != null && standardRoundTee.getCourseName() != null) {
            return standardRoundTee.getCourseName();
        }

        RoundTee alternateRoundTee = round.getAlternateRoundTee();
        if (alternateRoundTee != null && alternateRoundTee.getCourseName() != null) {
            return alternateRoundTee.getCourseName();
        }

        return null;
    }

    private String resolveStandardTeeName(Round round) {
        if (round == null || round.getStandardRoundTee() == null) {
            return null;
        }
        return round.getStandardRoundTee().getTeeName();
    }

    private String resolveAlternateTeeName(Round round) {
        if (round == null || round.getAlternateRoundTee() == null) {
            return null;
        }
        return round.getAlternateRoundTee().getTeeName();
    }

    private String resolveCurrentTeeName(Scorecard scorecard) {
        RoundTee roundTee = scorecard.getRoundTee();
        return roundTee != null ? roundTee.getTeeName() : null;
    }
}