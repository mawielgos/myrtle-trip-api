package com.myrtletrip.prize.service;

import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.prize.dto.PrizeScheduleResponse;
import com.myrtletrip.prize.dto.SaveTripPrizeSchedulesRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class TripPrizeService {

    public static final String LEGACY_TOURNAMENT_GAME_KEY = "FOUR_DAY_INDIVIDUAL";
    public static final String TOURNAMENT_LOW_NET_GAME_KEY = "TOURNAMENT_LOW_NET";
    public static final String TOURNAMENT_LOW_GROSS_GAME_KEY = "TOURNAMENT_LOW_GROSS";
    public static final String ROUND_EVENT_KEY_PREFIX = "ROUND_NUMBER_";
    public static final String ROUND_EVENT_KEY_SEPARATOR = "_EVENT_";

    private final TripPrizeReadModelService readModelService;
    private final TripPrizeCommandService commandService;

    public TripPrizeService(TripPrizeReadModelService readModelService,
                            TripPrizeCommandService commandService) {
        this.readModelService = readModelService;
        this.commandService = commandService;
    }

    @Transactional
    public List<PrizeScheduleResponse> getPrizeSchedules(Long tripId) {
        return readModelService.getPrizeSchedules(tripId);
    }

    @Transactional(readOnly = true)
    public Set<String> getActivePrizeScheduleKeys(Long tripId) {
        return readModelService.getActivePrizeScheduleKeys(tripId);
    }

    @Transactional
    public List<PrizeScheduleResponse> savePrizeSchedules(Long tripId, SaveTripPrizeSchedulesRequest request) {
        return commandService.savePrizeSchedules(tripId, request);
    }

    public static String buildRoundEventGameKey(Integer roundNumber, RoundEventType eventType) {
        return ROUND_EVENT_KEY_PREFIX + roundNumber + ROUND_EVENT_KEY_SEPARATOR + eventType;
    }

    public static RoundEventType parseRoundEventTypeFromGameKey(String gameKey) {
        if (gameKey == null) {
            return null;
        }
        int index = gameKey.indexOf(ROUND_EVENT_KEY_SEPARATOR);
        if (index < 0) {
            return null;
        }
        String eventTypeText = gameKey.substring(index + ROUND_EVENT_KEY_SEPARATOR.length());
        try {
            return RoundEventType.valueOf(eventTypeText);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
