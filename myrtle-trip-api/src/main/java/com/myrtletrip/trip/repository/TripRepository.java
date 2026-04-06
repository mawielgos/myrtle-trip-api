package com.myrtletrip.trip.repository;

import com.myrtletrip.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByTripCode(String tripCode);
}