package com.myrtletrip.games.service;

import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.round.entity.Round;
import org.springframework.stereotype.Service;

@Service
public class RoundScoringDataService {

    public RoundScoringData build(Round round) {
        RoundScoringData data = new RoundScoringData();
        data.setRoundId(round.getId());
        data.setFormat(round.getFormat());

        // populate teams here
        // data.setTeams(...)

        return data;
    }
}
