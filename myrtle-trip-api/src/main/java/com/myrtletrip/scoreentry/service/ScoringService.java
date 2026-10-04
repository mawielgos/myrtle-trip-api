package com.myrtletrip.scoreentry.service;

import com.myrtletrip.scoreentry.dto.RoundScorecardResponse;
import com.myrtletrip.scoreentry.dto.ScorecardResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoringService {

    private final ScoringCommandService scoringCommandService;
    private final ScoringReadModelService scoringReadModelService;

    public ScoringService(ScoringCommandService scoringCommandService,
                          ScoringReadModelService scoringReadModelService) {
        this.scoringCommandService = scoringCommandService;
        this.scoringReadModelService = scoringReadModelService;
    }

    public void updateHoleScore(Long scorecardId, int holeNumber, int strokes) {
        scoringCommandService.updateHoleScore(scorecardId, holeNumber, strokes);
    }

    public void recalculate(Long scorecardId) {
        scoringCommandService.recalculate(scorecardId);
    }

    public ScorecardResponse getScorecard(Long scorecardId) {
        return scoringReadModelService.getScorecard(scorecardId);
    }

    public List<RoundScorecardResponse> getRoundScorecards(Long roundId) {
        return scoringReadModelService.getRoundScorecards(roundId);
    }
}
