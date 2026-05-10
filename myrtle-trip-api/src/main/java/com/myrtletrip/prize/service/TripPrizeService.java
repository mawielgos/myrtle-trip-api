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
import com.myrtletrip.tournament.entity.TripTournament;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
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

    private static final String TOURNAMENT_GAME_KEY = "FOUR_DAY_INDIVIDUAL";

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final PrizeSchedulePayoutRepository prizeSchedulePayoutRepository;
    private final TripTournamentRepository tripTournamentRepository;
    private final TripEditingGuardService tripEditingGuardService;

    public TripPrizeService(TripRepository tripRepository,
                            RoundRepository roundRepository,
                            TripPlannedRoundRepository tripPlannedRoundRepository,
                            PrizeScheduleRepository prizeScheduleRepository,
                            PrizeSchedulePayoutRepository prizeSchedulePayoutRepository,
                            TripTournamentRepository tripTournamentRepository,
                            TripEditingGuardService tripEditingGuardService) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
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

    @Transactional
    public List<PrizeScheduleResponse> savePrizeSchedules(Long tripId, SaveTripPrizeSchedulesRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        tripEditingGuardService.assertStructureEditable(trip);

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

        List<PrizeSchedule> existingSchedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);

        String tournamentPrizeName = resolveTournamentPrizeName(tripId);
        PrizeSchedule tournamentSchedule = findByGameKey(existingSchedules, TOURNAMENT_GAME_KEY);
        if (tournamentSchedule == null) {
            tournamentSchedule = new PrizeSchedule();
            tournamentSchedule.setTrip(trip);
            tournamentSchedule.setGameKey(TOURNAMENT_GAME_KEY);
            tournamentSchedule.setGameName(tournamentPrizeName);
            tournamentSchedule.setResultScope(PrizeResultScope.PLAYER);
            tournamentSchedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
            prizeScheduleRepository.save(tournamentSchedule);
            existingSchedules.add(tournamentSchedule);
        } else if (isLegacyTournamentGameName(tournamentSchedule.getGameName())) {
            tournamentSchedule.setGameName(tournamentPrizeName);
            prizeScheduleRepository.save(tournamentSchedule);
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
            if (plannedRound == null || plannedRound.getRoundNumber() == null || plannedRound.getFormat() == null) {
                continue;
            }

            String plannedGameKey = buildPlannedRoundGameKey(plannedRound);
            Round matchingRound = roundsByNumber.get(plannedRound.getRoundNumber());
            PrizeSchedule schedule = findRoundSchedule(existingSchedules, plannedRound.getRoundNumber(), matchingRound, plannedGameKey);

            if (schedule == null) {
                schedule = new PrizeSchedule();
                schedule.setTrip(trip);
                schedule.setGameKey(plannedGameKey);
                schedule.setGameName(buildPlannedRoundGameName(plannedRound));
                schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
                existingSchedules.add(schedule);
            }

            if (matchingRound != null && schedule.getRound() == null) {
                schedule.setRound(matchingRound);
            }

            RoundFormat format = matchingRound != null && matchingRound.getFormat() != null
                    ? matchingRound.getFormat()
                    : plannedRound.getFormat();
            schedule.setResultScope(format != null && format.requiresTeams()
                    ? PrizeResultScope.TEAM
                    : PrizeResultScope.PLAYER);

            if (schedule.getGameName() == null || schedule.getGameName().isBlank() || isDefaultRoundGameName(schedule.getGameName())) {
                schedule.setGameName(buildPlannedRoundGameName(plannedRound));
            }

            prizeScheduleRepository.save(schedule);
        }

        for (Round round : rounds) {
            if (round.getRoundNumber() == null || round.getFormat() == null) {
                continue;
            }

            String gameKey = buildRoundGameKey(round);
            PrizeSchedule schedule = findRoundSchedule(existingSchedules, round.getRoundNumber(), round, gameKey);
            if (schedule != null) {
                if (schedule.getRound() == null) {
                    schedule.setRound(round);
                    prizeScheduleRepository.save(schedule);
                }
                continue;
            }

            schedule = new PrizeSchedule();
            schedule.setTrip(trip);
            schedule.setRound(round);
            schedule.setGameKey(gameKey);
            schedule.setGameName(buildRoundGameName(round));
            schedule.setResultScope(round.getFormat() != null && round.getFormat().requiresTeams()
                    ? PrizeResultScope.TEAM
                    : PrizeResultScope.PLAYER);
            schedule.setPayoutUnit(PrizePayoutUnit.PLAYER);
            prizeScheduleRepository.save(schedule);
            existingSchedules.add(schedule);
        }
    }

    private String resolveTournamentPrizeName(Long tripId) {
        TripTournament tournament = tripTournamentRepository.findByTrip_Id(tripId).orElse(null);
        if (tournament != null && tournament.getName() != null && !tournament.getName().isBlank()) {
            return tournament.getName().trim();
        }
        return "Multi-Round Tournament";
    }

    private boolean isLegacyTournamentGameName(String gameName) {
        if (gameName == null || gameName.isBlank()) {
            return true;
        }
        String normalized = gameName.trim();
        return "Multi-Round Individual Low Net".equalsIgnoreCase(normalized)
                || "Four Day Individual Low Net".equalsIgnoreCase(normalized)
                || "Multi-Round Tournament".equalsIgnoreCase(normalized)
                || "Tournament Standings".equalsIgnoreCase(normalized);
    }

    private PrizeSchedule findByGameKey(List<PrizeSchedule> schedules, String gameKey) {
        for (PrizeSchedule schedule : schedules) {
            if (schedule != null && gameKey.equals(schedule.getGameKey())) {
                return schedule;
            }
        }
        return null;
    }

    private PrizeSchedule findRoundSchedule(List<PrizeSchedule> schedules,
                                            Integer roundNumber,
                                            Round round,
                                            String preferredGameKey) {
        PrizeSchedule byPreferredKey = findByGameKey(schedules, preferredGameKey);
        if (byPreferredKey != null) {
            return byPreferredKey;
        }

        if (round != null && round.getId() != null) {
            for (PrizeSchedule schedule : schedules) {
                if (schedule != null && schedule.getRound() != null && round.getId().equals(schedule.getRound().getId())) {
                    return schedule;
                }
            }
        }

        for (PrizeSchedule schedule : schedules) {
            if (schedule == null || TOURNAMENT_GAME_KEY.equals(schedule.getGameKey())) {
                continue;
            }
            if (schedule.getRound() != null && roundNumber.equals(schedule.getRound().getRoundNumber())) {
                return schedule;
            }
            if (schedule.getGameKey() != null && schedule.getGameKey().equals("ROUND_NUMBER_" + roundNumber)) {
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
        if (TOURNAMENT_GAME_KEY.equals(schedule.getGameKey())) {
            response.setGameName(resolveTournamentPrizeName(schedule.getTrip().getId()));
        } else {
            response.setGameName(schedule.getGameName());
        }
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
        if (TOURNAMENT_GAME_KEY.equals(schedule.getGameKey())) {
            return 0;
        }

        if (schedule.getRound() != null && schedule.getRound().getRoundNumber() != null) {
            return schedule.getRound().getRoundNumber() * 10;
        }

        return 9999;
    }

    private String buildRoundGameKey(Round round) {
        if (round.getRoundNumber() != null) {
            return "ROUND_NUMBER_" + round.getRoundNumber();
        }
        return "ROUND_" + round.getId();
    }

    private String buildPlannedRoundGameKey(TripPlannedRound plannedRound) {
        return "ROUND_NUMBER_" + plannedRound.getRoundNumber();
    }

    private String buildRoundGameName(Round round) {
        return buildRoundGameName(round.getRoundNumber(), round.getFormat(), round.getScrambleTeamSize());
    }

    private String buildPlannedRoundGameName(TripPlannedRound plannedRound) {
        return buildRoundGameName(plannedRound.getRoundNumber(), plannedRound.getFormat(), plannedRound.getScrambleTeamSize());
    }

    private String buildRoundGameName(Integer roundNumber, RoundFormat format) {
        return buildRoundGameName(roundNumber, format, null);
    }

    private String buildRoundGameName(Integer roundNumber, RoundFormat format, Integer scrambleTeamSize) {
        String formatLabel = formatLabel(format, scrambleTeamSize);
        if (roundNumber == null) {
            return formatLabel;
        }
        return "Round " + roundNumber + " - " + formatLabel;
    }

    private String formatLabel(RoundFormat format) {
        return formatLabel(format, null);
    }

    private String formatLabel(RoundFormat format, Integer scrambleTeamSize) {
        if (format == null) {
            return "Game";
        }

        return switch (format) {
            case MIDDLE_MAN -> "Middle Man";
            case ONE_TWO_THREE -> "1-2-3";
            case TWO_MAN_LOW_NET -> "2-Man Low Net";
            case THREE_LOW_NET -> "3 Low Net";
            case TEAM_SCRAMBLE -> (scrambleTeamSize == null ? 4 : scrambleTeamSize) + "-Person Scramble";
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
