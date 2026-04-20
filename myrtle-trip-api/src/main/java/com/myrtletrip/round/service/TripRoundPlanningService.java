package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.TripRoundPlanRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TripRoundPlanningService {

    private final RoundRepository roundRepository;
    private final TripRepository tripRepository;

    public TripRoundPlanningService(RoundRepository roundRepository,
                                    TripRepository tripRepository) {
        this.roundRepository = roundRepository;
        this.tripRepository = tripRepository;
    }

    public List<Round> generateRounds(Long tripId, TripRoundPlanRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found: " + tripId));

        List<TripRoundPlanRequest.RoundPlanItem> planItems;
        if (request != null && request.getRounds() != null && !request.getRounds().isEmpty()) {
            planItems = request.getRounds();
        } else {
            planItems = buildDefaultPlan();
        }

        validatePlanItems(planItems);

        roundRepository.deleteByTrip_Id(tripId);

        List<Round> roundsToCreate = new ArrayList<Round>();
        for (int i = 0; i < planItems.size(); i++) {
            TripRoundPlanRequest.RoundPlanItem item = planItems.get(i);

            Round round = new Round();
            round.setTrip(trip);
            round.setRoundNumber(item.getRoundNumber());
            round.setFormat(item.getFormat());
            round.setFinalized(Boolean.FALSE);
            round.setHandicapPercent(100);

            roundsToCreate.add(round);
        }

        return roundRepository.saveAll(roundsToCreate);
    }

    private void validatePlanItems(List<TripRoundPlanRequest.RoundPlanItem> planItems) {
        if (planItems == null || planItems.size() != 5) {
            throw new IllegalArgumentException("Trip round plan must contain exactly 5 rounds.");
        }

        Set<Integer> roundNumbers = new HashSet<Integer>();

        for (int i = 0; i < planItems.size(); i++) {
            TripRoundPlanRequest.RoundPlanItem item = planItems.get(i);

            if (item == null) {
                throw new IllegalArgumentException("Round plan item cannot be null.");
            }
            if (item.getRoundNumber() == null) {
                throw new IllegalArgumentException("Round number is required.");
            }
            if (item.getRoundNumber() < 1 || item.getRoundNumber() > 5) {
                throw new IllegalArgumentException("Round number must be between 1 and 5.");
            }
            if (!roundNumbers.add(item.getRoundNumber())) {
                throw new IllegalArgumentException("Duplicate round number: " + item.getRoundNumber());
            }
            if (item.getFormat() == null) {
                throw new IllegalArgumentException("Round format is required for round " + item.getRoundNumber());
            }
        }
    }

    private List<TripRoundPlanRequest.RoundPlanItem> buildDefaultPlan() {
        List<TripRoundPlanRequest.RoundPlanItem> list = new ArrayList<TripRoundPlanRequest.RoundPlanItem>();

        list.add(createItem(1, RoundFormat.MIDDLE_MAN));
        list.add(createItem(2, RoundFormat.ONE_TWO_THREE));
        list.add(createItem(3, RoundFormat.TWO_MAN_LOW_NET));
        list.add(createItem(4, RoundFormat.THREE_LOW_NET));
        list.add(createItem(5, RoundFormat.TEAM_SCRAMBLE));

        return list;
    }

    private TripRoundPlanRequest.RoundPlanItem createItem(int roundNumber, RoundFormat format) {
        TripRoundPlanRequest.RoundPlanItem item = new TripRoundPlanRequest.RoundPlanItem();
        item.setRoundNumber(roundNumber);
        item.setFormat(format);
        return item;
    }
}
