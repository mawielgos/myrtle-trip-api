package com.myrtletrip.standings.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.standings.dto.FourDayStandingRoundResponse;
import com.myrtletrip.standings.dto.FourDayStandingRowResponse;
import com.myrtletrip.standings.dto.FourDayStandingsResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FourDayStandingsService {

    private static final String FOUR_DAY_GAME_KEY = "FOUR_DAY_INDIVIDUAL";

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;
    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;

    public FourDayStandingsService(TripRepository tripRepository,
                                   TripPlayerRepository tripPlayerRepository,
                                   TripPlannedRoundRepository tripPlannedRoundRepository,
                                   RoundRepository roundRepository,
                                   ScorecardRepository scorecardRepository,
                                   PrizeScheduleRepository prizeScheduleRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
    }

    @Transactional(readOnly = true)
    public FourDayStandingsResponse getFourDayStandings(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(tripId);
        List<TripPlannedRound> includedPlannedRounds = loadIncludedPlannedRounds(tripId);
        List<Round> eligibleRounds = loadEligibleRounds(tripId, includedPlannedRounds);

        FourDayStandingsResponse response = new FourDayStandingsResponse();
        response.setTripId(trip.getId());
        response.setTripName(trip.getName());
        response.setRequiredRounds(includedPlannedRounds.size());
        response.setCompletedRounds(eligibleRounds.size());
        response.setLeaderboardFinal(!includedPlannedRounds.isEmpty() && eligibleRounds.size() == includedPlannedRounds.size());

        List<String> roundLabels = new ArrayList<String>();
        for (Round round : eligibleRounds) {
            roundLabels.add("Rd " + round.getRoundNumber());
        }
        response.setRoundLabels(roundLabels);

        int leaderboardParTotal = 0;
        for (Round round : eligibleRounds) {
            leaderboardParTotal += getRoundPar(round);
        }
        response.setLeaderboardParTotal(leaderboardParTotal);

        Map<Long, PlayerAggregate> aggregateByPlayerId = new LinkedHashMap<Long, PlayerAggregate>();

        for (TripPlayer tripPlayer : tripPlayers) {
            Player player = tripPlayer.getPlayer();

            PlayerAggregate aggregate = new PlayerAggregate();
            aggregate.playerId = player.getId();
            aggregate.playerName = player.getDisplayName();
            aggregate.tripNumber = tripPlayer.getDisplayOrder();

            aggregateByPlayerId.put(player.getId(), aggregate);
        }

        for (Round round : eligibleRounds) {
            int roundPar = getRoundPar(round);
            List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());

            for (Scorecard scorecard : scorecards) {
                if (scorecard.getPlayer() == null || scorecard.getNetScore() == null) {
                    continue;
                }

                Long playerId = scorecard.getPlayer().getId();
                PlayerAggregate aggregate = aggregateByPlayerId.get(playerId);
                if (aggregate == null) {
                    continue;
                }

                FourDayStandingRoundResponse roundResponse = new FourDayStandingRoundResponse();
                roundResponse.setRoundId(round.getId());
                roundResponse.setRoundNumber(round.getRoundNumber());
                roundResponse.setLabel("Rd " + round.getRoundNumber());
                roundResponse.setScore(scorecard.getNetScore());
                roundResponse.setToPar(scorecard.getNetScore() - roundPar);

                aggregate.rounds.add(roundResponse);
                aggregate.totalScore += scorecard.getNetScore();
                aggregate.totalToPar += (scorecard.getNetScore() - roundPar);
                aggregate.completedRounds++;
            }
        }

        List<PlayerAggregate> ranked = new ArrayList<PlayerAggregate>(aggregateByPlayerId.values());
        ranked.removeIf(player -> player.completedRounds == 0);

        ranked.sort(
                Comparator.comparingInt((PlayerAggregate a) -> a.totalScore)
                        .thenComparingInt(a -> a.totalToPar)
                        .thenComparingInt(a -> a.tripNumber != null ? a.tripNumber : Integer.MAX_VALUE)
                        .thenComparing(a -> a.playerName != null ? a.playerName : "")
        );

        assignPositions(ranked);

        if (!includedPlannedRounds.isEmpty() && eligibleRounds.size() == includedPlannedRounds.size()) {
            applyPayouts(tripId, ranked);
        } else {
            clearPayouts(ranked);
        }

        List<FourDayStandingRowResponse> rows = new ArrayList<FourDayStandingRowResponse>();
        for (PlayerAggregate aggregate : ranked) {
            FourDayStandingRowResponse row = new FourDayStandingRowResponse();
            row.setPlayerId(aggregate.playerId);
            row.setTripNumber(aggregate.tripNumber);
            row.setPosition(aggregate.position);
            row.setPlayerName(aggregate.playerName);
            row.setTotalScore(aggregate.totalScore);
            row.setTotalToPar(aggregate.totalToPar);
            row.setMoney(aggregate.money);
            row.setRounds(aggregate.rounds);
            rows.add(row);
        }

        response.setRows(rows);
        return response;
    }

    private List<TripPlannedRound> loadIncludedPlannedRounds(Long tripId) {
        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        List<TripPlannedRound> included = new ArrayList<TripPlannedRound>();

        for (TripPlannedRound plannedRound : plannedRounds) {
            if (!Boolean.TRUE.equals(plannedRound.getIncludeInFourDayStandings())) {
                continue;
            }
            if (plannedRound.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
                continue;
            }
            included.add(plannedRound);
        }

        return included;
    }

    private List<Round> loadEligibleRounds(Long tripId, List<TripPlannedRound> includedPlannedRounds) {
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        Map<Integer, Round> finalizedByRoundNumber = new HashMap<Integer, Round>();

        for (Round round : rounds) {
            if (!Boolean.TRUE.equals(round.getFinalized())) {
                continue;
            }
            if (round.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
                continue;
            }
            finalizedByRoundNumber.put(round.getRoundNumber(), round);
        }

        List<Round> eligible = new ArrayList<Round>();
        for (TripPlannedRound plannedRound : includedPlannedRounds) {
            Round round = finalizedByRoundNumber.get(plannedRound.getRoundNumber());
            if (round != null) {
                eligible.add(round);
            }
        }

        return eligible;
    }

    private int getRoundPar(Round round) {
        RoundTee standardRoundTee = round.getStandardRoundTee();
        if (standardRoundTee == null || standardRoundTee.getParTotal() == null) {
            throw new IllegalStateException("Round " + round.getId() + " is missing standard round tee par total.");
        }
        return standardRoundTee.getParTotal();
    }

    private void assignPositions(List<PlayerAggregate> ranked) {
        Integer lastScore = null;
        Integer lastToPar = null;
        Integer lastPosition = null;

        for (int i = 0; i < ranked.size(); i++) {
            PlayerAggregate current = ranked.get(i);

            if (lastScore != null
                    && lastToPar != null
                    && current.totalScore == lastScore
                    && current.totalToPar == lastToPar) {
                current.position = lastPosition;
            } else {
                current.position = i + 1;
                lastPosition = current.position;
                lastScore = current.totalScore;
                lastToPar = current.totalToPar;
            }
        }
    }

    private void applyPayouts(Long tripId, List<PlayerAggregate> ranked) {
        clearPayouts(ranked);

        if (ranked.isEmpty()) {
            return;
        }

        PrizeSchedule schedule = prizeScheduleRepository.findByTrip_IdAndGameKey(tripId, FOUR_DAY_GAME_KEY)
                .orElse(null);

        if (schedule == null || schedule.getPayouts() == null || schedule.getPayouts().isEmpty()) {
            return;
        }

        Map<Integer, BigDecimal> payoutByPlace = new HashMap<Integer, BigDecimal>();
        for (PrizeSchedulePayout payout : schedule.getPayouts()) {
            if (payout.getFinishingPlace() == null || payout.getAmountPerPlayer() == null) {
                continue;
            }
            if (payout.getFinishingPlace() < 1) {
                continue;
            }
            payoutByPlace.put(payout.getFinishingPlace(), payout.getAmountPerPlayer());
        }

        int index = 0;
        while (index < ranked.size()) {
            int end = index;
            int position = ranked.get(index).position;

            while (end + 1 < ranked.size() && ranked.get(end + 1).position == position) {
                end++;
            }

            BigDecimal tiedPool = BigDecimal.ZERO;
            for (int place = position; place <= position + (end - index); place++) {
                BigDecimal placeAmount = payoutByPlace.get(place);
                if (placeAmount != null) {
                    tiedPool = tiedPool.add(placeAmount);
                }
            }

            if (tiedPool.signum() > 0) {
                BigDecimal split = tiedPool.divide(BigDecimal.valueOf(end - index + 1L), 2, RoundingMode.HALF_UP);
                for (int i = index; i <= end; i++) {
                    ranked.get(i).money = split;
                }
            }

            index = end + 1;
        }
    }

    private void clearPayouts(List<PlayerAggregate> ranked) {
        for (PlayerAggregate aggregate : ranked) {
            aggregate.money = null;
        }
    }

    private static class PlayerAggregate {
        private Long playerId;
        private Integer tripNumber;
        private String playerName;
        private int totalScore;
        private int totalToPar;
        private int completedRounds;
        private Integer position;
        private BigDecimal money;
        private List<FourDayStandingRoundResponse> rounds = new ArrayList<FourDayStandingRoundResponse>();
    }
}
