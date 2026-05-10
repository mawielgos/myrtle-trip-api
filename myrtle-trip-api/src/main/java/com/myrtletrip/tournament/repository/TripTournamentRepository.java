package com.myrtletrip.tournament.repository;

import com.myrtletrip.tournament.entity.TripTournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripTournamentRepository extends JpaRepository<TripTournament, Long> {

    Optional<TripTournament> findByTrip_Id(Long tripId);
}
