package com.myrtletrip.trip.repository;

import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripPlayerRepository extends JpaRepository<TripPlayer, Long> {

    List<TripPlayer> findByTrip(Trip trip);

    List<TripPlayer> findByTripOrderByDisplayOrderAsc(Trip trip);

    List<TripPlayer> findByTrip_Id(Long tripId);

    List<TripPlayer> findByTrip_IdOrderByDisplayOrderAsc(Long tripId);

    long countByTrip(Trip trip);

    @Modifying
    @Query("delete from TripPlayer tp where tp.trip = :trip")
    void deleteByTrip(@Param("trip") Trip trip);
}
