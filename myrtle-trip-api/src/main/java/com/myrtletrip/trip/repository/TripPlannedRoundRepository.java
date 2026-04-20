package com.myrtletrip.trip.repository;

import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlannedRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripPlannedRoundRepository extends JpaRepository<TripPlannedRound, Long> {

    List<TripPlannedRound> findByTripOrderByRoundNumberAsc(Trip trip);

    List<TripPlannedRound> findByTrip_IdOrderByRoundNumberAsc(Long tripId);

    void deleteByTrip(Trip trip);

    long countByTrip(Trip trip);
}
