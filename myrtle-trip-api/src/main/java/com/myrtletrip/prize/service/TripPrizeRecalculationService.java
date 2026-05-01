package com.myrtletrip.prize.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.dto.TeamGameResult;
import com.myrtletrip.games.service.RoundGameScoringService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.prize.dto.PrizePlayerTotalResponse;
import com.myrtletrip.prize.dto.PrizeRecalculationResponse;
import com.myrtletrip.prize.dto.PrizeWinningResponse;
import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import com.myrtletrip.prize.entity.PrizeWinning;
import com.myrtletrip.prize.entity.TripPlayerPayoutStatus;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.prize.repository.TripPlayerPayoutStatusRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.standings.dto.FourDayStandingRowResponse;
import com.myrtletrip.standings.dto.FourDayStandingsResponse;
import com.myrtletrip.standings.service.FourDayStandingsService;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TripPrizeRecalculationService {

    private static final String FOUR_DAY_GAME_KEY = "FOUR_DAY_INDIVIDUAL";

    private final TripRepository tripRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final PrizeWinningRepository prizeWinningRepository;
    private final RoundGameScoringService roundGameScoringService;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final ScorecardRepository scorecardRepository;
    private final FourDayStandingsService fourDayStandingsService;
    private final TripPrizeService tripPrizeService;
    private final TripPlayerPayoutStatusRepository tripPlayerPayoutStatusRepository;
    private final EntityManager entityManager;

    public TripPrizeRecalculationService(TripRepository tripRepository,
                                         PrizeScheduleRepository prizeScheduleRepository,
                                         PrizeWinningRepository prizeWinningRepository,
                                         RoundGameScoringService roundGameScoringService,
                                         RoundTeamPlayerRepository roundTeamPlayerRepository,
                                         ScorecardRepository scorecardRepository,
                                         FourDayStandingsService fourDayStandingsService,
                                         TripPrizeService tripPrizeService,
                                         TripPlayerPayoutStatusRepository tripPlayerPayoutStatusRepository,
                                         EntityManager entityManager) {
        this.tripRepository = tripRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.prizeWinningRepository = prizeWinningRepository;
        this.roundGameScoringService = roundGameScoringService;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
        this.fourDayStandingsService = fourDayStandingsService;
        this.tripPrizeService = tripPrizeService;
        this.tripPlayerPayoutStatusRepository = tripPlayerPayoutStatusRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public PrizeRecalculationResponse recalculate(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        tripPrizeService.getPrizeSchedules(tripId);

        prizeWinningRepository.deleteByTrip_Id(tripId);

        List<PrizeWinning> winnings = new ArrayList<PrizeWinning>();
        List<PrizeSchedule> schedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);
        schedules.sort(Comparator.comparing(this::sortValue));

        for (PrizeSchedule schedule : schedules) {
            if (schedule.getPayouts() == null || schedule.getPayouts().isEmpty()) {
                continue;
            }

            if (FOUR_DAY_GAME_KEY.equals(schedule.getGameKey())) {
                winnings.addAll(calculateFourDayWinnings(trip, schedule));
            } else if (schedule.getRound() != null) {
                winnings.addAll(calculateRoundWinnings(trip, schedule));
            }
        }

        prizeWinningRepository.saveAll(winnings);
        return buildResponse(tripId, winnings);
    }

    @Transactional
    public PrizeRecalculationResponse getCurrentWinnings(Long tripId) {
        List<PrizeWinning> winnings = prizeWinningRepository.findByTrip_IdOrderByGameKeyAscSourceRankAscPlayer_DisplayNameAsc(tripId);
        return buildResponse(tripId, winnings);
    }

    @Transactional
    public PrizeRecalculationResponse updatePlayerPayoutStatus(Long tripId, Long playerId, Boolean paid) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        Player player = entityManager.find(Player.class, playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found: " + playerId);
        }

        TripPlayerPayoutStatus status = tripPlayerPayoutStatusRepository
                .findByTrip_IdAndPlayer_Id(tripId, playerId)
                .orElseGet(() -> {
                    TripPlayerPayoutStatus created = new TripPlayerPayoutStatus();
                    created.setTrip(trip);
                    created.setPlayer(player);
                    return created;
                });

        boolean isPaid = Boolean.TRUE.equals(paid);
        status.setPaid(isPaid);
        status.setPaidAt(isPaid ? LocalDateTime.now() : null);
        tripPlayerPayoutStatusRepository.save(status);

        return getCurrentWinnings(tripId);
    }

    private List<PrizeWinning> calculateFourDayWinnings(Trip trip, PrizeSchedule schedule) {
        FourDayStandingsResponse standings = fourDayStandingsService.getFourDayStandings(trip.getId());
        List<PrizeUnit> units = new ArrayList<PrizeUnit>();

        if (!Boolean.TRUE.equals(standings.getLeaderboardFinal())) {
            return new ArrayList<PrizeWinning>();
        }

        for (FourDayStandingRowResponse row : standings.getRows()) {
            if (row.getPosition() == null || row.getPlayerId() == null) {
                continue;
            }

            PrizeUnit unit = new PrizeUnit();
            unit.rank = row.getPosition();
            unit.sourceName = row.getPlayerName();

            PrizeRecipient recipient = new PrizeRecipient();
            recipient.playerId = row.getPlayerId();
            unit.recipients.add(recipient);

            units.add(unit);
        }

        return buildWinningsFromUnits(trip, schedule, null, units);
    }

    private List<PrizeWinning> calculateRoundWinnings(Trip trip, PrizeSchedule schedule) {
        Round round = schedule.getRound();
        if (round == null || !Boolean.TRUE.equals(round.getFinalized())) {
            return new ArrayList<PrizeWinning>();
        }

        if (schedule.getResultScope() != null && "PLAYER".equals(schedule.getResultScope().name())) {
            return buildWinningsFromUnits(trip, schedule, round, buildIndividualRoundUnits(round));
        }

        RoundGameResult result = roundGameScoringService.getRoundResult(round.getId());
        List<PrizeUnit> units = new ArrayList<PrizeUnit>();

        for (TeamGameResult teamResult : result.getTeams()) {
            if (teamResult.getPlacement() == null || teamResult.getTeamId() == null) {
                continue;
            }

            PrizeUnit unit = new PrizeUnit();
            unit.rank = teamResult.getPlacement();
            unit.sourceName = teamResult.getTeamName();

            List<RoundTeamPlayer> teamPlayers = roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(teamResult.getTeamId());
            for (RoundTeamPlayer teamPlayer : teamPlayers) {
                if (teamPlayer.getPlayer() == null) {
                    continue;
                }
                PrizeRecipient recipient = new PrizeRecipient();
                recipient.playerId = teamPlayer.getPlayer().getId();
                unit.recipients.add(recipient);
            }

            units.add(unit);
        }

        if (units.isEmpty()) {
            units = buildIndividualRoundUnits(round);
        }

        return buildWinningsFromUnits(trip, schedule, round, units);
    }

    private List<PrizeUnit> buildIndividualRoundUnits(Round round) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(round.getId());
        List<Scorecard> completed = new ArrayList<Scorecard>();

        for (Scorecard scorecard : scorecards) {
            if (scorecard.getPlayer() != null && scorecard.getNetScore() != null) {
                completed.add(scorecard);
            }
        }

        completed.sort(
                Comparator.comparing(Scorecard::getNetScore, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(scorecard -> scorecard.getPlayer().getDisplayName(), String.CASE_INSENSITIVE_ORDER)
        );

        List<PrizeUnit> units = new ArrayList<PrizeUnit>();
        Integer previousNet = null;
        Integer previousRank = null;

        for (int i = 0; i < completed.size(); i++) {
            Scorecard scorecard = completed.get(i);
            Integer currentRank;
            if (previousNet != null && previousNet.equals(scorecard.getNetScore())) {
                currentRank = previousRank;
            } else {
                currentRank = i + 1;
                previousRank = currentRank;
                previousNet = scorecard.getNetScore();
            }

            Player player = scorecard.getPlayer();
            PrizeUnit unit = new PrizeUnit();
            unit.rank = currentRank;
            unit.sourceName = player.getDisplayName();

            PrizeRecipient recipient = new PrizeRecipient();
            recipient.playerId = player.getId();
            unit.recipients.add(recipient);

            units.add(unit);
        }

        return units;
    }

    private List<PrizeWinning> buildWinningsFromUnits(Trip trip,
                                                      PrizeSchedule schedule,
                                                      Round round,
                                                      List<PrizeUnit> units) {
        List<PrizeWinning> winnings = new ArrayList<PrizeWinning>();
        if (units.isEmpty()) {
            return winnings;
        }

        units.sort(
                Comparator.comparingInt((PrizeUnit unit) -> unit.rank)
                        .thenComparing(unit -> unit.sourceName != null ? unit.sourceName : "")
        );

        Map<Integer, BigDecimal> payoutByPlace = buildPayoutByPlace(schedule);

        int index = 0;
        while (index < units.size()) {
            int end = index;
            int rank = units.get(index).rank;

            while (end + 1 < units.size() && units.get(end + 1).rank == rank) {
                end++;
            }

            BigDecimal tiedPool = BigDecimal.ZERO;
            for (int place = rank; place <= rank + (end - index); place++) {
                BigDecimal placeAmount = payoutByPlace.get(place);
                if (placeAmount != null) {
                    tiedPool = tiedPool.add(placeAmount);
                }
            }

            if (tiedPool.signum() > 0) {
                BigDecimal amountPerRecipient = tiedPool
                        .divide(BigDecimal.valueOf(end - index + 1L), 2, RoundingMode.HALF_UP);

                for (int unitIndex = index; unitIndex <= end; unitIndex++) {
                    PrizeUnit unit = units.get(unitIndex);
                    for (PrizeRecipient recipient : unit.recipients) {
                        PrizeWinning winning = new PrizeWinning();
                        winning.setTrip(trip);
                        winning.setRound(round);
                        winning.setPrizeSchedule(schedule);
                        winning.setGameKey(schedule.getGameKey());
                        winning.setGameName(schedule.getGameName());
                        winning.setSourceRank(unit.rank);
                        winning.setSourceName(unit.sourceName);
                        winning.setAmount(amountPerRecipient);

                        winning.setPlayer(entityManager.getReference(Player.class, recipient.playerId));

                        winnings.add(winning);
                    }
                }
            }

            index = end + 1;
        }

        return winnings;
    }

    private Map<Integer, BigDecimal> buildPayoutByPlace(PrizeSchedule schedule) {
        Map<Integer, BigDecimal> payoutByPlace = new HashMap<Integer, BigDecimal>();
        for (PrizeSchedulePayout payout : schedule.getPayouts()) {
            if (payout.getFinishingPlace() == null || payout.getAmountPerPlayer() == null) {
                continue;
            }
            payoutByPlace.put(payout.getFinishingPlace(), payout.getAmountPerPlayer());
        }
        return payoutByPlace;
    }

    private PrizeRecalculationResponse buildResponse(Long tripId, List<PrizeWinning> winnings) {
        PrizeRecalculationResponse response = new PrizeRecalculationResponse();
        response.setTripId(tripId);

        List<PrizeWinningResponse> winningResponses = new ArrayList<PrizeWinningResponse>();
        Map<Long, PrizePlayerTotalResponse> totalByPlayerId = new LinkedHashMap<Long, PrizePlayerTotalResponse>();
        Map<Long, TripPlayerPayoutStatus> payoutStatusByPlayerId = new HashMap<Long, TripPlayerPayoutStatus>();
        List<TripPlayerPayoutStatus> payoutStatuses = tripPlayerPayoutStatusRepository.findByTrip_Id(tripId);
        for (TripPlayerPayoutStatus payoutStatus : payoutStatuses) {
            if (payoutStatus.getPlayer() != null) {
                payoutStatusByPlayerId.put(payoutStatus.getPlayer().getId(), payoutStatus);
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PrizeWinning winning : winnings) {
            BigDecimal amount = winning.getAmount() == null ? BigDecimal.ZERO : winning.getAmount();
            totalAmount = totalAmount.add(amount);

            PrizeWinningResponse winningResponse = new PrizeWinningResponse();
            winningResponse.setWinningId(winning.getId());
            winningResponse.setTripId(tripId);
            winningResponse.setPlayerId(winning.getPlayer().getId());
            winningResponse.setPlayerName(winning.getPlayer().getDisplayName());
            winningResponse.setGameKey(winning.getGameKey());
            winningResponse.setGameName(winning.getGameName());
            winningResponse.setSourceRank(winning.getSourceRank());
            winningResponse.setSourceName(winning.getSourceName());
            winningResponse.setAmount(amount);
            if (winning.getRound() != null) {
                winningResponse.setRoundId(winning.getRound().getId());
                winningResponse.setRoundNumber(winning.getRound().getRoundNumber());
            }
            winningResponses.add(winningResponse);

            Long playerId = winning.getPlayer().getId();
            PrizePlayerTotalResponse playerTotal = totalByPlayerId.get(playerId);
            if (playerTotal == null) {
                playerTotal = new PrizePlayerTotalResponse();
                playerTotal.setPlayerId(playerId);
                playerTotal.setPlayerName(winning.getPlayer().getDisplayName());
                playerTotal.setTotalAmount(BigDecimal.ZERO);

                TripPlayerPayoutStatus payoutStatus = payoutStatusByPlayerId.get(playerId);
                if (payoutStatus != null) {
                    playerTotal.setPaid(Boolean.TRUE.equals(payoutStatus.getPaid()));
                    playerTotal.setPaidAt(payoutStatus.getPaidAt());
                } else {
                    playerTotal.setPaid(Boolean.FALSE);
                    playerTotal.setPaidAt(null);
                }

                totalByPlayerId.put(playerId, playerTotal);
            }
            playerTotal.setTotalAmount(playerTotal.getTotalAmount().add(amount));
        }

        List<PrizePlayerTotalResponse> playerTotals = new ArrayList<PrizePlayerTotalResponse>(totalByPlayerId.values());
        playerTotals.sort(
                Comparator.comparing(PrizePlayerTotalResponse::getTotalAmount, Comparator.reverseOrder())
                        .thenComparing(PrizePlayerTotalResponse::getPlayerName, String.CASE_INSENSITIVE_ORDER)
        );

        response.setTotalAmount(totalAmount);
        response.setWinnings(winningResponses);
        response.setPlayerTotals(playerTotals);
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

    private static class PrizeUnit {
        private Integer rank;
        private String sourceName;
        private List<PrizeRecipient> recipients = new ArrayList<PrizeRecipient>();
    }

    private static class PrizeRecipient {
        private Long playerId;
    }
}
