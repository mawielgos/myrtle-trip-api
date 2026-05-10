package com.myrtletrip.tournament.repository;

import com.myrtletrip.tournament.entity.TripTournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripTournamentRoundRepository extends JpaRepository<TripTournamentRound, Long> {

    List<TripTournamentRound> findByTournament_IdOrderBySortOrderAsc(Long tournamentId);

    void deleteByTournament_Id(Long tournamentId);
}
