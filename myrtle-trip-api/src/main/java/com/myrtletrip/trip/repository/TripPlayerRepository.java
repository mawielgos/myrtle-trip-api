package com.myrtletrip.trip.repository;

import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripPlayerRepository extends JpaRepository<TripPlayer, Long> {

    List<TripPlayer> findByTrip_Id(Long tripId);

    List<TripPlayer> findByTrip(Trip trip);

    void deleteByTrip(Trip trip);
    List<TripPlayer> findByTripOrderByDisplayOrderAsc(Trip trip);

    long countByTrip(Trip trip);
}