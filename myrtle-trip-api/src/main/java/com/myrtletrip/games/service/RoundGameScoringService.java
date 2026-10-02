package com.myrtletrip.games.service;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.event.service.RoundEventService;
import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.model.EventScoringContext;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundGameScoringService {

    private final RoundRepository roundRepository;
    private final RoundScoringDataService roundScoringDataService;
    private final RoundGameScorerRegistry scorerRegistry;
    private final RoundEventService roundEventService;
    private final TripEditingGuardService tripEditingGuardService;
    private final RoundIndividualStrokePlayResultService individualStrokePlayResultService;
    private final RoundTeamGameResultService teamGameResultService;
    private final RoundUsedHoleScoreService usedHoleScoreService;

    public RoundGameScoringService(RoundRepository roundRepository,
                                   RoundScoringDataService roundScoringDataService,
                                   RoundGameScorerRegistry scorerRegistry,
                                   RoundEventService roundEventService,
                                   TripEditingGuardService tripEditingGuardService,
                                   RoundIndividualStrokePlayResultService individualStrokePlayResultService,
                                   RoundTeamGameResultService teamGameResultService,
                                   RoundUsedHoleScoreService usedHoleScoreService) {
        this.roundRepository = roundRepository;
        this.roundScoringDataService = roundScoringDataService;
        this.scorerRegistry = scorerRegistry;
        this.roundEventService = roundEventService;
        this.tripEditingGuardService = tripEditingGuardService;
        this.individualStrokePlayResultService = individualStrokePlayResultService;
        this.teamGameResultService = teamGameResultService;
        this.usedHoleScoreService = usedHoleScoreService;
    }

    @Transactional(readOnly = true)
    public RoundGameResult getRoundResult(Long roundId) {
        Round round = loadRound(roundId);
        RoundEvent event = resolvePrimaryTeamOrFallbackEvent(round);
        return getRoundResult(buildEventScoringContext(round, event));
    }

    @Transactional(readOnly = true)
    public RoundGameResult getRoundResult(Long roundId, RoundEventType eventType) {
        Round round = loadRound(roundId);
        RoundEvent event = resolveEvent(round, eventType);
        return getRoundResult(buildEventScoringContext(round, event));
    }

    private RoundGameResult getRoundResult(EventScoringContext context) {
        if (context.isIndividualEvent()) {
            return individualStrokePlayResultService.createResult(context.getScoringData());
        }

        if (!teamGameResultService.isCompleteForScoring(context)) {
            return teamGameResultService.createUnscoredResult(context.getScoringData());
        }

        RoundGameScorer scorer = scorerRegistry.getScorer(context.getEventType());
        return scorer.scoreRound(context.getScoringData());
    }

    @Transactional
    public RoundGameResult recalculateRound(Long roundId) {
        Round round = loadRound(roundId);
        RoundEvent event = resolvePrimaryTeamOrFallbackEvent(round);
        return recalculateRound(buildEventScoringContext(round, event));
    }

    @Transactional
    public RoundGameResult recalculateRound(Long roundId, RoundEventType eventType) {
        Round round = loadRound(roundId);
        RoundEvent event = resolveEvent(round, eventType);
        return recalculateRound(buildEventScoringContext(round, event));
    }

    /**
     * Recalculates every active event on a round and returns the event-scoped results.
     *
     * Existing callers still use recalculateRound(...) for compatibility, but this
     * method is the transition point for correction/finalization flows that need to
     * be fully event-scoped instead of relying on the round's legacy format field.
     */
    @Transactional
    public List<RoundGameResult> recalculateRoundEvents(Long roundId) {
        Round round = loadRound(roundId);
        tripEditingGuardService.assertCorrectionAllowedForRound(round);

        List<RoundGameResult> results = new ArrayList<RoundGameResult>();
        for (RoundEvent event : roundEventService.findActiveEventsForRound(round.getId())) {
            results.add(recalculateRound(buildEventScoringContext(round, event)));
        }
        return results;
    }

    private RoundGameResult recalculateRound(EventScoringContext context) {
        tripEditingGuardService.assertCorrectionAllowedForRound(context.getRound());

        usedHoleScoreService.clearUsedHoleScoreFlags(context);

        if (context.isIndividualEvent()) {
            return individualStrokePlayResultService.createResult(context.getScoringData());
        }

        if (!teamGameResultService.isCompleteForScoring(context)) {
            return teamGameResultService.createUnscoredResult(context.getScoringData());
        }

        RoundGameScorer scorer = scorerRegistry.getScorer(context.getEventType());
        RoundGameResult result = scorer.scoreRound(context.getScoringData());

        usedHoleScoreService.markUsedHoleScores(context);

        return result;
    }

    private EventScoringContext buildEventScoringContext(Round round, RoundEvent event) {
        RoundEventType eventType = event == null ? RoundEventType.fromLegacyRoundFormat(round.getFormat()) : event.getEventType();
        RoundScoringData data = roundScoringDataService.build(round);
        applyEventFormat(data, eventType);
        return new EventScoringContext(round, event, eventType, data);
    }

    private void applyEventFormat(RoundScoringData data, RoundEventType eventType) {
        if (data == null || eventType == null) {
            return;
        }
        data.setFormat(eventType.legacyRoundFormat());
    }

    private Round loadRound(Long roundId) {
        return roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
    }

    private RoundEvent resolvePrimaryTeamOrFallbackEvent(Round round) {
        List<RoundEvent> events = roundEventService.findActiveEventsForRound(round.getId());
        for (RoundEvent event : events) {
            if (event != null && event.getEventType() != null && event.getEventType().isTeamEvent()) {
                return event;
            }
        }
        if (!events.isEmpty() && events.get(0) != null && events.get(0).getEventType() != null) {
            return events.get(0);
        }
        return null;
    }

    private RoundEvent resolveEvent(Round round, RoundEventType requestedEventType) {
        if (requestedEventType == null) {
            return resolvePrimaryTeamOrFallbackEvent(round);
        }

        List<RoundEvent> events = roundEventService.findActiveEventsForRound(round.getId());
        for (RoundEvent event : events) {
            if (event != null && requestedEventType == event.getEventType()) {
                return event;
            }
        }

        RoundEventType legacyEventType = RoundEventType.fromLegacyRoundFormat(round.getFormat());
        if (requestedEventType == legacyEventType) {
            return null;
        }

        throw new IllegalArgumentException(
                "Round " + round.getId() + " does not have active event type " + requestedEventType
        );
    }

}
