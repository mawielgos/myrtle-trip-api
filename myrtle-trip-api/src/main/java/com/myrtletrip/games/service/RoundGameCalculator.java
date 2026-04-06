package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.GameScoreResponse;
import com.myrtletrip.round.model.RoundFormat;

public interface RoundGameCalculator {

    RoundFormat getSupportedFormat();

    GameScoreResponse calculate(Long roundId);
}
