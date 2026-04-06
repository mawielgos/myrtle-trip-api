package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.round.model.RoundFormat;

public interface RoundGameScorer {

    RoundFormat supports();

    RoundGameResult scoreRound(RoundScoringData data);
}
