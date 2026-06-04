package com.myrtletrip.trip.repository;

import com.myrtletrip.trip.entity.TripPlannedRoundEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripPlannedRoundEventRepository extends JpaRepository<TripPlannedRoundEvent, Long> {
    List<TripPlannedRoundEvent> findByPlannedRound_IdOrderByEventOrderAsc(Long plannedRoundId);
    void deleteByPlannedRound_Trip_Id(Long tripId);
}
