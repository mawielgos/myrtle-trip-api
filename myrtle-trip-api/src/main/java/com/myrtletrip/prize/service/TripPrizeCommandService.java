package com.myrtletrip.prize.service;

import com.myrtletrip.prize.dto.PrizeScheduleResponse;
import com.myrtletrip.prize.dto.SavePrizeSchedulePayoutRequest;
import com.myrtletrip.prize.dto.SavePrizeScheduleRequest;
import com.myrtletrip.prize.dto.SaveTripPrizeSchedulesRequest;
import com.myrtletrip.prize.entity.PrizeSchedule;
import com.myrtletrip.prize.entity.PrizeSchedulePayout;
import com.myrtletrip.prize.model.PrizePayoutUnit;
import com.myrtletrip.prize.model.PrizeResultScope;
import com.myrtletrip.prize.repository.PrizeSchedulePayoutRepository;
import com.myrtletrip.prize.repository.PrizeScheduleRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TripPrizeCommandService {

    private final TripRepository tripRepository;
    private final PrizeScheduleRepository prizeScheduleRepository;
    private final PrizeSchedulePayoutRepository prizeSchedulePayoutRepository;
    private final TripEditingGuardService tripEditingGuardService;
    private final TripPrizeReadModelService readModelService;

    public TripPrizeCommandService(TripRepository tripRepository,
                                   PrizeScheduleRepository prizeScheduleRepository,
                                   PrizeSchedulePayoutRepository prizeSchedulePayoutRepository,
                                   TripEditingGuardService tripEditingGuardService,
                                   TripPrizeReadModelService readModelService) {
        this.tripRepository = tripRepository;
        this.prizeScheduleRepository = prizeScheduleRepository;
        this.prizeSchedulePayoutRepository = prizeSchedulePayoutRepository;
        this.tripEditingGuardService = tripEditingGuardService;
        this.readModelService = readModelService;
    }

    @Transactional
    public List<PrizeScheduleResponse> savePrizeSchedules(Long tripId, SaveTripPrizeSchedulesRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        tripEditingGuardService.assertStructureEditable(trip);

        readModelService.ensureDefaultSchedules(tripId);
        Set<String> eligibleKeys = readModelService.buildEligiblePrizeKeys(tripId);

        Map<String, PrizeSchedule> schedulesByKey = new HashMap<>();
        List<PrizeSchedule> existingSchedules = prizeScheduleRepository.findByTrip_IdOrderByIdAsc(tripId);
        for (PrizeSchedule schedule : existingSchedules) {
            schedulesByKey.put(schedule.getGameKey(), schedule);
        }

        if (request != null && request.getSchedules() != null) {
            for (SavePrizeScheduleRequest scheduleRequest : request.getSchedules()) {
                if (scheduleRequest == null || scheduleRequest.getGameKey() == null) {
                    continue;
                }
                if (!eligibleKeys.contains(scheduleRequest.getGameKey())) {
                    continue;
                }

                PrizeSchedule schedule = schedulesByKey.get(scheduleRequest.getGameKey());
                if (schedule == null) {
                    continue;
                }

                if (scheduleRequest.getGameName() != null && !scheduleRequest.getGameName().isBlank()) {
                    schedule.setGameName(scheduleRequest.getGameName().trim());
                }

                schedule.setResultScope(parseResultScope(scheduleRequest.getResultScope(), schedule.getResultScope()));
                schedule.setPayoutUnit(parsePayoutUnit(scheduleRequest.getPayoutUnit(), schedule.getPayoutUnit()));
                prizeScheduleRepository.save(schedule);

                replacePayouts(schedule, scheduleRequest.getPayouts());
            }
        }

        return readModelService.loadResponses(tripId);
    }

    private void replacePayouts(PrizeSchedule schedule, List<SavePrizeSchedulePayoutRequest> requests) {
        prizeSchedulePayoutRepository.deleteByPrizeSchedule_Id(schedule.getId());
        prizeSchedulePayoutRepository.flush();

        List<PrizeSchedulePayout> replacementPayouts = new ArrayList<>();
        if (requests != null) {
            List<SavePrizeSchedulePayoutRequest> payoutRequests = new ArrayList<>(requests);
            payoutRequests.sort(Comparator.comparing(SavePrizeSchedulePayoutRequest::getFinishingPlace,
                    Comparator.nullsLast(Integer::compareTo)));

            for (SavePrizeSchedulePayoutRequest payoutRequest : payoutRequests) {
                if (payoutRequest == null || payoutRequest.getFinishingPlace() == null
                        || payoutRequest.getFinishingPlace() < 1) {
                    continue;
                }

                BigDecimal amount = payoutRequest.getAmountPerPlayer();
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }

                PrizeSchedulePayout payout = new PrizeSchedulePayout();
                payout.setPrizeSchedule(schedule);
                payout.setFinishingPlace(payoutRequest.getFinishingPlace());
                payout.setAmountPerPlayer(amount);
                replacementPayouts.add(payout);
            }
        }

        if (!replacementPayouts.isEmpty()) {
            prizeSchedulePayoutRepository.saveAll(replacementPayouts);
            prizeSchedulePayoutRepository.flush();
        }
    }

    private PrizeResultScope parseResultScope(String value, PrizeResultScope fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return PrizeResultScope.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private PrizePayoutUnit parsePayoutUnit(String value, PrizePayoutUnit fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return PrizePayoutUnit.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
