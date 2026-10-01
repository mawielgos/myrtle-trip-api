package com.myrtletrip.trip.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.prize.repository.TripPlayerPayoutStatusRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import com.myrtletrip.tournament.repository.TripTournamentRepository;
import com.myrtletrip.tournament.repository.TripTournamentRoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripBillInventoryRepository;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;

@Service
public class TripLifecycleService {

    private static final String GHIN_FROZEN = "GHIN_FROZEN";

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final RoundRepository roundRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;
    private final PrizeWinningRepository prizeWinningRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final TripPlayerPayoutStatusRepository tripPlayerPayoutStatusRepository;
    private final TripBillInventoryRepository tripBillInventoryRepository;
    private final TripTournamentRepository tripTournamentRepository;
    private final TripTournamentRoundRepository tripTournamentRoundRepository;
    private final TripPlannedRoundRepository tripPlannedRoundRepository;

    public TripLifecycleService(TripRepository tripRepository,
                                TripPlayerRepository tripPlayerRepository,
                                RoundRepository roundRepository,
                                ScoreHistoryEntryRepository scoreHistoryEntryRepository,
                                PrizeWinningRepository prizeWinningRepository,
                                PrizeScheduleRepository prizeScheduleRepository,
                                TripPlayerPayoutStatusRepository tripPlayerPayoutStatusRepository,
                                TripBillInventoryRepository tripBillInventoryRepository,
                                TripTournamentRepository tripTournamentRepository,
                                TripTournamentRoundRepository tripTournamentRoundRepository,
                                TripPlannedRoundRepository tripPlannedRoundRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.roundRepository = roundRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
        this.prizeWinningRepository = prizeWinningRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.tripPlayerPayoutStatusRepository = tripPlayerPayoutStatusRepository;
        this.tripBillInventoryRepository = tripBillInventoryRepository;
        this.tripTournamentRepository = tripTournamentRepository;
        this.tripTournamentRoundRepository = tripTournamentRoundRepository;
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
    }

    @Transactional
    public void archiveTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (Boolean.TRUE.equals(trip.getArchived())) {
            return;
        }

        trip.setArchived(Boolean.TRUE);
        trip.setArchivedAt(LocalDateTime.now());
        tripRepository.save(trip);
    }

    @Transactional
    public void restoreTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (!Boolean.TRUE.equals(trip.getArchived())) {
            return;
        }

        trip.setArchived(Boolean.FALSE);
        trip.setArchivedAt(null);
        tripRepository.save(trip);
    }

    @Transactional
    public void deleteTrip(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        long startedRoundCount = roundRepository.countByTrip_Id(tripId);
        if (startedRoundCount > 0L) {
            throw new IllegalStateException("Only trips with no started rounds can be deleted.");
        }

        List<TripPlayer> tripPlayers = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);
        String handicapGroupCode = trip.getTripCode();

        if (handicapGroupCode != null && !handicapGroupCode.isBlank()) {
            for (TripPlayer tripPlayer : tripPlayers) {
                if (tripPlayer == null || tripPlayer.getPlayer() == null) {
                    continue;
                }

                scoreHistoryEntryRepository.deleteByPlayerAndHandicapGroupCodeAndSourceType(
                        tripPlayer.getPlayer(),
                        handicapGroupCode,
                        GHIN_FROZEN
                );
            }
        }

        prizeWinningRepository.deleteByTrip_Id(tripId);
        tripPlayerPayoutStatusRepository.deleteByTrip_Id(tripId);
        tripBillInventoryRepository.deleteByTripId(tripId);

        tripTournamentRepository.findByTrip_Id(tripId).ifPresent(tournament -> {
            tripTournamentRoundRepository.deleteByTournament_Id(tournament.getId());
            tripTournamentRepository.delete(tournament);
        });

        prizeScheduleRepository.deleteAll(prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId));
        prizeScheduleRepository.flush();

        List<TripPlannedRound> plannedRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);
        if (!plannedRounds.isEmpty()) {
            tripPlannedRoundRepository.deleteAll(plannedRounds);
            tripPlannedRoundRepository.flush();
        }

        if (!tripPlayers.isEmpty()) {
            tripPlayerRepository.deleteAll(tripPlayers);
            tripPlayerRepository.flush();
        }

        tripRepository.delete(trip);
        tripRepository.flush();
    }
}
