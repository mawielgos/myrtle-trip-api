package com.myrtletrip.prize.service;

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
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TripPrizeService {

    private static final String FOUR_DAY_GAME_KEY = "FOUR_DAY_INDIVIDUAL";

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final PrizeSchedulePayoutRepository prizeSchedulePayoutRepository;

    public TripPrizeService(TripRepository tripRepository,
                            RoundRepository roundRepository,
                            PrizeScheduleRepository prizeScheduleRepository,
                            PrizeSchedulePayoutRepository prizeSchedulePayoutRepository) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.prizeSchedulePayoutRepository = prizeSchedulePayoutRepository;
    }

    @Transactional
    public List<PrizeScheduleResponse> getPrizeSchedules(Long tripId) {
        ensureDefaultSchedules(tripId);
        return loadResponses(tripId);
    }

    @Transactional
    public List<PrizeScheduleResponse> savePrizeSchedules(Long tripId, SaveTripPrizeSchedulesRequest request) {
        ensureDefaultSchedules(tripId);

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
        List<PrizeSchedule> schedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);
        schedules.sort((a, b) -> {
            Integer aSort = sortValue(a);
            Integer bSort = sortValue(b);
            return aSort.compareTo(bSort);
        });

        List<PrizeScheduleResponse> responses = new ArrayList<>();
        for (PrizeSchedule schedule : schedules) {
            responses.add(toResponse(schedule));
        }
        return responses;
    }

    private void ensureDefaultSchedules(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (prizeScheduleRepository.findByTrip_IdAndGameKey(tripId, FOUR_DAY_GAME_KEY).isEmpty()) {
            PrizeSchedule schedule = new PrizeSchedule();
            schedule.setTrip(trip);
            schedule.setGameKey(FOUR_DAY_GAME_KEY);
            schedule.setGameName("4-Day Individual Low Net");
            schedule.setResultScope(PrizeResultScope.PLAYER);
            schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
            prizeScheduleRepository.save(schedule);
        }

        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        for (Round round : rounds) {
            String gameKey = buildRoundGameKey(round);
            if (prizeScheduleRepository.findByTrip_IdAndGameKey(tripId, gameKey).isPresent()) {
                continue;
            }

            PrizeSchedule schedule = new PrizeSchedule();
            schedule.setTrip(trip);
            schedule.setRound(round);
            schedule.setGameKey(gameKey);
            schedule.setGameName(buildRoundGameName(round));
            schedule.setResultScope(round.getFormat() != null && round.getFormat().requiresTeams()
                    ? PrizeResultScope.TEAM
                    : PrizeResultScope.PLAYER);
            schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
            prizeScheduleRepository.save(schedule);
        }
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
        if (FOUR_DAY_GAME_KEY.equals(schedule.getGameKey())) {
            return 0;
        }

        if (schedule.getRound() != null && schedule.getRound().getRoundNumber() != null) {
            return schedule.getRound().getRoundNumber() * 10;
        }

        return 9999;
    }

    private String buildRoundGameKey(Round round) {
        return "ROUND_" + round.getId();
    }

    private String buildRoundGameName(Round round) {
        String formatLabel = formatLabel(round.getFormat());
        Integer roundNumber = round.getRoundNumber();
        if (roundNumber == null) {
            return formatLabel;
        }
        return "Round " + roundNumber + " - " + formatLabel;
    }

    private String formatLabel(RoundFormat format) {
        if (format == null) {
            return "Game";
        }

        return switch (format) {
            case MIDDLE_MAN -> "Middle Man";
            case ONE_TWO_THREE -> "1-2-3";
            case TWO_MAN_LOW_NET -> "2-Man Low Net";
            case THREE_LOW_NET -> "3 Low Net";
            case TEAM_SCRAMBLE -> "Team Scramble";
            case STROKE_PLAY -> "Stroke Play";
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
}
