package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.service.ScoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        RoundTee targetRoundTee = useAlternateTee ? round.getAlternateRoundTee() : round.getStandardRoundTee();
        if (targetRoundTee == null) {
            throw new IllegalStateException(useAlternateTee
                    ? "Round does not have an alternate tee configured"
                    : "Round does not have a standard tee configured");
        }

        scorecard.setRoundTee(targetRoundTee);
        refreshHandicaps(scorecard);
        scorecardRepository.save(scorecard);
        scoringService.recalculate(scorecardId);
    }
    
    @Transactional
    public void refreshHandicaps(Long scorecardId) {
        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));

        refreshHandicaps(scorecard);
        scorecardRepository.save(scorecard);
        scoringService.recalculate(scorecardId);
    }

    @Transactional
    public void refreshRoundHandicaps(Long roundId) {
        refreshRoundHandicapsInternal(roundId, false);
    }

    @Transactional
    public void refreshRoundHandicapsForCorrection(Long roundId) {
        refreshRoundHandicapsInternal(roundId, true);
    }

    private void refreshRoundHandicapsInternal(Long roundId, boolean allowFinalized) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        for (Scorecard scorecard : scorecards) {
            if (!allowFinalized && Boolean.TRUE.equals(scorecard.getRound().getFinalized())) {
                throw new IllegalStateException("Cannot refresh handicaps after round is finalized");
            }

            if (scorecard.getRoundTee() == null) {
                scorecard.setRoundTee(scorecard.getRound().getStandardRoundTee());
            }

            refreshHandicaps(scorecard);
            scorecardRepository.save(scorecard);
            scoringService.recalculate(scorecard.getId());
        }
    }

    private void refreshHandicaps(Scorecard scorecard) {
        Round round = scorecard.getRound();
        if (round == null) {
            throw new IllegalArgumentException("scorecard.round is required");
        }
        if (scorecard.getRoundTee() == null) {
            throw new IllegalArgumentException("scorecard.roundTee is required");
        }

        String handicapGroupCode = round.getTrip().getTripCode();
        roundHandicapService.populateCurrentHandicaps(scorecard, handicapGroupCode);
    }
}
