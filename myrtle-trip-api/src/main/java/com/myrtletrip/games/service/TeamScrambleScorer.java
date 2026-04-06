package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.round.model.RoundFormat;
import org.springframework.stereotype.Service;

@Service
public class TeamScrambleScorer implements RoundGameScorer {

    @Override
    public RoundFormat supports() {
        return RoundFormat.TEAM_SCRAMBLE;
    }

    @Override
    public RoundGameResult scoreRound(RoundScoringData data) {
        throw new UnsupportedOperationException(
                "TEAM_SCRAMBLE scorer requires a team-hole-score model"
        );
    }
}