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
import com.myrtletrip.standings.dto.TournamentStandingRoundResponse;
import com.myrtletrip.standings.dto.TournamentStandingRowResponse;
import com.myrtletrip.standings.dto.TournamentStandingsResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.tournament.service.TripTournamentService;
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
public class TournamentStandingsService {

    private static final String TOURNAMENT_GAME_KEY = "FOUR_DAY_INDIVIDUAL";

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final TripTournamentService tripTournamentService;
    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;

    public TournamentStandingsService(TripRepository tripRepository,
                                   TripPlayerRepository tripPlayerRepository,
                                   TripTournamentService tripTournamentService,
                                   RoundRepository roundRepository,
                                   ScorecardRepository scorecardRepository,
                                   PrizeScheduleRepository prizeScheduleRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.tripTournamentService = tripTournamentService;
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
    }

    @Transactional(readOnly = true)
    public TournamentStandingsResponse getTournamentStandings(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTrip_IdOrderByDisplayOrderAsc(tripId);
        List<TripPlannedRound> includedPlannedRounds = loadIncludedPlannedRounds(tripId);
        List<Round> eligibleRounds = loadEligibleRounds(tripId, includedPlannedRounds);

        TournamentStandingsResponse response = new TournamentStandingsResponse();
        response.setTripId(trip.getId());
        response.setTripName(trip.getName());
        response.setTournamentName(tripTournamentService.getTournamentName(tripId));
        response.setStandingsLabel(tripTournamentService.getStandingsLabel(tripId));
        int requiredRoundCount = includedPlannedRounds.size();
        response.setRequiredRounds(requiredRoundCount);
        response.setCompletedRounds(eligibleRounds.size());

        List<String> roundLabels = new ArrayList<String>();
        for (Round round : eligibleRounds) {
            roundLabels.add(buildRoundLabel(round));
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

                TournamentStandingRoundResponse roundResponse = new TournamentStandingRoundResponse();
                roundResponse.setRoundId(round.getId());
                roundResponse.setRoundNumber(round.getRoundNumber());
                roundResponse.setLabel(buildRoundLabel(round));
                roundResponse.setScore(scorecard.getNetScore());
                roundResponse.setToPar(scorecard.getNetScore() - roundPar);

                aggregate.rounds.add(roundResponse);
                aggregate.totalScore += scorecard.getNetScore();
                aggregate.totalToPar += (scorecard.getNetScore() - roundPar);
                aggregate.completedRounds++;
            }
        }

        int currentlyExpectedRoundCount = eligibleRounds.size();
        boolean allTournamentRoundsFinalized = requiredRoundCount > 0 && currentlyExpectedRoundCount == requiredRoundCount;

        List<PlayerAggregate> ranked = new ArrayList<PlayerAggregate>(aggregateByPlayerId.values());
        for (PlayerAggregate player : ranked) {
            player.currentLeaderboardComplete = currentlyExpectedRoundCount > 0 && player.completedRounds == currentlyExpectedRoundCount;
            player.tournamentComplete = allTournamentRoundsFinalized && player.completedRounds == requiredRoundCount;
        }

        ranked.sort(
                Comparator.comparing((PlayerAggregate a) -> !a.currentLeaderboardComplete)
                        .thenComparingInt((PlayerAggregate a) -> -a.completedRounds)
                        .thenComparingInt((PlayerAggregate a) -> a.totalScore)
                        .thenComparingInt(a -> a.totalToPar)
                        .thenComparingInt(a -> a.tripNumber != null ? a.tripNumber : Integer.MAX_VALUE)
                        .thenComparing(a -> a.playerName != null ? a.playerName : "")
        );

        boolean leaderboardFinal = allTournamentRoundsFinalized && allPlayersTournamentComplete(ranked, requiredRoundCount);
        response.setLeaderboardFinal(leaderboardFinal);

        List<PlayerAggregate> payoutEligibleRanked = new ArrayList<PlayerAggregate>();
        for (PlayerAggregate player : ranked) {
            if (player.completedRounds > 0) {
                payoutEligibleRanked.add(player);
            }
        }

