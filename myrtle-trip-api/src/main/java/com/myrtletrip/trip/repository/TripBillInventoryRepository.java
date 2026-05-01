package com.myrtletrip.trip.repository;

import com.myrtletrip.trip.entity.TripBillInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripBillInventoryRepository extends JpaRepository<TripBillInventory, Long> {

    Optional<TripBillInventory> findByTripId(Long tripId);
}
