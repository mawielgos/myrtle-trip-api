package com.myrtletrip.games.service;

import com.myrtletrip.event.model.RoundEventType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RoundGameScorerRegistry {

    private final Map<RoundEventType, RoundGameScorer> scorersByEventType = new EnumMap<RoundEventType, RoundGameScorer>(RoundEventType.class);

    public RoundGameScorerRegistry(List<RoundGameScorer> scorers) {
        for (RoundGameScorer scorer : scorers) {
            if (scorer == null || scorer.supports() == null) {
                continue;
            }
            RoundGameScorer existing = scorersByEventType.put(scorer.supports(), scorer);
            if (existing != null) {
                throw new IllegalStateException("Multiple scorers registered for event type " + scorer.supports());
            }
        }
    }

    public RoundGameScorer getScorer(RoundEventType eventType) {
        RoundGameScorer scorer = scorersByEventType.get(eventType);
        if (scorer == null) {
            throw new IllegalStateException("No scorer registered for event type " + eventType);
        }
        return scorer;
    }
}
