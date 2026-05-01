package com.myrtletrip.trip.service;

import com.myrtletrip.trip.dto.SaveTripBillInventoryRequest;
import com.myrtletrip.trip.dto.TripBillInventoryResponse;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripBillInventory;
import com.myrtletrip.trip.repository.TripBillInventoryRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripBillInventoryService {

    private final TripRepository tripRepository;
    private final TripBillInventoryRepository tripBillInventoryRepository;

    public TripBillInventoryService(TripRepository tripRepository,
                                    TripBillInventoryRepository tripBillInventoryRepository) {
        this.tripRepository = tripRepository;
        this.tripBillInventoryRepository = tripBillInventoryRepository;
    }

    @Transactional(readOnly = true)
    public TripBillInventoryResponse getBillInventory(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        TripBillInventory inventory = tripBillInventoryRepository.findByTripId(tripId)
                .orElseGet(() -> createEmptyInventory(trip));

        return toResponse(inventory);
    }

    @Transactional
    public TripBillInventoryResponse saveBillInventory(Long tripId, SaveTripBillInventoryRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));

        TripBillInventory inventory = tripBillInventoryRepository.findByTripId(tripId)
                .orElseGet(() -> createEmptyInventory(trip));

        inventory.setHundredsCount(request.getHundredsCount());
        inventory.setFiftiesCount(request.getFiftiesCount());
        inventory.setTwentiesCount(request.getTwentiesCount());
        inventory.setTensCount(request.getTensCount());
        inventory.setFivesCount(request.getFivesCount());
        inventory.setOnesCount(request.getOnesCount());

        TripBillInventory saved = tripBillInventoryRepository.save(inventory);
        return toResponse(saved);
    }

    private TripBillInventory createEmptyInventory(Trip trip) {
        TripBillInventory inventory = new TripBillInventory();
        inventory.setTrip(trip);
        inventory.setHundredsCount(0);
        inventory.setFiftiesCount(0);
        inventory.setTwentiesCount(0);
        inventory.setTensCount(0);
        inventory.setFivesCount(0);
        inventory.setOnesCount(0);
        return inventory;
    }

    private TripBillInventoryResponse toResponse(TripBillInventory inventory) {
        TripBillInventoryResponse response = new TripBillInventoryResponse();
        response.setTripId(inventory.getTrip().getId());
        response.setHundredsCount(inventory.getHundredsCount());
        response.setFiftiesCount(inventory.getFiftiesCount());
        response.setTwentiesCount(inventory.getTwentiesCount());
        response.setTensCount(inventory.getTensCount());
        response.setFivesCount(inventory.getFivesCount());
        response.setOnesCount(inventory.getOnesCount());
        return response;
    }
}
