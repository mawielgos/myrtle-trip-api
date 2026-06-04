package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.event.model.RoundEventType;

public interface RoundGameScorer {

    RoundEventType supports();

    RoundGameResult scoreRound(RoundScoringData data);
}
