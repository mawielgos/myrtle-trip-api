package com.myrtletrip.prize.repository;

import com.myrtletrip.prize.entity.TripPlayerPayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripPlayerPayoutStatusRepository extends JpaRepository<TripPlayerPayoutStatus, Long> {

    Optional<TripPlayerPayoutStatus> findByTrip_IdAndPlayer_Id(Long tripId, Long playerId);

    List<TripPlayerPayoutStatus> findByTrip_Id(Long tripId);
}
