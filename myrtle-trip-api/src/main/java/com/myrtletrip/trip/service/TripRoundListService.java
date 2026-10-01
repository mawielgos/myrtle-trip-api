package com.myrtletrip.trip.service;

import com.myrtletrip.event.dto.RoundEventResponse;
import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.round.service.RoundTeamAutoAssignmentService;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.dto.TripRoundListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripRoundListService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamAutoAssignmentService roundTeamAutoAssignmentService;
    private final RoundEventRepository roundEventRepository;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public TripRoundListService(
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundGroupRepository roundGroupRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamAutoAssignmentService roundTeamAutoAssignmentService,
            RoundEventRepository roundEventRepository,
            RoundEventCapabilityService roundEventCapabilityService) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamAutoAssignmentService = roundTeamAutoAssignmentService;
        this.roundEventRepository = roundEventRepository;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    @Transactional
    public List<TripRoundListResponse> getTripRounds(Long tripId) {
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundDateAsc(tripId);
        List<TripRoundListResponse> responses = new ArrayList<TripRoundListResponse>();

        for (Round round : rounds) {
            roundTeamAutoAssignmentService.syncTeamsFromGroupsIfNeeded(round.getId());

            List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
            List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(round.getId());
            List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());

            boolean finalized = Boolean.TRUE.equals(round.getFinalized());
            boolean needsGrouping = false;
            boolean needsTeams = false;
            boolean readyForScoring = false;

            if (!finalized) {
                needsGrouping = calculateNeedsGrouping(scorecards, groups);

                if (!needsGrouping) {
                    needsTeams = calculateNeedsTeams(round, scorecards, teams);
                }

                readyForScoring = !needsGrouping && !needsTeams;
            }

            TripRoundListResponse response = new TripRoundListResponse();
            response.setRoundId(round.getId());
            response.setRoundNumber(round.getRoundNumber());
            response.setRoundDate(round.getRoundDate());
            response.setCourseName(round.getCourse() != null ? round.getCourse().getName() : null);
            response.setTeeName(round.getStandardRoundTee() != null ? round.getStandardRoundTee().getTeeName() : null);
            response.setGameFormat(round.getFormat() != null ? round.getFormat().name() : null);
            response.setFinalized(finalized);
            response.setNeedsGrouping(needsGrouping);
            response.setNeedsTeams(needsTeams);
            response.setReadyForScoring(readyForScoring);
            response.setEvents(buildRoundEventResponses(round.getId()));

            responses.add(response);
        }

        return responses;
    }

    private List<RoundEventResponse> buildRoundEventResponses(Long roundId) {
        List<RoundEventResponse> responses = new ArrayList<RoundEventResponse>();
        if (roundId == null) {
            return responses;
        }

        List<RoundEvent> events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(roundId);
        for (RoundEvent event : events) {
            RoundEventResponse response = new RoundEventResponse();
            response.setId(event.getId());
            response.setRoundId(roundId);
            response.setEventType(event.getEventType());
            response.setEventName(event.getEventName());
            response.setEventOrder(event.getEventOrder());
            response.setActive(event.getActive());
            response.setUsesGross(event.getUsesGross());
            response.setUsesNet(event.getUsesNet());
            response.setUsesTeams(event.getUsesTeams());
            response.setTeamSize(event.getTeamSize());
            response.setHandicapPercent(event.getHandicapPercent());
            responses.add(response);
        }

        return responses;
    }

    private boolean calculateNeedsGrouping(List<Scorecard> scorecards, List<RoundGroup> groups) {
        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }

        if (groups == null || groups.isEmpty()) {
            return true;
        }

        Set<Long> roundPlayerIds = new HashSet<Long>();

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getPlayer() == null || scorecard.getPlayer().getId() == null) {
                continue;
            }

            roundPlayerIds.add(scorecard.getPlayer().getId());
        }

        if (roundPlayerIds.isEmpty()) {
            return true;
        }

        Set<Long> groupedPlayerIds = new HashSet<Long>();

        for (RoundGroup group : groups) {
            List<RoundGroupPlayer> players = group.getPlayers();

            if (players == null || players.isEmpty()) {
                return true;
            }

            if (players.size() > 4) {
                return true;
            }

            for (RoundGroupPlayer groupPlayer : players) {
                if (groupPlayer == null || groupPlayer.getPlayer() == null || groupPlayer.getPlayer().getId() == null) {
                    return true;
                }

                Long playerId = groupPlayer.getPlayer().getId();

                if (!roundPlayerIds.contains(playerId)) {
                    return true;
                }

                if (!groupedPlayerIds.add(playerId)) {
                    return true;
                }
            }
        }

        return groupedPlayerIds.size() != roundPlayerIds.size();
    }

    private boolean calculateNeedsTeams(Round round, List<Scorecard> scorecards, List<RoundTeam> teams) {
        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);

        if (!capabilities.requiresTeams()) {
            return false;
        }

        if (scorecards == null || scorecards.isEmpty()) {
            return true;
        }
        if (teams == null || teams.isEmpty()) {
            return true;
        }

        int expectedTeamSize = roundEventCapabilityService.expectedTeamSize(round);
        Map<Long, Integer> teamCounts = new HashMap<Long, Integer>();
        Set<Long> knownTeamIds = new HashSet<Long>();

        for (RoundTeam team : teams) {
            if (team.getId() != null) {
                knownTeamIds.add(team.getId());
                teamCounts.put(team.getId(), 0);
            }
        }

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null || scorecard.getPlayer() == null || scorecard.getPlayer().getId() == null) {
                return true;
            }

            if (scorecard.getTeam() == null || scorecard.getTeam().getId() == null) {
                return true;
            }

            Long teamId = scorecard.getTeam().getId();

            if (!knownTeamIds.contains(teamId)) {
                return true;
            }

            Integer currentCount = teamCounts.get(teamId);
            if (currentCount == null) {
                currentCount = 0;
            }

            teamCounts.put(teamId, currentCount + 1);
        }

        for (Map.Entry<Long, Integer> entry : teamCounts.entrySet()) {
            Integer teamSize = entry.getValue();

            if (teamSize == null || teamSize.intValue() != expectedTeamSize) {
                return true;
            }
        }

        return false;
    }
}
