package com.myrtletrip.trip.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import com.myrtletrip.trip.repository.TripPlannedRoundRepository;

@Service
public class TripPlannedRoundLifecycleService {

    private static final int DEFAULT_PLANNED_ROUND_COUNT = 5;

    private final TripPlannedRoundRepository tripPlannedRoundRepository;

    public TripPlannedRoundLifecycleService(TripPlannedRoundRepository tripPlannedRoundRepository) {
        this.tripPlannedRoundRepository = tripPlannedRoundRepository;
    }

    public int resolvePlannedRoundCount(Integer plannedRoundCount) {
        if (plannedRoundCount == null) {
            return DEFAULT_PLANNED_ROUND_COUNT;
        }
        return plannedRoundCount;
    }

    public void syncPlannedRoundsToTripRoundCount(Trip trip) {
        int targetCount = resolvePlannedRoundCount(trip.getPlannedRoundCount());
        List<TripPlannedRound> existingRounds = tripPlannedRoundRepository.findByTripOrderByRoundNumberAsc(trip);

        if (existingRounds.size() == targetCount) {
            return;
        }

        if (existingRounds.size() > targetCount) {
            for (int i = targetCount; i < existingRounds.size(); i++) {
                TripPlannedRound plannedRound = existingRounds.get(i);
                if (plannedRound.getRoundDate() != null
                        || plannedRound.getCourseId() != null
                        || plannedRound.getStandardTeeId() != null
                        || plannedRound.getFormat() != null
                        || Boolean.TRUE.equals(plannedRound.getIncludeInFourDayStandings())) {
                    throw new IllegalArgumentException(
                            "Cannot reduce planned round count because Round " + plannedRound.getRoundNumber()
                                    + " already has setup details. Clear that round first.");
                }
            }

            for (int i = existingRounds.size() - 1; i >= targetCount; i--) {
                tripPlannedRoundRepository.delete(existingRounds.get(i));
            }
            tripPlannedRoundRepository.flush();
            return;
        }

        for (int roundNumber = existingRounds.size() + 1; roundNumber <= targetCount; roundNumber++) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(roundNumber);
            plannedRound.setFormat(null);
            plannedRound.setIncludeInFourDayStandings(false);
            tripPlannedRoundRepository.save(plannedRound);
        }
        tripPlannedRoundRepository.flush();
    }

    public void createDefaultPlannedRounds(Trip trip) {
        if (tripPlannedRoundRepository.countByTrip(trip) > 0) {
            return;
        }

        int plannedRoundCount = resolvePlannedRoundCount(trip.getPlannedRoundCount());

        for (int roundNumber = 1; roundNumber <= plannedRoundCount; roundNumber++) {
            TripPlannedRound plannedRound = new TripPlannedRound();
            plannedRound.setTrip(trip);
            plannedRound.setRoundNumber(roundNumber);
            plannedRound.setFormat(null);
            plannedRound.setIncludeInFourDayStandings(false);
            tripPlannedRoundRepository.save(plannedRound);
        }
    }
}
