package com.myrtletrip.scorehistory.service;

import org.springframework.stereotype.Service;

@Service
public class AdjustedScoreService {

    public int calculateAdjustedHoleScore(int grossScore, int par, int strokesReceived) {
        int maxAllowed = par + 2 + strokesReceived;
        return Math.min(grossScore, maxAllowed);
    }

    public int strokesReceivedOnHole(int courseHandicap, int strokeIndex) {
        if (courseHandicap <= 0) {
            return 0;
        }

        int base = courseHandicap / 18;
        int remainder = courseHandicap % 18;

        return base + (strokeIndex <= remainder ? 1 : 0);
    }
}
