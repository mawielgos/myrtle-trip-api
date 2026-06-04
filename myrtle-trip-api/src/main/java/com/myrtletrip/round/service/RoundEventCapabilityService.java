package com.myrtletrip.round.service;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.round.entity.Round;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundEventCapabilityService {

    private final RoundEventRepository roundEventRepository;

    public RoundEventCapabilityService(RoundEventRepository roundEventRepository) {
        this.roundEventRepository = roundEventRepository;
    }

    public RoundEventCapabilities getCapabilities(Round round) {
        List<RoundEvent> events = loadActiveEvents(round);

        boolean hasTeamEvent = false;
        boolean hasIndividualEvent = false;
        boolean hasScrambleEvent = false;
        boolean hasTwoManLowNetEvent = false;
        boolean hasNetEvent = false;
        boolean hasGrossEvent = false;
        Integer expectedTeamSize = null;

        for (RoundEvent event : events) {
            if (event == null || event.getEventType() == null) {
                continue;
            }

            RoundEventType eventType = event.getEventType();

            if (eventType.isTeamEvent()) {
                hasTeamEvent = true;
                Integer teamSize = event.getTeamSize();
                if (teamSize == null || teamSize < 1) {
                    teamSize = eventType.defaultTeamSize(round == null ? null : round.getScrambleTeamSize());
                }
                if (teamSize != null && teamSize > 0) {
                    expectedTeamSize = teamSize;
                }
            } else {
                hasIndividualEvent = true;
            }

            if (eventType == RoundEventType.TEAM_SCRAMBLE) {
                hasScrambleEvent = true;
            }
            if (eventType == RoundEventType.TEAM_TWO_MAN_LOW_NET) {
                hasTwoManLowNetEvent = true;
            }
            if (eventType.usesNet()) {
                hasNetEvent = true;
            }
            if (eventType.usesGross()) {
                hasGrossEvent = true;
            }
        }

        if (hasTeamEvent && (expectedTeamSize == null || expectedTeamSize < 1)) {
            expectedTeamSize = 4;
        }

        return new RoundEventCapabilities(
                hasTeamEvent,
                hasIndividualEvent,
                hasScrambleEvent,
                hasTwoManLowNetEvent,
                hasNetEvent,
                hasGrossEvent,
                expectedTeamSize
        );
    }

    public boolean hasConfiguredEvents(Round round) {
        if (round != null && round.getId() != null) {
            List<RoundEvent> events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(round.getId());
            if (events != null && !events.isEmpty()) {
                return true;
            }
        }
        // Compatibility only: older rounds may not have round_event rows yet.
        return round != null && round.getFormat() != null;
    }

    public boolean requiresTeams(Round round) {
        return getCapabilities(round).requiresTeams();
    }

    public boolean hasIndividualEvents(Round round) {
        return getCapabilities(round).hasIndividualEvent();
    }

    public boolean isScrambleRound(Round round) {
        return getCapabilities(round).hasScrambleEvent();
    }

    public boolean isTwoManLowNetRound(Round round) {
        return getCapabilities(round).hasTwoManLowNetEvent();
    }

    public boolean requiresNetScores(Round round) {
        return getCapabilities(round).requiresNetScores();
    }

    public int expectedTeamSize(Round round) {
        Integer size = getCapabilities(round).expectedTeamSize();
        return size == null || size < 1 ? 1 : size;
    }

    private List<RoundEvent> loadActiveEvents(Round round) {
        List<RoundEvent> events = new ArrayList<>();
        if (round != null && round.getId() != null) {
            events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(round.getId());
        }

        if (events != null && !events.isEmpty()) {
            return events;
        }

        // Compatibility only: synthesize a single event from legacy round.format when no round_event rows exist.
        List<RoundEvent> fallback = new ArrayList<>();
        RoundEventType fallbackType = RoundEventType.fromLegacyRoundFormat(round == null ? null : round.getFormat());
        RoundEvent event = new RoundEvent();
        event.setRound(round);
        event.setEventType(fallbackType);
        Integer teamSize = fallbackType.defaultTeamSize(round == null ? null : round.getScrambleTeamSize());
        event.setTeamSize(teamSize);
        event.setUsesTeams(fallbackType.isTeamEvent());
        event.setUsesGross(fallbackType.usesGross());
        event.setUsesNet(fallbackType.usesNet());
        fallback.add(event);
        return fallback;
    }

    public static class RoundEventCapabilities {
        private final boolean hasTeamEvent;
        private final boolean hasIndividualEvent;
        private final boolean hasScrambleEvent;
        private final boolean hasTwoManLowNetEvent;
        private final boolean hasNetEvent;
        private final boolean hasGrossEvent;
        private final Integer expectedTeamSize;

        public RoundEventCapabilities(
                boolean hasTeamEvent,
                boolean hasIndividualEvent,
                boolean hasScrambleEvent,
                boolean hasTwoManLowNetEvent,
                boolean hasNetEvent,
                boolean hasGrossEvent,
                Integer expectedTeamSize
        ) {
            this.hasTeamEvent = hasTeamEvent;
            this.hasIndividualEvent = hasIndividualEvent;
            this.hasScrambleEvent = hasScrambleEvent;
            this.hasTwoManLowNetEvent = hasTwoManLowNetEvent;
            this.hasNetEvent = hasNetEvent;
            this.hasGrossEvent = hasGrossEvent;
            this.expectedTeamSize = expectedTeamSize;
        }

        public boolean hasTeamEvent() { return hasTeamEvent; }
        public boolean hasIndividualEvent() { return hasIndividualEvent; }
        public boolean hasScrambleEvent() { return hasScrambleEvent; }
        public boolean hasTwoManLowNetEvent() { return hasTwoManLowNetEvent; }
        public boolean hasNetEvent() { return hasNetEvent; }
        public boolean hasGrossEvent() { return hasGrossEvent; }
        public Integer expectedTeamSize() { return expectedTeamSize; }

        public boolean requiresTeams() { return hasTeamEvent; }
        public boolean requiresNetScores() { return hasNetEvent; }
        public boolean requiresPlayerScorecards() { return !hasScrambleEvent || hasIndividualEvent || hasNetEvent; }
    }
}
