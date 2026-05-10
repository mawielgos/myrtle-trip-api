package com.myrtletrip.prize.repository;

import com.myrtletrip.prize.entity.PrizeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrizeScheduleRepository extends JpaRepository<PrizeSchedule, Long> {

    List<PrizeSchedule> findByTrip_IdOrderByIdAsc(Long tripId);

    Optional<PrizeSchedule> findByTrip_IdAndGameKey(Long tripId, String gameKey);

    @Modifying
    @Query("update PrizeSchedule ps set ps.round = null where ps.trip.id = :tripId")
    void clearRoundReferencesByTripId(@Param("tripId") Long tripId);
}
