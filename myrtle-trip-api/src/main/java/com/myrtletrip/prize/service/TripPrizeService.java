package com.myrtletrip.prize.service;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.repository.RoundEventRepository;
import com.myrtletrip.prize.dto.PrizeSchedulePayoutResponse;
import com.myrtletrip.prize.dto.PrizeScheduleResponse;
import com.myrtletrip.prize.dto.SavePrizeSchedulePayoutRequest;
import com.myrtletrip.prize.dto.SavePrizeScheduleRequest;
import com.myrtletrip.prize.dto.SaveTripPrizeSchedulesRequest;
import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import com.myrtletrip.prize.model.PrizePayoutUnit;
import com.myrtletrip.prize.model.PrizeResultScope;
import com.myrtletrip.prize.repository.PrizeSchedulePayoutRepository;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.tournament.entity.TripTournament;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import com.myrtletrip.trip.repository.TripPlannedRoundEventRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripPrizeService {

    public static final String LEGACY_TOURNAMENT_GAME_KEY = "FOUR_DAY_INDIVIDUAL";
    public static final String TOURNAMENT_LOW_NET_GAME_KEY = "TOURNAMENT_LOW_NET";
    public static final String TOURNAMENT_LOW_GROSS_GAME_KEY = "TOURNAMENT_LOW_GROSS";
    public static final String ROUND_EVENT_KEY_PREFIX = "ROUND_NUMBER_";
    public static final String ROUND_EVENT_KEY_SEPARATOR = "_EVENT_";

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final TripPlannedRoundEventRepository tripPlannedRoundEventRepository;
    private final RoundEventRepository roundEventRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final PrizeSchedulePayoutRepository prizeSchedulePayoutRepository;
    private final TripTournamentRepository tripTournamentRepository;
    private final TripEditingGuardService tripEditingGuardService;

    public TripPrizeService(TripRepository tripRepository,
                            RoundRepository roundRepository,
                            TripPlannedRoundRepository tripPlannedRoundRepository,
                            TripPlannedRoundEventRepository tripPlannedRoundEventRepository,
                            RoundEventRepository roundEventRepository,
                            PrizeScheduleRepository prizeScheduleRepository,
                            PrizeSchedulePayoutRepository prizeSchedulePayoutRepository,
                            TripTournamentRepository tripTournamentRepository,
                            TripEditingGuardService tripEditingGuardService) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.tripPlannedRoundEventRepository = tripPlannedRoundEventRepository;
        this.roundEventRepository = roundEventRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.prizeSchedulePayoutRepository = prizeSchedulePayoutRepository;
        this.tripTournamentRepository = tripTournamentRepository;
        this.tripEditingGuardService = tripEditingGuardService;
    }

    @Transactional
    public List<PrizeScheduleResponse> getPrizeSchedules(Long tripId) {
        ensureDefaultSchedules(tripId);
        return loadResponses(tripId);
    }

    @Transactional(readOnly = true)
    public Set<String> getActivePrizeScheduleKeys(Long tripId) {
        return buildEligiblePrizeKeys(tripId);
    }

    @Transactional
    public List<PrizeScheduleResponse> savePrizeSchedules(Long tripId, SaveTripPrizeSchedulesRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        tripEditingGuardService.assertStructureEditable(trip);

        ensureDefaultSchedules(tripId);
        Set<String> eligibleKeys = buildEligiblePrizeKeys(tripId);

        Map<String, PrizeSchedule> schedulesByKey = new HashMap<>();
        List<PrizeSchedule> existingSchedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);
        for (PrizeSchedule schedule : existingSchedules) {
            schedulesByKey.put(schedule.getGameKey(), schedule);
        }

        if (request != null && request.getSchedules() != null) {
            for (SavePrizeScheduleRequest scheduleRequest : request.getSchedules()) {
                if (scheduleRequest == null || scheduleRequest.getGameKey() == null) {
                    continue;
                }
                if (!eligibleKeys.contains(scheduleRequest.getGameKey())) {
                    continue;
                }

                PrizeSchedule schedule = schedulesByKey.get(scheduleRequest.getGameKey());
                if (schedule == null) {
                    continue;
                }

                if (scheduleRequest.getGameName() != null && !scheduleRequest.getGameName().isBlank()) {
                    schedule.setGameName(scheduleRequest.getGameName().trim());
                }

                schedule.setResultScope(parseResultScope(scheduleRequest.getResultScope(), schedule.getResultScope()));
                schedule.setPayoutUnit(parsePayoutUnit(scheduleRequest.getPayoutUnit(), schedule.getPayoutUnit()));
                prizeScheduleRepository.save(schedule);

                prizeSchedulePayoutRepository.deleteByPrizeSchedule_Id(schedule.getId());
                prizeSchedulePayoutRepository.flush();

                List<PrizeSchedulePayout> replacementPayouts = new ArrayList<>();

                if (scheduleRequest.getPayouts() != null) {
                    List<SavePrizeSchedulePayoutRequest> payoutRequests = new ArrayList<>(scheduleRequest.getPayouts());
                    payoutRequests.sort(Comparator.comparing(SavePrizeSchedulePayoutRequest::getFinishingPlace,
                            Comparator.nullsLast(Integer::compareTo)));

                    for (SavePrizeSchedulePayoutRequest payoutRequest : payoutRequests) {
                        if (payoutRequest == null || payoutRequest.getFinishingPlace() == null) {
                            continue;
                        }

                        if (payoutRequest.getFinishingPlace() < 1) {
                            continue;
                        }

                        BigDecimal amount = payoutRequest.getAmountPerPlayer();
                        if (amount == null) {
                            amount = BigDecimal.ZERO;
                        }

                        PrizeSchedulePayout payout = new PrizeSchedulePayout();
                        payout.setPrizeSchedule(schedule);
                        payout.setFinishingPlace(payoutRequest.getFinishingPlace());
                        payout.setAmountPerPlayer(amount);
                        replacementPayouts.add(payout);
                    }
                }

                if (!replacementPayouts.isEmpty()) {
                    prizeSchedulePayoutRepository.saveAll(replacementPayouts);
                    prizeSchedulePayoutRepository.flush();
                }
            }
        }

        return loadResponses(tripId);
    }

    private List<PrizeScheduleResponse> loadResponses(Long tripId) {
        Set<String> eligibleKeys = buildEligiblePrizeKeys(tripId);
        List<PrizeSchedule> schedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);
        schedules.sort((a, b) -> sortValue(a).compareTo(sortValue(b)));

        List<PrizeScheduleResponse> responses = new ArrayList<>();
        for (PrizeSchedule schedule : schedules) {
            if (!eligibleKeys.contains(schedule.getGameKey())) {
                continue;
            }
            responses.add(toResponse(schedule));
        }
        return responses;
    }

    private void ensureDefaultSchedules(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<PrizeSchedule> existingSchedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);

        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (isTournamentPrizeEligible(tournament)) {
            ensureTournamentSchedule(trip, existingSchedules, TOURNAMENT_LOW_NET_GAME_KEY, resolveTournamentPrizeName(tournament, "Low Net"), true);
            if (Boolean.TRUE.equals(tournament.getLowGrossEnabled())) {
                ensureTournamentSchedule(trip, existingSchedules, TOURNAMENT_LOW_GROSS_GAME_KEY, resolveTournamentPrizeName(tournament, "Low Gross"), false);
            }
        }

        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        Map<Integer, Round> roundsByNumber = new HashMap<>();
        for (Round round : rounds) {
            if (round.getRoundNumber() != null) {
                roundsByNumber.put(round.getRoundNumber(), round);
            }
        }

        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null || plannedRound.getRoundNumber() == null) {
                continue;
            }

            Round matchingRound = roundsByNumber.get(plannedRound.getRoundNumber());
            List<PrizeEventDescriptor> descriptors = buildPrizeEventDescriptors(plannedRound, matchingRound);
            for (PrizeEventDescriptor descriptor : descriptors) {
                PrizeSchedule schedule = findByGameKey(existingSchedules, descriptor.gameKey);
                if (schedule == null) {
                    schedule = new PrizeSchedule();
                    schedule.setTrip(trip);
                    schedule.setGameKey(descriptor.gameKey);
                    schedule.setGameName(descriptor.gameName);
                    schedule.setResultScope(descriptor.resultScope);
                    schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
                    existingSchedules.add(schedule);
                }

                if (matchingRound != null && schedule.getRound() == null) {
                    schedule.setRound(matchingRound);
                }

                schedule.setResultScope(descriptor.resultScope);
                if (schedule.getGameName() == null || schedule.getGameName().isBlank() || isDefaultRoundGameName(schedule.getGameName())) {
                    schedule.setGameName(descriptor.gameName);
                }
                prizeScheduleRepository.save(schedule);
            }
        }

        for (Round round : rounds) {
            if (round.getRoundNumber() == null) {
                continue;
            }

            List<RoundEvent> events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(round.getId());
            if (events.isEmpty()) {
                continue;
            }

            for (RoundEvent event : events) {
                PrizeEventDescriptor descriptor = descriptorForRoundEvent(round.getRoundNumber(), event.getEventType(), event.getEventName());
                PrizeSchedule schedule = findByGameKey(existingSchedules, descriptor.gameKey);
                if (schedule == null) {
                    schedule = new PrizeSchedule();
                    schedule.setTrip(trip);
                    schedule.setGameKey(descriptor.gameKey);
                    schedule.setGameName(descriptor.gameName);
                    schedule.setResultScope(descriptor.resultScope);
                    schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
                    existingSchedules.add(schedule);
                }
                schedule.setRound(round);
                schedule.setResultScope(descriptor.resultScope);
                if (schedule.getGameName() == null || schedule.getGameName().isBlank() || isDefaultRoundGameName(schedule.getGameName())) {
                    schedule.setGameName(descriptor.gameName);
                }
                prizeScheduleRepository.save(schedule);
            }
        }
    }

    private Set<String> buildEligiblePrizeKeys(Long tripId) {
        Set<String> keys = new LinkedHashSet<>();
        Trip trip = tripRepository.findById(tripId).orElse(null);
        if (trip == null) {
            return keys;
        }

        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (isTournamentPrizeEligible(tournament)) {
            if (tournament.getLowNetEnabled() == null || Boolean.TRUE.equals(tournament.getLowNetEnabled())) {
                keys.add(TOURNAMENT_LOW_NET_GAME_KEY);
                keys.add(LEGACY_TOURNAMENT_GAME_KEY);
            }
            if (Boolean.TRUE.equals(tournament.getLowGrossEnabled())) {
                keys.add(TOURNAMENT_LOW_GROSS_GAME_KEY);
            }
        }

        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        Map<Integer, Round> roundsByNumber = new HashMap<>();
        for (Round round : rounds) {
            if (round.getRoundNumber() != null) {
                roundsByNumber.put(round.getRoundNumber(), round);
            }
        }

        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        for (TripPlannedRound plannedRound : plannedRounds) {
            if (plannedRound == null || plannedRound.getRoundNumber() == null) {
                continue;
            }
            Round matchingRound = roundsByNumber.get(plannedRound.getRoundNumber());
            for (PrizeEventDescriptor descriptor : buildPrizeEventDescriptors(plannedRound, matchingRound)) {
                keys.add(descriptor.gameKey);
            }
        }

        for (Round round : rounds) {
            if (round.getRoundNumber() == null) {
                continue;
            }
            List<RoundEvent> events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(round.getId());
            for (RoundEvent event : events) {
                keys.add(buildRoundEventGameKey(round.getRoundNumber(), event.getEventType()));
            }
        }

        return keys;
    }

    private List<PrizeEventDescriptor> buildPrizeEventDescriptors(TripPlannedRound plannedRound, Round matchingRound) {
        List<PrizeEventDescriptor> descriptors = new ArrayList<>();
        if (matchingRound != null) {
            List<RoundEvent> events = roundEventRepository.findByRound_IdAndActiveTrueOrderByEventOrderAsc(matchingRound.getId());
            if (!events.isEmpty()) {
                for (RoundEvent event : events) {
                    descriptors.add(descriptorForRoundEvent(plannedRound.getRoundNumber(), event.getEventType(), event.getEventName()));
                }
                return descriptors;
            }
        }

        List<TripPlannedRoundEvent> plannedEvents = tripPlannedRoundEventRepository.findByPlannedRound_IdOrderByEventOrderAsc(plannedRound.getId());
        if (plannedEvents.isEmpty() && plannedRound.getFormat() != null) {
            RoundEventType legacyType = RoundEventType.fromLegacyRoundFormat(plannedRound.getFormat());
            descriptors.add(descriptorForRoundEvent(plannedRound.getRoundNumber(), legacyType, legacyType.defaultName(plannedRound.getScrambleTeamSize())));
            return descriptors;
        }

        for (TripPlannedRoundEvent event : plannedEvents) {
            descriptors.add(descriptorForRoundEvent(plannedRound.getRoundNumber(), event.getEventType(), event.getEventName()));
        }
        return descriptors;
    }

    private PrizeEventDescriptor descriptorForRoundEvent(Integer roundNumber, RoundEventType eventType, String eventName) {
        PrizeEventDescriptor descriptor = new PrizeEventDescriptor();
        descriptor.gameKey = buildRoundEventGameKey(roundNumber, eventType);
        descriptor.gameName = buildRoundEventGameName(roundNumber, eventType, eventName);
        descriptor.resultScope = eventType != null && eventType.isTeamEvent() ? PrizeResultScope.TEAM : PrizeResultScope.PLAYER;
        return descriptor;
    }

    public static String buildRoundEventGameKey(Integer roundNumber, RoundEventType eventType) {
        return ROUND_EVENT_KEY_PREFIX + roundNumber + ROUND_EVENT_KEY_SEPARATOR + eventType;
    }

    public static RoundEventType parseRoundEventTypeFromGameKey(String gameKey) {
        if (gameKey == null) {
            return null;
        }
        int index = gameKey.indexOf(ROUND_EVENT_KEY_SEPARATOR);
        if (index < 0) {
            return null;
        }
        String eventTypeText = gameKey.substring(index + ROUND_EVENT_KEY_SEPARATOR.length());
        try {
            return RoundEventType.valueOf(eventTypeText);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String buildRoundEventGameName(Integer roundNumber, RoundEventType eventType, String eventName) {
        String label = eventName;
        if (label == null || label.isBlank()) {
            label = eventType == null ? "Event" : eventType.defaultName(null);
        }
        if (roundNumber == null) {
            return label;
        }
        return "Round " + roundNumber + " - " + label.trim();
    }

    private boolean isTournamentPrizeEligible(TripTournament tournament) {
        return tournament != null
                && Boolean.TRUE.equals(tournament.getEnabled())
                && tournament.getRounds() != null
                && tournament.getRounds().size() >= 2;
    }

    private void ensureTournamentSchedule(Trip trip, List<PrizeSchedule> existingSchedules, String gameKey, String gameName, boolean migrateLegacy) {
        PrizeSchedule schedule = findByGameKey(existingSchedules, gameKey);
        if (schedule == null && migrateLegacy) {
            schedule = findByGameKey(existingSchedules, LEGACY_TOURNAMENT_GAME_KEY);
            if (schedule != null) {
                schedule.setGameKey(gameKey);
            }
        }
        if (schedule == null) {
            schedule = new PrizeSchedule();
            schedule.setTrip(trip);
            schedule.setGameKey(gameKey);
            schedule.setGameName(gameName);
            schedule.setResultScope(PrizeResultScope.PLAYER);
            schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
            existingSchedules.add(schedule);
        } else if (isLegacyTournamentGameName(schedule.getGameName())) {
            schedule.setGameName(gameName);
        }
        schedule.setResultScope(PrizeResultScope.PLAYER);
        schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
        prizeScheduleRepository.save(schedule);
    }

    private String resolveTournamentPrizeName(TripTournament tournament, String competitionLabel) {
        if (tournament != null) {
            if ("Low Gross".equalsIgnoreCase(competitionLabel)
                    && tournament.getLowGrossName() != null
                    && !tournament.getLowGrossName().isBlank()) {
                return tournament.getLowGrossName().trim();
            }
            if ("Low Net".equalsIgnoreCase(competitionLabel)
                    && tournament.getLowNetName() != null
                    && !tournament.getLowNetName().isBlank()) {
                return tournament.getLowNetName().trim();
            }
        }
        return "Multi-Round Tournament - " + competitionLabel;
    }

    private boolean isLegacyTournamentGameName(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            return true;
        }
        String normalized = gameName.trim();
        return "Multi-Round Individual Low Net".equalsIgnoreCase(normalized)
                || "Four Day Individual Low Net".equalsIgnoreCase(normalized)
                || "Multi-Round Tournament".equalsIgnoreCase(normalized)
                || "Tournament Standings".equalsIgnoreCase(normalized)
                || "Low Net Tournament".equalsIgnoreCase(normalized)
                || "Low Gross Tournament".equalsIgnoreCase(normalized)
                || "Low Net Tournament - Low Gross".equalsIgnoreCase(normalized)
                || "Tournament - Low Gross".equalsIgnoreCase(normalized);
    }

    private PrizeSchedule findByGameKey(List<PrizeSchedule> schedules, String gameKey) {
        for (PrizeSchedule schedule : schedules) {
            if (schedule != null && gameKey.equals(schedule.getGameKey())) {
                return schedule;
            }
        }
        return null;
    }

    private boolean isDefaultRoundGameName(String gameName) {
        return gameName != null && gameName.startsWith("Round ");
    }

    private PrizeScheduleResponse toResponse(PrizeSchedule schedule) {
        PrizeScheduleResponse response = new PrizeScheduleResponse();
        response.setScheduleId(schedule.getId());
        response.setTripId(schedule.getTrip().getId());
        response.setGameKey(schedule.getGameKey());
        response.setGameName(schedule.getGameName());
        response.setResultScope(schedule.getResultScope().name());
        response.setPayoutUnit(schedule.getPayoutUnit().name());

        Round round = schedule.getRound();
        if (round != null) {
            response.setRoundId(round.getId());
            response.setRoundNumber(round.getRoundNumber());
        }

        List<PrizeSchedulePayoutResponse> payoutResponses = new ArrayList<>();
        List<PrizeSchedulePayout> payouts = prizeSchedulePayoutRepository.findByPrizeSchedule_IdOrderByFinishingPlaceAsc(schedule.getId());
        for (PrizeSchedulePayout payout : payouts) {
            PrizeSchedulePayoutResponse payoutResponse = new PrizeSchedulePayoutResponse();
            payoutResponse.setPayoutId(payout.getId());
            payoutResponse.setFinishingPlace(payout.getFinishingPlace());
            payoutResponse.setAmountPerPlayer(payout.getAmountPerPlayer());
            payoutResponses.add(payoutResponse);
        }
        response.setPayouts(payoutResponses);

        return response;
    }

    private Integer sortValue(PrizeSchedule schedule) {
        if (TOURNAMENT_LOW_NET_GAME_KEY.equals(schedule.getGameKey()) || LEGACY_TOURNAMENT_GAME_KEY.equals(schedule.getGameKey())) {
            return 0;
        }
        if (TOURNAMENT_LOW_GROSS_GAME_KEY.equals(schedule.getGameKey())) {
            return 1;
        }

        if (schedule.getRound() != null && schedule.getRound().getRoundNumber() != null) {
            return schedule.getRound().getRoundNumber() * 10 + eventSortOffset(schedule.getGameKey());
        }

        String key = schedule.getGameKey();
        if (key != null && key.startsWith(ROUND_EVENT_KEY_PREFIX)) {
            String withoutPrefix = key.substring(ROUND_EVENT_KEY_PREFIX.length());
            int separatorIndex = withoutPrefix.indexOf(ROUND_EVENT_KEY_SEPARATOR);
            if (separatorIndex > 0) {
                try {
                    Integer roundNumber = Integer.valueOf(withoutPrefix.substring(0, separatorIndex));
                    return roundNumber * 10 + eventSortOffset(key);
                } catch (NumberFormatException ignored) {
                    return 9999;
                }
            }
        }

        return 9999;
    }

    private Integer eventSortOffset(String gameKey) {
        RoundEventType eventType = parseRoundEventTypeFromGameKey(gameKey);
        if (eventType == null) {
            return 0;
        }
        return switch (eventType) {
            case INDIVIDUAL_LOW_NET -> 1;
            case INDIVIDUAL_LOW_GROSS -> 2;
            case TEAM_TWO_MAN_LOW_NET -> 3;
            case TEAM_TWO_LOW_NET -> 4;
            case TEAM_MIDDLE_MAN -> 5;
            case TEAM_ONE_TWO_THREE -> 6;
            case TEAM_THREE_LOW_NET -> 7;
            case TEAM_SCRAMBLE -> 8;
        };
    }

    private PrizeResultScope parseResultScope(String value, PrizeResultScope fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return PrizeResultScope.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private PrizePayoutUnit parsePayoutUnit(String value, PrizePayoutUnit fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return PrizePayoutUnit.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static class PrizeEventDescriptor {
        private String gameKey;
        private String gameName;
        private PrizeResultScope resultScope;
    }
}
