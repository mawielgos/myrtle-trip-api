package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundPlayerStatusResponse;
import com.myrtletrip.round.dto.RoundScorecardSummaryResponse;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundQueryService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundHandicapService roundHandicapService;

    public RoundQueryService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundTeeResolver roundTeeResolver,
            RoundHandicapService roundHandicapService
    ) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundHandicapService = roundHandicapService;
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
        dto.setFormat(round.getFormat() != null ? round.getFormat().name() : null);
        dto.setScrambleTeamSize(resolveScrambleTeamSize(round));
        dto.setRoundDate(round.getRoundDate());
        dto.setFinalized(round.getFinalized());

        Trip trip = round.getTrip();
        boolean tripComplete = trip != null && TripStatus.COMPLETE.equals(trip.getStatus());
        boolean tripCorrectionMode = trip != null && Boolean.TRUE.equals(trip.getCorrectionMode());
        dto.setTripStatus(trip != null && trip.getStatus() != null ? trip.getStatus().name() : null);
        dto.setTripCorrectionMode(tripCorrectionMode);
        dto.setTripLocked(tripComplete && !tripCorrectionMode);
        dto.setEditable(!tripComplete || tripCorrectionMode);

        List<RoundPlayerStatusResponse> playerStatuses = new java.util.ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            playerStatuses.add(toPlayerStatus(scorecard));
        }
        dto.setPlayers(playerStatuses);

        return dto;
    }

    private Integer resolveScrambleTeamSize(Round round) {
        if (round == null || round.getFormat() != com.myrtletrip.round.model.RoundFormat.TEAM_SCRAMBLE) {
            return 4;
        }
        Integer size = round.getScrambleTeamSize();
        return size == null ? 4 : size;
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
        populateHandicapSnapshot(dto, scorecard);
        dto.setGender(normalizeGender(player.getGender()));

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
        populateHandicapSnapshot(dto, scorecard);

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
        dto.setCurrentTeeName(resolvedTee != null ? resolvedTee.getTeeName() : null);
        dto.setRoundTeeId(resolvedTee != null ? resolvedTee.getId() : null);

        return dto;
    }

    private void populateHandicapSnapshot(RoundPlayerStatusResponse dto, Scorecard scorecard) {
        if (dto == null || scorecard == null) {
            return;
        }

        Round round = scorecard.getRound();
        if (round == null || round.getRoundDate() == null || round.getTrip() == null) {
            return;
        }

        dto.setHandicapAsOfDate(round.getRoundDate());
        dto.setHandicapMethod(scorecard.getPlayer() != null ? scorecard.getPlayer().getHandicapMethod() : null);
        dto.setHandicapLabel(buildHandicapLabel(scorecard));
        dto.setTripIndex(calculateTripIndexSafely(scorecard));
    }

    private void populateHandicapSnapshot(RoundScorecardSummaryResponse dto, Scorecard scorecard) {
        if (dto == null || scorecard == null) {
            return;
        }

        Round round = scorecard.getRound();
        if (round == null || round.getRoundDate() == null || round.getTrip() == null) {
            return;
        }

        dto.setHandicapAsOfDate(round.getRoundDate());
        dto.setHandicapMethod(scorecard.getPlayer() != null ? scorecard.getPlayer().getHandicapMethod() : null);
        dto.setHandicapLabel(buildHandicapLabel(scorecard));
        dto.setTripIndex(calculateTripIndexSafely(scorecard));
    }

    private BigDecimal calculateTripIndexSafely(Scorecard scorecard) {
        try {
            Round round = scorecard.getRound();
            if (round == null || round.getTrip() == null || round.getTrip().getTripCode() == null) {
                return null;
            }
            return roundHandicapService.calculateTripIndex(scorecard, round.getTrip().getTripCode());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String buildHandicapLabel(Scorecard scorecard) {
        if (scorecard == null || scorecard.getRound() == null || scorecard.getRound().getRoundDate() == null) {
            return null;
        }

        String method = scorecard.getPlayer() != null ? scorecard.getPlayer().getHandicapMethod() : null;
        String displayMethod = displayHandicapMethod(method);
        LocalDate roundDate = scorecard.getRound().getRoundDate();
        return displayMethod + " index as of " + roundDate + " (same-day scores excluded)";
    }

    private String displayHandicapMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return "Trip";
        }

        String normalized = method.trim().toUpperCase();
        if ("MYRTLE_BEACH".equals(normalized) || "DB_SCORE_HISTORY".equals(normalized)) {
            return "DB Score History";
        }
        if ("GHIN".equals(normalized)) {
            return "GHIN";
        }

        return method.trim();
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