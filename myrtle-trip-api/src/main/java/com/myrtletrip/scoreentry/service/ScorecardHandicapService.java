package com.myrtletrip.scoreentry.service;

import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScorecardHandicapService {

    private final ScorecardRepository scorecardRepository;
    private final RoundHandicapService roundHandicapService;
    private final ScoringService scoringService;

    public ScorecardHandicapService(ScorecardRepository scorecardRepository,
                                    RoundHandicapService roundHandicapService,
                                    ScoringService scoringService) {
        this.scorecardRepository = scorecardRepository;
        this.roundHandicapService = roundHandicapService;
        this.scoringService = scoringService;
    }

    @Transactional
    public void setAlternateTee(Long scorecardId, boolean useAlternateTee) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        Round round = scorecard.getRound();
        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Cannot change tee after round is finalized");
        }

        scorecard.setUseAlternateTee(useAlternateTee);

        String handicapGroupCode = round.getTrip().getTripCode();
        roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);

        scorecardRepository.save(scorecard);
        scoringService.recalculate(scorecardId);
    }
}