package com.myrtletrip.prize.repository;

import com.myrtletrip.prize.entity.PrizeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrizeScheduleRepository extends JpaRepository<PrizeSchedule, Long> {

    List<PrizeSchedule> findByTrip_IdOrderByIdAsc(Long tripId);

    Optional<PrizeSchedule> findByTrip_IdAndGameKey(Long tripId, String gameKey);
}
