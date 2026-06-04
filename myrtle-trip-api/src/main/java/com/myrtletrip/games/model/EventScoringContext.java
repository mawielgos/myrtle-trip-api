package com.myrtletrip.games.model;

import com.myrtletrip.event.entity.RoundEvent;
import com.myrtletrip.event.model.RoundEventType;
import com.myrtletrip.round.entity.Round;

/**
 * Event-scoped scoring context used by the scoring orchestration layer.
 *
 * This is intentionally small for the first cleanup pass: it keeps the
 * existing RoundScoringData model intact while making the active RoundEvent
 * explicit. Future result persistence/recalculation work can grow this object
 * instead of adding more round-format branching to service methods.
 */
public class EventScoringContext {

    private final Round round;
    private final RoundEvent roundEvent;
    private final RoundEventType eventType;
    private final RoundScoringData scoringData;

    public EventScoringContext(Round round,
                               RoundEvent roundEvent,
                               RoundEventType eventType,
                               RoundScoringData scoringData) {
        this.round = round;
        this.roundEvent = roundEvent;
        this.eventType = eventType;
        this.scoringData = scoringData;
    }

    public Round getRound() {
        return round;
    }

    public RoundEvent getRoundEvent() {
        return roundEvent;
    }

    public Long getRoundEventId() {
        return roundEvent == null ? null : roundEvent.getId();
    }

    public RoundEventType getEventType() {
        return eventType;
    }

    public RoundScoringData getScoringData() {
        return scoringData;
    }

    public boolean isIndividualEvent() {
        return eventType == null || eventType.isIndividualEvent();
    }

    public boolean isTeamEvent() {
        return eventType != null && eventType.isTeamEvent();
    }
}
