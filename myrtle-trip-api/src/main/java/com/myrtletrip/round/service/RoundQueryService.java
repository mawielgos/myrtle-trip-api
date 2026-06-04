package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundPlayerStatusResponse;
import com.myrtletrip.round.dto.RoundScorecardSummaryResponse;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
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
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundHandicapService roundHandicapService;
    private final RoundCapabilityService roundCapabilityService;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundQueryService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundGroupRepository roundGroupRepository,
            RoundTeeResolver roundTeeResolver,
            RoundHandicapService roundHandicapService,
            RoundCapabilityService roundCapabilityService,
            RoundEventCapabilityService roundEventCapabilityService
    ) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundHandicapService = roundHandicapService;
        this.roundCapabilityService = roundCapabilityService;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    @Transactional(readOnly = true)
    public RoundStatusResponse getRoundStatus(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        List<RoundTeamPlayer> roundTeamPlayers =
                roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(roundId);
        List<RoundGroup> roundGroups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);

        Map<Long, RoundTeamPlayer> roundTeamPlayerByPlayerId = buildRoundTeamPlayerByPlayerId(roundTeamPlayers);
        Map<Long, PlayerGroupOrder> groupOrderByPlayerId = buildGroupOrderByPlayerId(roundGroups);

        RoundStatusResponse dto = new RoundStatusResponse();
        dto.setRoundId(round.getId());
        dto.setTripId(round.getTrip() != null ? round.getTrip().getId() : null);
        dto.setCourseName(resolveCourseName(round));
        dto.setTeeName(resolveDefaultTeeName(round));
        dto.setFormat(round.getFormat() != null ? round.getFormat().name() : null);
        dto.setScrambleTeamSize(resolveScrambleTeamSize(round));
        dto.setScrambleScoreEntryMode(resolveScrambleScoreEntryMode(round));
        dto.setRoundDate(round.getRoundDate());
        dto.setFinalized(round.getFinalized());

        Trip trip = round.getTrip();
        boolean tripComplete = trip != null && TripStatus.COMPLETE.equals(trip.getStatus());
        boolean tripCorrectionMode = trip != null && Boolean.TRUE.equals(trip.getCorrectionMode());
        dto.setTripStatus(trip != null && trip.getStatus() != null ? trip.getStatus().name() : null);
        dto.setTripCorrectionMode(tripCorrectionMode);
        dto.setTripLocked(tripComplete && !tripCorrectionMode);
        dto.setEditable(!tripComplete || tripCorrectionMode);
        dto.setCapabilities(roundCapabilityService.build(round));

        scorecards.sort((a, b) -> compareScorecardsByGroupOrder(a, b, roundTeamPlayerByPlayerId, groupOrderByPlayerId));

        List<RoundPlayerStatusResponse> playerStatuses = new java.util.ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            Long playerId = scorecard.getPlayer() != null ? scorecard.getPlayer().getId() : null;
            RoundTeamPlayer roundTeamPlayer = playerId != null ? roundTeamPlayerByPlayerId.get(playerId) : null;
            PlayerGroupOrder groupOrder = playerId != null ? groupOrderByPlayerId.get(playerId) : null;
            playerStatuses.add(toPlayerStatus(scorecard, roundTeamPlayer, groupOrder));
        }
        dto.setPlayers(playerStatuses);

        return dto;
    }

    private Integer resolveScrambleTeamSize(Round round) {
        if (!roundEventCapabilityService.isScrambleRound(round)) {
            return 4;
        }
        Integer size = round.getScrambleTeamSize();
        return size == null ? 4 : size;
    }

    private String resolveScrambleScoreEntryMode(Round round) {
        if (!roundEventCapabilityService.isScrambleRound(round)) {
            return null;
        }
        String mode = round.getScrambleScoreEntryMode();
        if (mode == null || mode.trim().isEmpty()) {
            return "TOTAL";
        }
        String normalized = mode.trim().toUpperCase();
        if ("HOLES".equals(normalized)) {
            return "HOLES";
        }
        return "TOTAL";
    }

    @Transactional(readOnly = true)
    public List<RoundScorecardSummaryResponse> getRoundScorecards(Long roundId) {
        roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        List<RoundTeamPlayer> roundTeamPlayers =
                roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(roundId);
        List<RoundGroup> roundGroups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);

        Map<Long, RoundTeamPlayer> roundTeamPlayerByPlayerId = buildRoundTeamPlayerByPlayerId(roundTeamPlayers);
        Map<Long, PlayerGroupOrder> groupOrderByPlayerId = buildGroupOrderByPlayerId(roundGroups);

        scorecards.sort((a, b) -> compareScorecardsByGroupOrder(a, b, roundTeamPlayerByPlayerId, groupOrderByPlayerId));

        List<RoundScorecardSummaryResponse> results = new java.util.ArrayList<>();
        for (Scorecard scorecard : scorecards) {
            Long playerId = scorecard.getPlayer() != null ? scorecard.getPlayer().getId() : null;
            RoundTeamPlayer roundTeamPlayer = playerId != null ? roundTeamPlayerByPlayerId.get(playerId) : null;
            PlayerGroupOrder groupOrder = playerId != null ? groupOrderByPlayerId.get(playerId) : null;
            results.add(toRoundScorecardSummary(scorecard, roundTeamPlayer, groupOrder));
        }

        return results;
    }

    private Map<Long, RoundTeamPlayer> buildRoundTeamPlayerByPlayerId(List<RoundTeamPlayer> roundTeamPlayers) {
        Map<Long, RoundTeamPlayer> roundTeamPlayerByPlayerId = new HashMap<>();

        for (RoundTeamPlayer roundTeamPlayer : roundTeamPlayers) {
            if (roundTeamPlayer.getPlayer() == null || roundTeamPlayer.getPlayer().getId() == null) {
                continue;
            }

            roundTeamPlayerByPlayerId.put(roundTeamPlayer.getPlayer().getId(), roundTeamPlayer);
        }

        return roundTeamPlayerByPlayerId;
    }


    private Map<Long, PlayerGroupOrder> buildGroupOrderByPlayerId(List<RoundGroup> groups) {
        Map<Long, PlayerGroupOrder> groupOrderByPlayerId = new HashMap<>();

        if (groups == null) {
            return groupOrderByPlayerId;
        }

        for (RoundGroup group : groups) {
            if (group == null || group.getPlayers() == null) {
                continue;
            }

            for (RoundGroupPlayer groupPlayer : group.getPlayers()) {
                if (groupPlayer == null || groupPlayer.getPlayer() == null || groupPlayer.getPlayer().getId() == null) {
                    continue;
                }

                PlayerGroupOrder order = new PlayerGroupOrder();
                order.groupId = group.getId();
                order.groupNumber = group.getGroupNumber();
                order.groupName = group.getGroupNumber() == null ? "Group" : "Group " + group.getGroupNumber();
                order.playerOrder = groupPlayer.getSeatOrder();

                groupOrderByPlayerId.put(groupPlayer.getPlayer().getId(), order);
            }
        }

        return groupOrderByPlayerId;
    }

    private int compareScorecardsByGroupOrder(
            Scorecard a,
            Scorecard b,
            Map<Long, RoundTeamPlayer> roundTeamPlayerByPlayerId,
            Map<Long, PlayerGroupOrder> groupOrderByPlayerId
    ) {
        Long aPlayerId = a.getPlayer() != null ? a.getPlayer().getId() : null;
        Long bPlayerId = b.getPlayer() != null ? b.getPlayer().getId() : null;

        RoundTeamPlayer aAssignment = aPlayerId != null ? roundTeamPlayerByPlayerId.get(aPlayerId) : null;
        RoundTeamPlayer bAssignment = bPlayerId != null ? roundTeamPlayerByPlayerId.get(bPlayerId) : null;
        PlayerGroupOrder aGroupOrder = aPlayerId != null ? groupOrderByPlayerId.get(aPlayerId) : null;
        PlayerGroupOrder bGroupOrder = bPlayerId != null ? groupOrderByPlayerId.get(bPlayerId) : null;

        Integer aTeamNumber = resolveTeamNumber(aAssignment, aGroupOrder);
        Integer bTeamNumber = resolveTeamNumber(bAssignment, bGroupOrder);

        int teamCompare = aTeamNumber.compareTo(bTeamNumber);
        if (teamCompare != 0) {
            return teamCompare;
        }

        Integer aPlayerOrder = resolvePlayerOrder(aAssignment, aGroupOrder);
        Integer bPlayerOrder = resolvePlayerOrder(bAssignment, bGroupOrder);

        int orderCompare = aPlayerOrder.compareTo(bPlayerOrder);
        if (orderCompare != 0) {
            return orderCompare;
        }

        String aName = a.getPlayer() != null && a.getPlayer().getDisplayName() != null
                ? a.getPlayer().getDisplayName()
                : "";
        String bName = b.getPlayer() != null && b.getPlayer().getDisplayName() != null
                ? b.getPlayer().getDisplayName()
                : "";

        return aName.compareToIgnoreCase(bName);
    }

    private Integer resolveTeamNumber(RoundTeamPlayer roundTeamPlayer, PlayerGroupOrder groupOrder) {
        if (roundTeamPlayer != null && roundTeamPlayer.getRoundTeam() != null && roundTeamPlayer.getRoundTeam().getTeamNumber() != null) {
            return roundTeamPlayer.getRoundTeam().getTeamNumber();
        }

        if (groupOrder != null && groupOrder.groupNumber != null) {
            return groupOrder.groupNumber;
        }

        return Integer.MAX_VALUE;
    }

    private Integer resolvePlayerOrder(RoundTeamPlayer roundTeamPlayer, PlayerGroupOrder groupOrder) {
        if (roundTeamPlayer != null && roundTeamPlayer.getPlayerOrder() != null) {
            return roundTeamPlayer.getPlayerOrder();
        }

        if (groupOrder != null && groupOrder.playerOrder != null) {
            return groupOrder.playerOrder;
        }

        return Integer.MAX_VALUE;
    }

    private RoundPlayerStatusResponse toPlayerStatus(Scorecard scorecard, RoundTeamPlayer roundTeamPlayer, PlayerGroupOrder groupOrder) {
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
        populateGroupOrder(dto, roundTeamPlayer, groupOrder);
        dto.setParticipationStatus(scorecard.getParticipationStatus() != null ? scorecard.getParticipationStatus().name() : "ACTIVE");
        dto.setWithdrawalHoleNumber(scorecard.getWithdrawalHoleNumber());

        return dto;
    }

    private RoundScorecardSummaryResponse toRoundScorecardSummary(Scorecard scorecard, RoundTeamPlayer roundTeamPlayer, PlayerGroupOrder groupOrder) {
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
        populateGroupOrder(dto, roundTeamPlayer, groupOrder);

        dto.setCourseHandicap(scorecard.getCourseHandicap());
        dto.setPlayingHandicap(scorecard.getPlayingHandicap());
        dto.setGrossScore(scorecard.getGrossScore());
        dto.setAdjustedGrossScore(scorecard.getAdjustedGrossScore());
        dto.setNetScore(scorecard.getNetScore());

        dto.setTeeName(resolvedTee != null ? resolvedTee.getTeeName() : null);
        dto.setCurrentTeeName(resolvedTee != null ? resolvedTee.getTeeName() : null);
        dto.setRoundTeeId(resolvedTee != null ? resolvedTee.getId() : null);
        dto.setParticipationStatus(scorecard.getParticipationStatus() != null ? scorecard.getParticipationStatus().name() : "ACTIVE");
        dto.setWithdrawalHoleNumber(scorecard.getWithdrawalHoleNumber());

        return dto;
    }

    private void populateGroupOrder(RoundPlayerStatusResponse dto, RoundTeamPlayer roundTeamPlayer, PlayerGroupOrder groupOrder) {
        if (dto == null) {
            return;
        }

        if (roundTeamPlayer != null && roundTeamPlayer.getRoundTeam() != null) {
            dto.setTeamId(roundTeamPlayer.getRoundTeam().getId());
            dto.setTeamName(roundTeamPlayer.getRoundTeam().getTeamName());
            dto.setTeamNumber(roundTeamPlayer.getRoundTeam().getTeamNumber());
            dto.setPlayerOrder(roundTeamPlayer.getPlayerOrder());
            return;
        }

        if (groupOrder != null) {
            dto.setTeamId(groupOrder.groupId);
            dto.setTeamName(groupOrder.groupName);
            dto.setTeamNumber(groupOrder.groupNumber);
            dto.setPlayerOrder(groupOrder.playerOrder);
        }
    }

    private void populateGroupOrder(RoundScorecardSummaryResponse dto, RoundTeamPlayer roundTeamPlayer, PlayerGroupOrder groupOrder) {
        if (dto == null) {
            return;
        }

        if (roundTeamPlayer != null && roundTeamPlayer.getRoundTeam() != null) {
            dto.setTeamId(roundTeamPlayer.getRoundTeam().getId());
            dto.setTeamName(roundTeamPlayer.getRoundTeam().getTeamName());
            dto.setTeamNumber(roundTeamPlayer.getRoundTeam().getTeamNumber());
            dto.setPlayerOrder(roundTeamPlayer.getPlayerOrder());
            return;
        }

        if (groupOrder != null) {
            dto.setTeamId(groupOrder.groupId);
            dto.setTeamName(groupOrder.groupName);
            dto.setTeamNumber(groupOrder.groupNumber);
            dto.setPlayerOrder(groupOrder.playerOrder);
        }
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


    private static class PlayerGroupOrder {
        private Long groupId;
        private String groupName;
        private Integer groupNumber;
        private Integer playerOrder;
    }
}
