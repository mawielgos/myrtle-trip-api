package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.BulkRoundScoreRequest;
import com.myrtletrip.round.dto.PlayerBulkScoreDto;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.service.ScoringService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BulkScoreEntryService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final ScoringService scoringService;

    public BulkScoreEntryService(RoundRepository roundRepository,
                                 ScorecardRepository scorecardRepository,
                                 HoleScoreRepository holeScoreRepository,
                                 ScoringService scoringService) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.scoringService = scoringService;
    }

    @Transactional
    public void saveBulkScores(Long roundId, BulkRoundScoreRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Round is already finalized");
        }

        if (request == null || request.getScorecards() == null || request.getScorecards().isEmpty()) {
            throw new IllegalArgumentException("No scorecards supplied");
        }

        List<Scorecard> roundScorecards = scorecardRepository.findByRound_Id(roundId);

        Map<Long, Scorecard> scorecardByPlayerId = new HashMap<>();
        for (Scorecard scorecard : roundScorecards) {
            scorecardByPlayerId.put(scorecard.getPlayer().getId(), scorecard);
        }

        for (PlayerBulkScoreDto dto : request.getScorecards()) {
            validate(dto);

            Scorecard scorecard = scorecardByPlayerId.get(dto.getPlayerId());
            if (scorecard == null) {
                throw new IllegalArgumentException(
                        "No scorecard found for player " + dto.getPlayerId() + " in round " + roundId
                );
            }

            saveOrUpdateHoleScores(scorecard, dto.getHoles());
            scoringService.recalculate(scorecard.getId());
        }
    }

    private void validate(PlayerBulkScoreDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Scorecard entry is required");
        }

        if (dto.getPlayerId() == null) {
            throw new IllegalArgumentException("playerId is required");
        }

        if (dto.getHoles() == null || dto.getHoles().size() != 18) {
            throw new IllegalArgumentException(
                    "Exactly 18 hole scores are required for player " + dto.getPlayerId()
            );
        }

        for (int i = 0; i < dto.getHoles().size(); i++) {
            Integer strokes = dto.getHoles().get(i);
            if (strokes != null && strokes < 1) {
                throw new IllegalArgumentException(
                        "Invalid score on hole " + (i + 1) + " for player " + dto.getPlayerId()
                );
            }
        }
    }

    private void saveOrUpdateHoleScores(Scorecard scorecard, List<Integer> holes) {
        List<HoleScore> existing = holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(scorecard.getId());

        Map<Integer, HoleScore> existingByHole = new HashMap<>();
        for (HoleScore holeScore : existing) {
            existingByHole.put(holeScore.getHoleNumber(), holeScore);
        }

        for (int i = 0; i < 18; i++) {
            int holeNumber = i + 1;
            Integer strokes = holes.get(i);

            HoleScore holeScore = existingByHole.get(holeNumber);
            if (holeScore == null) {
                holeScore = new HoleScore();
                holeScore.setScorecard(scorecard);
                holeScore.setHoleNumber(holeNumber);
            }

            holeScore.setStrokes(strokes);
            holeScoreRepository.save(holeScore);
        }
    }
}