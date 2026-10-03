package com.myrtletrip.trip.service;

import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.prize.repository.PrizeWinningRepository;
import com.myrtletrip.round.repository.RoundCorrectionLogRepository;
import com.myrtletrip.round.repository.RoundGroupPlayerRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripStatus;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripStartResetService {

    private final TripRepository tripRepository;
    private final RoundRepository roundRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundGroupPlayerRepository roundGroupPlayerRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundCorrectionLogRepository roundCorrectionLogRepository;
    private final RoundTeeRepository roundTeeRepository;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final PrizeWinningRepository prizeWinningRepository;

    public TripStartResetService(
            TripRepository tripRepository,
            RoundRepository roundRepository,
            RoundGroupRepository roundGroupRepository,
            RoundGroupPlayerRepository roundGroupPlayerRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundCorrectionLogRepository roundCorrectionLogRepository,
            RoundTeeRepository roundTeeRepository,
            RoundTeeHoleRepository roundTeeHoleRepository,
            ScorecardRepository scorecardRepository,
            HoleScoreRepository holeScoreRepository,
            TeamHoleScoreRepository teamHoleScoreRepository,
            PrizeScheduleRepository prizeScheduleRepository,
            PrizeWinningRepository prizeWinningRepository
    ) {
        this.tripRepository = tripRepository;
        this.roundRepository = roundRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundGroupPlayerRepository = roundGroupPlayerRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundCorrectionLogRepository = roundCorrectionLogRepository;
        this.roundTeeRepository = roundTeeRepository;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.prizeWinningRepository = prizeWinningRepository;
    }

    @Transactional
    public void resetTripStart(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        if (!Boolean.TRUE.equals(trip.getInitialized()) && !TripStatus.IN_PROGRESS.equals(trip.getStatus())) {
            throw new IllegalStateException("Trip has not been started.");
        }

        if (TripStatus.COMPLETE.equals(trip.getStatus())) {
            throw new IllegalStateException("A completed trip cannot be reset to planning.");
        }

        if (roundRepository.countByTrip_IdAndFinalizedTrue(tripId) > 0) {
            throw new IllegalStateException("Cannot reset trip start after any round has been finalized.");
        }

        if (scorecardRepository.countByRound_Trip_IdAndGrossScoreIsNotNull(tripId) > 0
                || scorecardRepository.countByRound_Trip_IdAndAdjustedGrossScoreIsNotNull(tripId) > 0
                || scorecardRepository.countByRound_Trip_IdAndNetScoreIsNotNull(tripId) > 0
                || holeScoreRepository.countByScorecard_Round_Trip_IdAndStrokesIsNotNull(tripId) > 0
                || roundTeamRepository.countByRound_Trip_IdAndScrambleTotalScoreIsNotNull(tripId) > 0
                || teamHoleScoreRepository.countByRoundTeam_Round_Trip_Id(tripId) > 0) {
            throw new IllegalStateException("Cannot reset trip start after scoring has begun.");
        }

        if (roundCorrectionLogRepository.countByRound_Trip_Id(tripId) > 0) {
            throw new IllegalStateException("Cannot reset trip start after corrections have been recorded.");
        }

        prizeWinningRepository.deleteByTrip_Id(tripId);
        prizeScheduleRepository.clearRoundReferencesByTripId(tripId);

        teamHoleScoreRepository.deleteByRoundTeam_Round_Trip_Id(tripId);
        holeScoreRepository.deleteByScorecard_Round_Trip_Id(tripId);
        scorecardRepository.deleteByRound_Trip_Id(tripId);

        roundGroupPlayerRepository.deleteByRoundGroup_Round_Trip_Id(tripId);
        roundGroupRepository.deleteByRound_Trip_Id(tripId);

        roundTeamPlayerRepository.deleteByRoundTeam_Round_Trip_Id(tripId);
        roundTeamRepository.deleteByRound_Trip_Id(tripId);

        roundCorrectionLogRepository.deleteByRound_Trip_Id(tripId);
        roundRepository.clearDefaultRoundTeeByTripId(tripId);
        roundTeeHoleRepository.deleteByRoundTee_Round_Trip_Id(tripId);
        roundTeeRepository.deleteByRound_Trip_Id(tripId);
        roundRepository.deleteByTrip_Id(tripId);

        trip.setInitialized(false);
        trip.setStatus(TripStatus.PLANNING);
        tripRepository.save(trip);
    }
}
