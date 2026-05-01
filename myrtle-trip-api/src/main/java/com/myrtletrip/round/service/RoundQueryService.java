package com.myrtletrip.round.service;

import com.myrtletrip.player.entity.Player;
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
    private final RoundTeeResolver roundTeeResolver;

    public RoundQueryService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundTeeResolver roundTeeResolver
    ) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundTeeResolver = roundTeeResolver;
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
        dto.setTeeName(resolveDefaultTeeName(round));
        dto.setAlternateTeeName(null);
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

        Player player = scorecard.getPlayer();
        RoundTee resolvedTee = roundTeeResolver.resolve(scorecard);

        dto.setScorecardId(scorecard.getId());
        dto.setPlayerId(player.getId());
        dto.setPlayerName(player.getDisplayName());
        dto.setGender(normalizeGender(player.getGender()));
        dto.setUseAlternateTee(false);

        dto.setRoundTeeId(resolvedTee != null ? resolvedTee.getId() : null);
        dto.setRoundTeeName(resolvedTee != null ? resolvedTee.getTeeName() : null);

        dto.setCourseHandicap(scorecard.getCourseHandicap());
        dto.setPlayingHandicap(scorecard.getPlayingHandicap());

        return dto;
    }

    private RoundScorecardSummaryResponse toRoundScorecardSummary(Scorecard scorecard) {
        RoundScorecardSummaryResponse dto = new RoundScorecardSummaryResponse();

        RoundTee resolvedTee = roundTeeResolver.resolve(scorecard);

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

        dto.setTeeName(resolvedTee != null ? resolvedTee.getTeeName() : null);
        dto.setAlternateTeeName(null);
        dto.setUseAlternateTee(false);
        dto.setCurrentTeeName(resolvedTee != null ? resolvedTee.getTeeName() : null);

        return dto;
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return "M";
        }

        String normalized = gender.trim().toUpperCase();

        if ("F".equals(normalized)
                || "FEMALE".equals(normalized)
                || "W".equals(normalized)
                || "WOMAN".equals(normalized)) {
            return "F";
        }

        return "M";
    }

    private String resolveCourseName(Round round) {
        if (round == null) {
            return null;
        }

        if (round.getCourse() != null && round.getCourse().getName() != null) {
            return round.getCourse().getName();
        }

        RoundTee defaultRoundTee = round.getDefaultRoundTee();
        if (defaultRoundTee != null && defaultRoundTee.getCourseName() != null) {
            return defaultRoundTee.getCourseName();
        }

        return null;
    }

    private String resolveDefaultTeeName(Round round) {
        if (round == null || round.getDefaultRoundTee() == null) {
            return null;
        }

        return round.getDefaultRoundTee().getTeeName();
    }
}