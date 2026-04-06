package com.myrtletrip.scoreentry.service;

import com.myrtletrip.course.service.CourseService;
import com.myrtletrip.scoreentry.dto.HoleScoreResponse;
import com.myrtletrip.scoreentry.dto.RoundScorecardResponse;
import com.myrtletrip.scoreentry.dto.ScorecardResponse;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScoringService {

    private final HoleScoreRepository holeRepo;
    private final ScorecardRepository scorecardRepo;
    private final CourseService courseService;

    public ScoringService(HoleScoreRepository holeRepo,
                          ScorecardRepository scorecardRepo,
                          CourseService courseService) {
        this.holeRepo = holeRepo;
        this.scorecardRepo = scorecardRepo;
        this.courseService = courseService;
    }

    @Transactional
    public void updateHoleScore(Long scorecardId, int holeNumber, int strokes) {

        Scorecard scorecard = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));

        if (Boolean.TRUE.equals(scorecard.getRound().getFinalized())) {
            throw new IllegalStateException("Round is already finalized");
        }

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

        if (Boolean.TRUE.equals(scorecard.getRound().getFinalized())) {
            throw new IllegalStateException("Round is already finalized");
        }

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

        Scorecard sc = scorecardRepo.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found"));

        List<HoleScore> holes = holeRepo.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);

        int gross = 0;
        int totalNet = 0;
        int adjustedGross = 0;

        Long courseTeeId = sc.getRound().getCourseTee().getId();

        int playingHandicap = sc.getPlayingHandicap() == null ? 0 : sc.getPlayingHandicap();
        int courseHandicap = sc.getCourseHandicap() == null ? 0 : sc.getCourseHandicap();

        for (HoleScore h : holes) {
            int strokes = h.getStrokes() == null ? 0 : h.getStrokes();

            gross += strokes;

            int holeHcp = courseService.getHoleHandicap(courseTeeId, h.getHoleNumber());
            int par = courseService.getHolePar(courseTeeId, h.getHoleNumber());

            int playingHandicapStrokes = getStrokesForHole(playingHandicap, holeHcp);
            int courseHandicapStrokes = getStrokesForHole(courseHandicap, holeHcp);

            int netStrokes = strokes == 0 ? 0 : strokes - playingHandicapStrokes;
            int adjustedStrokes = strokes == 0
                    ? 0
                    : Math.min(strokes, par + 2 + courseHandicapStrokes);

            totalNet += netStrokes;
            adjustedGross += adjustedStrokes;

            h.setNetStrokes(netStrokes);
            h.setAdjustedStrokes(adjustedStrokes);

            holeRepo.save(h);
        }

        sc.setGrossScore(gross == 0 ? null : gross);
        sc.setNetScore(gross == 0 ? null : totalNet);
        sc.setAdjustedGrossScore(gross == 0 ? null : adjustedGross);

        scorecardRepo.save(sc);
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