        assignPositions(payoutEligibleRanked, currentlyExpectedRoundCount);

        if (leaderboardFinal) {
            applyPayouts(tripId, payoutEligibleRanked);
        } else {
            clearPayouts(ranked);
        }

        List<TournamentStandingRowResponse> rows = new ArrayList<TournamentStandingRowResponse>();
        for (PlayerAggregate aggregate : ranked) {
            TournamentStandingRowResponse row = new TournamentStandingRowResponse();
            row.setPlayerId(aggregate.playerId);
            row.setTripNumber(aggregate.tripNumber);
            row.setPosition(aggregate.position);
            row.setPlayerName(aggregate.playerName);
            row.setTotalScore(aggregate.totalScore);
            row.setTotalToPar(aggregate.completedRounds > 0 ? aggregate.totalToPar : null);
            row.setCompletedRounds(aggregate.completedRounds);
            row.setTournamentComplete(aggregate.tournamentComplete);
            row.setMoney(aggregate.money);
            row.setRounds(aggregate.rounds);
            rows.add(row);
        }

        response.setRows(rows);
        return response;
    }

    private List<TripPlannedRound> loadIncludedPlannedRounds(Long tripId) {
        return tripTournamentService.getIncludedTournamentPlannedRounds(tripId);
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
        RoundTee roundTee = round.getDefaultRoundTee();
        if (roundTee == null) {
            roundTee = round.getStandardRoundTee();
        }
        if (roundTee == null || roundTee.getParTotal() == null) {
            throw new IllegalStateException("Round " + round.getId() + " is missing default round tee par total.");
        }
        return roundTee.getParTotal();
    }

    private String buildRoundLabel(Round round) {
        StringBuilder label = new StringBuilder();
        label.append("Rd ");
        label.append(round != null && round.getRoundNumber() != null ? round.getRoundNumber() : "?");

        if (round != null && round.getRoundDate() != null) {
            label.append(" • ").append(round.getRoundDate());
        }

        if (round != null && round.getCourse() != null
                && round.getCourse().getName() != null
                && !round.getCourse().getName().isBlank()) {
            label.append(" • ").append(round.getCourse().getName());
        }

        return label.toString();
    }

    private boolean allPlayersTournamentComplete(List<PlayerAggregate> ranked, int requiredRoundCount) {
        if (requiredRoundCount <= 0 || ranked.isEmpty()) {
            return false;
        }

        for (PlayerAggregate player : ranked) {
            if (player.completedRounds != requiredRoundCount) {
                return false;
            }
        }
        return true;
    }

    private void assignPositions(List<PlayerAggregate> ranked, int expectedRoundCount) {
        Integer lastScore = null;
        Integer lastToPar = null;
        Integer lastPosition = null;
        int rankedIndex = 0;

        for (PlayerAggregate current : ranked) {
            current.position = null;

            if (expectedRoundCount <= 0 || current.completedRounds != expectedRoundCount) {
                continue;
            }

            if (lastScore != null
                    && lastToPar != null
                    && current.totalScore == lastScore
                    && current.totalToPar == lastToPar) {
                current.position = lastPosition;
            } else {
                current.position = rankedIndex + 1;
                lastPosition = current.position;
                lastScore = current.totalScore;
                lastToPar = current.totalToPar;
            }
            rankedIndex++;
        }
    }

    private void applyPayouts(Long tripId, List<PlayerAggregate> ranked) {
        clearPayouts(ranked);

        if (ranked.isEmpty()) {
            return;
        }

        PrizeSchedule schedule = prizeScheduleRepository.findByTrip_IdAndGameKey(tripId, TOURNAMENT_GAME_KEY)
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
        private boolean currentLeaderboardComplete;
        private boolean tournamentComplete;
        private Integer position;
        private BigDecimal money;
        private List<TournamentStandingRoundResponse> rounds = new ArrayList<TournamentStandingRoundResponse>();
    }
}
