package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.BulkRoundScoreRequest;
import com.myrtletrip.round.dto.PlayerBulkScoreDto;
import com.myrtletrip.round.dto.RoundCorrectionRequest;
import com.myrtletrip.round.dto.RoundCorrectionResponse;
import com.myrtletrip.round.dto.RoundTeeCorrectionRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundCorrectionType;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundCorrectionService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final BulkScoreEntryService bulkScoreEntryService;
    private final ScorecardHandicapService scorecardHandicapService;
    private final RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;
    private final RoundCorrectionLogService roundCorrectionLogService;

    public RoundCorrectionService(RoundRepository roundRepository,
                                  ScorecardRepository scorecardRepository,
                                  HoleScoreRepository holeScoreRepository,
                                  BulkScoreEntryService bulkScoreEntryService,
                                  ScorecardHandicapService scorecardHandicapService,
                                  RoundRecalculationOrchestrationService roundRecalculationOrchestrationService,
                                  RoundCorrectionLogService roundCorrectionLogService) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.bulkScoreEntryService = bulkScoreEntryService;
        this.scorecardHandicapService = scorecardHandicapService;
        this.roundRecalculationOrchestrationService = roundRecalculationOrchestrationService;
        this.roundCorrectionLogService = roundCorrectionLogService;
    }

    @Transactional
    public RoundCorrectionResponse applyCorrection(Long roundId, RoundCorrectionRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        validateRequest(request);

        Boolean wasFinalized = round.getFinalized();
        boolean changedWithoutBulkScoreSave = false;

        List<ScorecardSnapshot> teeSnapshots = snapshotTeeCorrections(roundId, request.getTeeCorrections());
        List<ScorecardSnapshot> handicapSnapshots = Boolean.TRUE.equals(request.getRefreshHandicaps())
                ? snapshotAllRoundScorecards(roundId)
                : new ArrayList<ScorecardSnapshot>();
        List<ScorecardSnapshot> scoreSnapshots = snapshotPlayerCorrections(roundId, request.getPlayerCorrections());

        if (request.getTeeCorrections() != null && !request.getTeeCorrections().isEmpty()) {
            scorecardHandicapService.applyTeeCorrections(roundId, request.getTeeCorrections());
            changedWithoutBulkScoreSave = true;
            logTeeChanges(round, teeSnapshots);
        }

        if (Boolean.TRUE.equals(request.getRefreshHandicaps())) {
            scorecardHandicapService.refreshRoundHandicapsForCorrection(roundId);
            changedWithoutBulkScoreSave = true;
            logHandicapRefreshes(round, handicapSnapshots);
        }

        if (request.getPlayerCorrections() != null && !request.getPlayerCorrections().isEmpty()) {
            BulkRoundScoreRequest bulkRequest = new BulkRoundScoreRequest();
            List<PlayerBulkScoreDto> scorecards = new ArrayList<PlayerBulkScoreDto>();

            for (RoundCorrectionRequest.PlayerCorrectionDto correction : request.getPlayerCorrections()) {
                if (correction == null) {
                    continue;
                }

                PlayerBulkScoreDto dto = new PlayerBulkScoreDto();
                dto.setPlayerId(correction.getPlayerId());
                dto.setHoles(correction.getHoles());
                scorecards.add(dto);
            }

            bulkRequest.setScorecards(scorecards);

            // This path saves hole scores and then runs the full correction cascade once.
            bulkScoreEntryService.saveBulkScores(roundId, bulkRequest);
            logScoreChanges(round, scoreSnapshots);
        } else if (changedWithoutBulkScoreSave) {
            // Tee/handicap-only corrections still affect net scores, game results, history, standings, and payouts.
            roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
        }

        // Keep the finalized flag stable for post-finalization corrections.
        Round refreshedRound = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found after correction: " + roundId));

        if (Boolean.TRUE.equals(wasFinalized) && !Boolean.TRUE.equals(refreshedRound.getFinalized())) {
            refreshedRound.setFinalized(true);
            roundRepository.save(refreshedRound);
            roundRecalculationOrchestrationService.handlePostRoundChange(roundId);
        }

        return new RoundCorrectionResponse(
                true,
                "Correction applied and recalculated successfully"
        );
    }

    private void validateRequest(RoundCorrectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Correction request is required");
        }

        boolean hasPlayerCorrections = request.getPlayerCorrections() != null && !request.getPlayerCorrections().isEmpty();
        boolean hasTeeCorrections = request.getTeeCorrections() != null && !request.getTeeCorrections().isEmpty();
        boolean refreshHandicaps = Boolean.TRUE.equals(request.getRefreshHandicaps());

        if (!hasPlayerCorrections && !hasTeeCorrections && !refreshHandicaps) {
            throw new IllegalArgumentException("At least one score, tee, or handicap correction is required");
        }
    }

    private List<ScorecardSnapshot> snapshotTeeCorrections(Long roundId, List<RoundTeeCorrectionRequest> corrections) {
        List<ScorecardSnapshot> snapshots = new ArrayList<ScorecardSnapshot>();

        if (corrections == null) {
            return snapshots;
        }

        for (RoundTeeCorrectionRequest correction : corrections) {
            if (correction == null || correction.getScorecardId() == null) {
                continue;
            }

            Scorecard scorecard = scorecardRepository.findById(correction.getScorecardId()).orElse(null);
            if (scorecard == null || scorecard.getRound() == null || scorecard.getRound().getId() == null
                    || !scorecard.getRound().getId().equals(roundId)) {
                continue;
            }

            ScorecardSnapshot snapshot = buildSnapshot(scorecard);
            snapshots.add(snapshot);
        }

        return snapshots;
    }

    private List<ScorecardSnapshot> snapshotPlayerCorrections(Long roundId, List<RoundCorrectionRequest.PlayerCorrectionDto> corrections) {
        List<ScorecardSnapshot> snapshots = new ArrayList<ScorecardSnapshot>();

        if (corrections == null) {
            return snapshots;
        }

        for (RoundCorrectionRequest.PlayerCorrectionDto correction : corrections) {
            if (correction == null || correction.getPlayerId() == null) {
                continue;
            }

            Scorecard scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(roundId, correction.getPlayerId()).orElse(null);
            if (scorecard == null) {
                continue;
            }

            ScorecardSnapshot snapshot = buildSnapshot(scorecard);
            snapshots.add(snapshot);
        }

        return snapshots;
    }

    private List<ScorecardSnapshot> snapshotAllRoundScorecards(Long roundId) {
        List<ScorecardSnapshot> snapshots = new ArrayList<ScorecardSnapshot>();
        List<Scorecard> scorecards = scorecardRepository.findByRound_IdOrderByIdAsc(roundId);

        for (Scorecard scorecard : scorecards) {
            snapshots.add(buildSnapshot(scorecard));
        }

        return snapshots;
    }

    private ScorecardSnapshot buildSnapshot(Scorecard scorecard) {
        ScorecardSnapshot snapshot = new ScorecardSnapshot();
        snapshot.scorecardId = scorecard.getId();
        snapshot.roundTeeName = scorecard.getRoundTee() != null ? scorecard.getRoundTee().getTeeName() : null;
        snapshot.courseHandicap = scorecard.getCourseHandicap();
        snapshot.playingHandicap = scorecard.getPlayingHandicap();
        snapshot.grossScore = scorecard.getGrossScore();
        snapshot.netScore = scorecard.getNetScore();
        snapshot.holes = getHoleScoreText(scorecard.getId());
        return snapshot;
    }

    private String getHoleScoreText(Long scorecardId) {
        List<HoleScore> holeScores = holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(scorecardId);
        List<String> values = new ArrayList<String>();

        for (HoleScore holeScore : holeScores) {
            values.add(String.valueOf(holeScore.getStrokes()));
        }

        return String.join(",", values);
    }

    private void logTeeChanges(Round round, List<ScorecardSnapshot> snapshots) {
        for (ScorecardSnapshot before : snapshots) {
            Scorecard afterScorecard = scorecardRepository.findById(before.scorecardId).orElse(null);
            if (afterScorecard == null) {
                continue;
            }

            String beforeText = "tee=" + before.roundTeeName
                    + ", courseHandicap=" + before.courseHandicap
                    + ", playingHandicap=" + before.playingHandicap;
            String afterText = "tee=" + (afterScorecard.getRoundTee() != null ? afterScorecard.getRoundTee().getTeeName() : null)
                    + ", courseHandicap=" + afterScorecard.getCourseHandicap()
                    + ", playingHandicap=" + afterScorecard.getPlayingHandicap();

            roundCorrectionLogService.logCorrectionSafely(
                    round,
                    afterScorecard.getPlayer(),
                    RoundCorrectionType.TEE_CHANGE,
                    beforeText,
                    afterText
            );
        }
    }

    private void logHandicapRefreshes(Round round, List<ScorecardSnapshot> snapshots) {
        for (ScorecardSnapshot before : snapshots) {
            Scorecard afterScorecard = scorecardRepository.findById(before.scorecardId).orElse(null);
            if (afterScorecard == null) {
                continue;
            }

            String beforeText = "courseHandicap=" + before.courseHandicap
                    + ", playingHandicap=" + before.playingHandicap
                    + ", netScore=" + before.netScore;
            String afterText = "courseHandicap=" + afterScorecard.getCourseHandicap()
                    + ", playingHandicap=" + afterScorecard.getPlayingHandicap()
                    + ", netScore=" + afterScorecard.getNetScore();

            roundCorrectionLogService.logCorrectionSafely(
                    round,
                    afterScorecard.getPlayer(),
                    RoundCorrectionType.HANDICAP_REFRESH,
                    beforeText,
                    afterText
            );
        }
    }

    private void logScoreChanges(Round round, List<ScorecardSnapshot> snapshots) {
        for (ScorecardSnapshot before : snapshots) {
            Scorecard afterScorecard = scorecardRepository.findById(before.scorecardId).orElse(null);
            if (afterScorecard == null) {
                continue;
            }

            String beforeText = "holes=" + before.holes
                    + ", grossScore=" + before.grossScore
                    + ", netScore=" + before.netScore;
            String afterText = "holes=" + getHoleScoreText(afterScorecard.getId())
                    + ", grossScore=" + afterScorecard.getGrossScore()
                    + ", netScore=" + afterScorecard.getNetScore();

            roundCorrectionLogService.logCorrectionSafely(
                    round,
                    afterScorecard.getPlayer(),
                    RoundCorrectionType.SCORE_CHANGE,
                    beforeText,
                    afterText
            );
        }
    }

    private static class ScorecardSnapshot {
        private Long scorecardId;
        private String roundTeeName;
        private Integer courseHandicap;
        private Integer playingHandicap;
        private Integer grossScore;
        private Integer netScore;
        private String holes;
    }
}
