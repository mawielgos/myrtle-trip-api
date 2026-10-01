package com.myrtletrip.trip.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripParticipationService {

    private final TripRepository tripRepository;
    private final TripPlayerRepository tripPlayerRepository;
    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;

    public TripParticipationService(TripRepository tripRepository,
                                    TripPlayerRepository tripPlayerRepository,
                                    RoundRepository roundRepository,
                                    ScorecardRepository scorecardRepository) {
        this.tripRepository = tripRepository;
        this.tripPlayerRepository = tripPlayerRepository;
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
    }

    @Transactional
    public void updateParticipation(Long tripId, Long playerId, String participationStatusText) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        TripPlayer tripPlayer = tripPlayerRepository.findByTrip_IdAndPlayer_Id(tripId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player is not on this event roster."));

        ScorecardParticipationStatus nextStatus = parseParticipationStatus(participationStatusText);
        tripPlayer.setParticipationStatus(nextStatus);
        tripPlayerRepository.save(tripPlayer);

        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        for (Round round : rounds) {
            if (round == null || round.getId() == null) {
                continue;
            }
            if (Boolean.TRUE.equals(round.getFinalized()) && !Boolean.TRUE.equals(trip.getCorrectionMode())) {
                continue;
            }
            Scorecard scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(round.getId(), playerId).orElse(null);
            if (scorecard == null) {
                continue;
            }
            scorecard.setParticipationStatus(nextStatus);
            scorecard.setWithdrawalHoleNumber(null);
            if (nextStatus != ScorecardParticipationStatus.ACTIVE) {
                scorecard.setTeam(null);
            }
            scorecardRepository.save(scorecard);
        }
    }

    public long countUnavailableRounds(Long tripId, Long playerId) {
        List<Round> rounds = roundRepository.findByTrip_IdOrderByRoundNumberAsc(tripId);
        long count = 0L;
        for (Round round : rounds) {
            if (round == null || round.getId() == null) {
                continue;
            }
            Scorecard scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(round.getId(), playerId).orElse(null);
            if (scorecard == null || scorecard.getParticipationStatus() == null) {
                continue;
            }
            if (scorecard.getParticipationStatus() != ScorecardParticipationStatus.ACTIVE) {
                count++;
            }
        }
        return count;
    }

    private ScorecardParticipationStatus parseParticipationStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ScorecardParticipationStatus.ACTIVE;
        }
        try {
            return ScorecardParticipationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported participation status: " + value);
        }
    }
}
