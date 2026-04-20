package com.myrtletrip.scoreentry.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.scoreentry.dto.HoleScoreResponse;
import com.myrtletrip.scoreentry.dto.RoundScorecardResponse;
import com.myrtletrip.scoreentry.dto.ScorecardResponse;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.service.RoundScoreHistorySyncService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoringService {

    private final HoleScoreRepository holeRepo;
    private final ScorecardRepository scorecardRepo;
    private final RoundTeeHoleRepository roundTeeHoleRepository;
    private final RoundScoreHistorySyncService roundScoreHistorySyncService;

    public ScoringService(HoleScoreRepository holeRepo,
                          ScorecardRepository scorecardRepo,
                          RoundTeeHoleRepository roundTeeHoleRepository,
                          RoundScoreHistorySyncService roundScoreHistorySyncService) {
        this.holeRepo = holeRepo;
        this.scorecardRepo = scorecardRepo;
        this.roundTeeHoleRepository = roundTeeHoleRepository;
        this.roundScoreHistorySyncService = roundScoreHistorySyncService;
    }

    @Transactional
    public void updateHoleScore(Long scorecardId, int holeNumber, int strokes) {
        if (holeNumber < 1 || holeNumber > 18) {
            throw new IllegalArgumentException("holeNumber must be between 1 and 18");
        }

        if (strokes < 1 || strokes > 20) {
            throw new IllegalArgumentException("strokes must be between 1 and 20");
        }

        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));


        HoleScore hole = holeRepo.findByScorecard_IdAndHoleNumber(scorecardId, holeNumber)
                .orElseGet(() -> {
                    HoleScore newHole = new HoleScore();
                    newHole.setHoleNumber(holeNumber);
                    newHole.setScorecard(scorecard);
                    return newHole;
                });

        hole.setStrokes(strokes);
        holeRepo.save(hole);

        recalculateScorecard(scorecardId);
    }

    @Transactional
    public void recalculate(Long scorecardId) {
        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));


        recalculateScorecard(scorecardId);
    }

    @Transactional(readOnly = true)
    public ScorecardResponse getScorecard(Long scorecardId) {
        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));

        List<HoleScore> holes = holeRepo.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);

        ScorecardResponse response = new ScorecardResponse();
        response.setScorecardId(scorecard.getId());
        response.setRoundId(scorecard.getRound().getId());
        response.setPlayerId(scorecard.getPlayer().getId());
        response.setPlayingHandicap(scorecard.getPlayingHandicap());
        response.setGrossScore(scorecard.getGrossScore());
        response.setNetScore(scorecard.getNetScore());
        response.setAdjustedGrossScore(scorecard.getAdjustedGrossScore());

        List<HoleScoreResponse> holeResponses = holes.stream().map(h -> {
            HoleScoreResponse dto = new HoleScoreResponse();
            dto.setHoleNumber(h.getHoleNumber());
            dto.setStrokes(h.getStrokes());
            dto.setNetStrokes(h.getNetStrokes());
            dto.setAdjustedStrokes(h.getAdjustedStrokes());
            return dto;
        }).toList();

        response.setHoles(holeResponses);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RoundScorecardResponse> getRoundScorecards(Long roundId) {
        List<Scorecard> scorecards = scorecardRepo.findByRound_Id(roundId);

        return scorecards.stream().map(sc -> {
            RoundScorecardResponse dto = new RoundScorecardResponse();

            dto.setScorecardId(sc.getId());
            dto.setPlayerId(sc.getPlayer().getId());
            dto.setPlayerName(sc.getPlayer().getDisplayName());
            dto.setPlayingHandicap(sc.getPlayingHandicap());
            dto.setGrossScore(sc.getGrossScore());
            dto.setNetScore(sc.getNetScore());
            dto.setAdjustedGrossScore(sc.getAdjustedGrossScore());

            return dto;
        })
        .sorted((a, b) -> Integer.compare(
                a.getNetScore() == null ? 999 : a.getNetScore(),
                b.getNetScore() == null ? 999 : b.getNetScore()
        ))
        .toList();
    }

    private void recalculateScorecard(Long scorecardId) {
        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));

        Round round = scorecard.getRound();
        RoundTee roundTee = scorecard.getRoundTee();

        if (roundTee == null) {
            throw new IllegalStateException("scorecard.roundTee is required");
        }

        List<RoundTeeHole> roundTeeHoles = roundTeeHoleRepository.findByRoundTee_IdOrderByHoleNumberAsc(roundTee.getId());
        if (roundTeeHoles.size() != 18) {
            throw new IllegalStateException("Expected 18 holes for round tee " + roundTee.getId() + " but found " + roundTeeHoles.size());
        }

        List<HoleScore> holeScores = holeRepo.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);

        Map<Integer, HoleScore> holeScoreByHoleNumber = new HashMap<>();
        for (HoleScore holeScore : holeScores) {
            holeScoreByHoleNumber.put(holeScore.getHoleNumber(), holeScore);
        }

        int grossTotal = 0;
        int adjustedGrossTotal = 0;
        int netTotal = 0;
        int thruHole = 0;
        boolean hasAnyPlayedHoles = false;

        int playingHandicap = scorecard.getPlayingHandicap() == null ? 0 : scorecard.getPlayingHandicap();
        int courseHandicap = scorecard.getCourseHandicap() == null ? 0 : scorecard.getCourseHandicap();
        boolean scramble = isScramble(round);

        for (RoundTeeHole roundTeeHole : roundTeeHoles) {
            int holeNumber = roundTeeHole.getHoleNumber();
            int par = roundTeeHole.getPar();
            int strokeIndex = roundTeeHole.getHandicap();

            HoleScore holeScore = holeScoreByHoleNumber.get(holeNumber);
            if (holeScore == null) {
                holeScore = new HoleScore();
                holeScore.setScorecard(scorecard);
                holeScore.setHoleNumber(holeNumber);
            }

            Integer strokes = holeScore.getStrokes();

            if (strokes == null) {
                holeScore.setNetStrokes(null);
                holeScore.setAdjustedStrokes(null);
                holeRepo.save(holeScore);
                continue;
            }

            hasAnyPlayedHoles = true;
            thruHole = Math.max(thruHole, holeNumber);
            grossTotal += strokes;

            if (scramble) {
                holeScore.setNetStrokes(null);
                holeScore.setAdjustedStrokes(null);
            } else {
                int playingHandicapStrokes = getStrokesForHole(playingHandicap, strokeIndex);
                int courseHandicapStrokes = getStrokesForHole(courseHandicap, strokeIndex);

                int netStrokes = strokes - playingHandicapStrokes;
                int adjustedStrokes = Math.min(strokes, par + 2 + courseHandicapStrokes);

                holeScore.setNetStrokes(netStrokes);
                holeScore.setAdjustedStrokes(adjustedStrokes);

                netTotal += netStrokes;
                adjustedGrossTotal += adjustedStrokes;
            }

            holeRepo.save(holeScore);
        }

        if (!hasAnyPlayedHoles) {
            scorecard.setGrossScore(null);
            scorecard.setAdjustedGrossScore(null);
            scorecard.setNetScore(null);
            scorecard.setThruHole(null);
        } else {
            scorecard.setGrossScore(grossTotal);
            scorecard.setThruHole(thruHole);

            if (scramble) {
                scorecard.setAdjustedGrossScore(null);
                scorecard.setNetScore(null);
            } else {
                scorecard.setAdjustedGrossScore(adjustedGrossTotal);
                scorecard.setNetScore(netTotal);
            }
        }

        scorecardRepo.save(scorecard);

        if (Boolean.TRUE.equals(round.getFinalized())) {
            roundScoreHistorySyncService.syncFinalizedScorecard(scorecard);
        }
    }

    private boolean isScramble(Round round) {
        return round.getFormat() == RoundFormat.TEAM_SCRAMBLE;
    }

    private int getStrokesForHole(int handicap, int holeHandicap) {
        if (handicap <= 0) {
            return 0;
        }

        int base = handicap / 18;
        int remainder = handicap % 18;
        int extra = holeHandicap <= remainder ? 1 : 0;

        return base + extra;
    }
}
