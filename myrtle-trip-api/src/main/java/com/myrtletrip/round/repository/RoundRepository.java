package com.myrtletrip.round.repository;

import com.myrtletrip.round.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {

    List<Round> findByTrip_IdOrderByRoundDateAsc(Long tripId);

    Optional<Round> findByTrip_IdAndRoundDate(Long tripId, LocalDate roundDate);

    long countByTrip_Id(Long tripId);

    long countByTrip_IdAndFinalizedTrue(Long tripId);

    @Modifying
    @Query("update Round r set r.defaultRoundTee = null where r.trip.id = :tripId")
    void clearDefaultRoundTeeByTripId(@Param("tripId") Long tripId);

    List<Round> findByTrip_IdOrderByRoundNumberAsc(Long tripId);

    void deleteByTrip_Id(Long tripId);

    List<Round> findByTripIdOrderById(Long tripId);
    
    List<Round> findByTripId(Long tripId);

}