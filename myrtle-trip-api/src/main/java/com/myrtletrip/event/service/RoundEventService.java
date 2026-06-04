package com.myrtletrip.event.service;

import com.myrtletrip.event.dto.RoundEventResponse;
import com.myrtletrip.event.dto.SaveRoundEventsRequest;
import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoundEventService {

    private final RoundEventRepository roundEventRepository;
    private final RoundRepository roundRepository;
    private final TripEditingGuardService tripEditingGuardService;

    public RoundEventService(RoundEventRepository roundEventRepository,
                             RoundRepository roundRepository,
                             TripEditingGuardService tripEditingGuardService) {
        this.roundEventRepository = roundEventRepository;
        this.roundRepository = roundRepository;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @Transactional(readOnly = true)
    public List<RoundEvent> findActiveEventsForRound(Long roundId) {
        List<RoundEvent> events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(roundId);
        if (!events.isEmpty()) {
            return events;
        }

        Round round = loadRound(roundId);
        List<RoundEvent> fallbackEvents = new ArrayList<>();
        fallbackEvents.add(buildLegacyFallbackEvent(round));
        return fallbackEvents;
    }

    @Transactional(readOnly = true)
    public List<RoundEventResponse> getRoundEvents(Long roundId) {
        List<RoundEventResponse> responses = new ArrayList<>();
        for (RoundEvent event : findActiveEventsForRound(roundId)) {
            responses.add(toResponse(event));
        }
        return responses;
    }

    @Transactional
    public List<RoundEventResponse> saveRoundEvents(Long roundId, SaveRoundEventsRequest request) {
        Round round = loadRound(roundId);
        tripEditingGuardService.assertStructureEditable(round.getTrip());

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Round events cannot be changed after the round is finalized.");
        }

        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            throw new IllegalArgumentException("At least one round event is required.");
        }

        validateRequestedEvents(request, round.getScrambleTeamSize());

        List<RoundEvent> existing = roundEventRepository.findByRound_IdOrderByEventOrderAsc(roundId);
        roundEventRepository.deleteAll(existing);
        roundEventRepository.flush();

        List<RoundEvent> savedEvents = new ArrayList<>();
        int defaultOrder = 1;
        for (SaveRoundEventsRequest.SaveRoundEventItemRequest item : request.getEvents()) {
            RoundEvent event = new RoundEvent();
            applyRequest(round, event, item, defaultOrder);
            savedEvents.add(roundEventRepository.save(event));
            defaultOrder++;
        }

        syncLegacyRoundFormat(round, savedEvents);
        roundRepository.save(round);

        List<RoundEventResponse> responses = new ArrayList<>();
        for (RoundEvent event : savedEvents) {
            responses.add(toResponse(event));
        }
        return responses;
    }

    private void validateRequestedEvents(SaveRoundEventsRequest request, Integer scrambleTeamSize) {
        Set<RoundEventType> seen = new HashSet<>();
        RoundEventType teamEventType = null;

        for (SaveRoundEventsRequest.SaveRoundEventItemRequest item : request.getEvents()) {
            if (item == null || item.getEventType() == null) {
                throw new IllegalArgumentException("Each round event must have an eventType.");
            }

            RoundEventType eventType = item.getEventType();
            if (!seen.add(eventType)) {
                throw new IllegalArgumentException("Duplicate round event type: " + eventType);
            }

            if (eventType.isTeamEvent()) {
                if (teamEventType != null) {
                    throw new IllegalArgumentException("This round cannot include more than one team event. "
                            + "Create separate rounds for additional team games, or use one team event with individual side events.");
                }
                teamEventType = eventType;
            }
        }
    }

    private void applyRequest(Round round,
                              RoundEvent event,
                              SaveRoundEventsRequest.SaveRoundEventItemRequest item,
                              int defaultOrder) {
        RoundEventType eventType = item.getEventType();
        Integer teamSize = item.getTeamSize() != null ? item.getTeamSize() : eventType.defaultTeamSize(round.getScrambleTeamSize());

        event.setRound(round);
        event.setEventType(eventType);
        event.setEventName(item.getEventName() == null || item.getEventName().isBlank()
                ? eventType.defaultName(teamSize)
                : item.getEventName().trim());
        event.setEventOrder(item.getEventOrder() == null ? defaultOrder : item.getEventOrder());
        event.setActive(Boolean.TRUE);
        event.setUsesGross(eventType.usesGross());
        event.setUsesNet(eventType.usesNet());
        event.setUsesTeams(eventType.isTeamEvent());
        event.setTeamSize(teamSize);
        event.setHandicapPercent(item.getHandicapPercent() == null ? round.getHandicapPercent() : item.getHandicapPercent());
    }

    private void syncLegacyRoundFormat(Round round, List<RoundEvent> events) {
        for (RoundEvent event : events) {
            if (event.getEventType() != null && event.getEventType().isTeamEvent()) {
                round.setFormat(event.getEventType().legacyRoundFormat());
                if (event.getEventType() == RoundEventType.TEAM_SCRAMBLE && event.getTeamSize() != null) {
                    round.setScrambleTeamSize(event.getTeamSize());
                }
                return;
            }
        }

        round.setFormat(RoundEventType.INDIVIDUAL_LOW_NET.legacyRoundFormat());
    }

    private RoundEvent buildLegacyFallbackEvent(Round round) {
        RoundEventType eventType = RoundEventType.fromLegacyRoundFormat(round.getFormat());
        RoundEvent event = new RoundEvent();
        Integer teamSize = eventType.defaultTeamSize(round.getScrambleTeamSize());
        event.setRound(round);
        event.setEventType(eventType);
        event.setEventName(eventType.defaultName(teamSize));
        event.setEventOrder(1);
        event.setActive(Boolean.TRUE);
        event.setUsesGross(eventType.usesGross());
        event.setUsesNet(eventType.usesNet());
        event.setUsesTeams(eventType.isTeamEvent());
        event.setTeamSize(teamSize);
        event.setHandicapPercent(round.getHandicapPercent());
        return event;
    }

    private RoundEventResponse toResponse(RoundEvent event) {
        RoundEventResponse response = new RoundEventResponse();
        response.setId(event.getId());
        response.setRoundId(event.getRound() == null ? null : event.getRound().getId());
        response.setEventType(event.getEventType());
        response.setEventName(event.getEventName());
        response.setEventOrder(event.getEventOrder());
        response.setActive(event.getActive());
        response.setUsesGross(event.getUsesGross());
        response.setUsesNet(event.getUsesNet());
        response.setUsesTeams(event.getUsesTeams());
        response.setTeamSize(event.getTeamSize());
        response.setHandicapPercent(event.getHandicapPercent());
        if (event.getEventType() != null) {
            Integer teamSize = event.getTeamSize() != null ? event.getTeamSize() : event.getEventType().defaultTeamSize(null);
            response.setTeamBased(event.getEventType().isTeamEvent());
            response.setIndividualEvent(event.getEventType().isIndividualEvent());
            response.setUsesHandicap(event.getEventType().usesHandicap());
            response.setPayoutEligible(event.getEventType().isPayoutEligibleByDefault());
            response.setTournamentEligible(event.getEventType().isTournamentEligibleByDefault());
            response.setScoringModeLabel(event.getEventType().scoringModeLabel());
            response.setDefaultEventName(event.getEventType().defaultName(teamSize));
        }
        return response;
    }

    private Round loadRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
    }
}
