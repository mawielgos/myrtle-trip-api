package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findByTrip_IdOrderByRoundDateAsc(Long tripId);

    Optional<Round> findByTrip_IdAndRoundDate(Long tripId, LocalDate roundDate);

    long countByTrip_Id(Long tripId);
}