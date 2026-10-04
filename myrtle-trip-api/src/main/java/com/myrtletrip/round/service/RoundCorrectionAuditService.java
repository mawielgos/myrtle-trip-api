package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundCorrectionRequest;
import com.myrtletrip.round.dto.RoundTeeCorrectionRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundCorrectionType;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundCorrectionAuditService {

    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final RoundCorrectionLogService roundCorrectionLogService;

    public RoundCorrectionAuditService(ScorecardRepository scorecardRepository,
                                       HoleScoreRepository holeScoreRepository,
                                       RoundCorrectionLogService roundCorrectionLogService) {
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.roundCorrectionLogService = roundCorrectionLogService;
    }

    public List<ScorecardSnapshot> snapshotParticipationCorrections(Long roundId, List<RoundCorrectionRequest.ParticipationCorrectionDto> corrections) {
        List<ScorecardSnapshot> snapshots = new ArrayList<>();
        if (corrections == null) return snapshots;
        for (RoundCorrectionRequest.ParticipationCorrectionDto correction : corrections) {
            if (correction == null || correction.getScorecardId() == null) continue;
            Scorecard scorecard = scorecardRepository.findById(correction.getScorecardId()).orElse(null);
            if (!belongsToRound(scorecard, roundId)) continue;
            snapshots.add(buildSnapshot(scorecard));
        }
        return snapshots;
    }

    public List<ScorecardSnapshot> snapshotTeeCorrections(Long roundId, List<RoundTeeCorrectionRequest> corrections) {
        List<ScorecardSnapshot> snapshots = new ArrayList<>();
        if (corrections == null) return snapshots;
        for (RoundTeeCorrectionRequest correction : corrections) {
            if (correction == null || correction.getScorecardId() == null) continue;
            Scorecard scorecard = scorecardRepository.findById(correction.getScorecardId()).orElse(null);
            if (!belongsToRound(scorecard, roundId)) continue;
            snapshots.add(buildSnapshot(scorecard));
        }
        return snapshots;
    }

    public List<ScorecardSnapshot> snapshotPlayerCorrections(Long roundId, List<RoundCorrectionRequest.PlayerCorrectionDto> corrections) {
        List<ScorecardSnapshot> snapshots = new ArrayList<>();
        if (corrections == null) return snapshots;
        for (RoundCorrectionRequest.PlayerCorrectionDto correction : corrections) {
            if (correction == null || correction.getPlayerId() == null) continue;
            Scorecard scorecard = scorecardRepository.findByRound_IdAndPlayer_Id(roundId, correction.getPlayerId()).orElse(null);
            if (scorecard != null) snapshots.add(buildSnapshot(scorecard));
        }
        return snapshots;
    }

    public List<ScorecardSnapshot> snapshotAllRoundScorecards(Long roundId) {
        List<ScorecardSnapshot> snapshots = new ArrayList<>();
        for (Scorecard scorecard : scorecardRepository.findByRound_IdOrderByIdAsc(roundId)) {
            snapshots.add(buildSnapshot(scorecard));
        }
        return snapshots;
    }

    public void logTeeChanges(Round round, List<ScorecardSnapshot> snapshots) {
        for (ScorecardSnapshot before : snapshots) {
            Scorecard after = scorecardRepository.findById(before.scorecardId).orElse(null);
            if (after == null) continue;
            String beforeText = "tee=" + before.roundTeeName + ", courseHandicap=" + before.courseHandicap + ", playingHandicap=" + before.playingHandicap;
            String afterText = "tee=" + (after.getRoundTee() != null ? after.getRoundTee().getTeeName() : null)
                    + ", courseHandicap=" + after.getCourseHandicap() + ", playingHandicap=" + after.getPlayingHandicap();
            roundCorrectionLogService.logCorrectionSafely(round, after.getPlayer(), RoundCorrectionType.TEE_CHANGE, beforeText, afterText);
        }
    }

    public void logHandicapRefreshes(Round round, List<ScorecardSnapshot> snapshots) {
        for (ScorecardSnapshot before : snapshots) {
            Scorecard after = scorecardRepository.findById(before.scorecardId).orElse(null);
            if (after == null) continue;
            String beforeText = "courseHandicap=" + before.courseHandicap + ", playingHandicap=" + before.playingHandicap + ", netScore=" + before.netScore;
            String afterText = "courseHandicap=" + after.getCourseHandicap() + ", playingHandicap=" + after.getPlayingHandicap() + ", netScore=" + after.getNetScore();
            roundCorrectionLogService.logCorrectionSafely(round, after.getPlayer(), RoundCorrectionType.HANDICAP_REFRESH, beforeText, afterText);
        }
    }

    public void logParticipationChanges(Round round, List<ScorecardSnapshot> snapshots) {
        for (ScorecardSnapshot before : snapshots) {
            Scorecard after = scorecardRepository.findById(before.scorecardId).orElse(null);
            if (after == null) continue;
            String beforeText = "participationStatus=" + before.participationStatus + ", withdrawalHoleNumber=" + before.withdrawalHoleNumber
                    + ", grossScore=" + before.grossScore + ", netScore=" + before.netScore;
            String afterText = "participationStatus=" + after.getParticipationStatus() + ", withdrawalHoleNumber=" + after.getWithdrawalHoleNumber()
                    + ", grossScore=" + after.getGrossScore() + ", netScore=" + after.getNetScore();
            roundCorrectionLogService.logCorrectionSafely(round, after.getPlayer(), RoundCorrectionType.PARTICIPATION_CHANGE, beforeText, afterText);
        }
    }

    public void logScoreChanges(Round round, List<ScorecardSnapshot> snapshots) {
        for (ScorecardSnapshot before : snapshots) {
            Scorecard after = scorecardRepository.findById(before.scorecardId).orElse(null);
            if (after == null) continue;
            String beforeText = "holes=" + before.holes + ", grossScore=" + before.grossScore + ", netScore=" + before.netScore;
            String afterText = "holes=" + getHoleScoreText(after.getId()) + ", grossScore=" + after.getGrossScore() + ", netScore=" + after.getNetScore();
            roundCorrectionLogService.logCorrectionSafely(round, after.getPlayer(), RoundCorrectionType.SCORE_CHANGE, beforeText, afterText);
        }
    }

    private boolean belongsToRound(Scorecard scorecard, Long roundId) {
        return scorecard != null && scorecard.getRound() != null && scorecard.getRound().getId() != null
                && scorecard.getRound().getId().equals(roundId);
    }

    private ScorecardSnapshot buildSnapshot(Scorecard scorecard) {
        ScorecardSnapshot snapshot = new ScorecardSnapshot();
        snapshot.scorecardId = scorecard.getId();
        snapshot.roundTeeName = scorecard.getRoundTee() != null ? scorecard.getRoundTee().getTeeName() : null;
        snapshot.courseHandicap = scorecard.getCourseHandicap();
        snapshot.playingHandicap = scorecard.getPlayingHandicap();
        snapshot.grossScore = scorecard.getGrossScore();
        snapshot.netScore = scorecard.getNetScore();
        snapshot.participationStatus = scorecard.getParticipationStatus() == null ? null : scorecard.getParticipationStatus().name();
        snapshot.withdrawalHoleNumber = scorecard.getWithdrawalHoleNumber();
        snapshot.holes = getHoleScoreText(scorecard.getId());
        return snapshot;
    }

    private String getHoleScoreText(Long scorecardId) {
        List<String> values = new ArrayList<>();
        for (HoleScore holeScore : holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(scorecardId)) {
            values.add(String.valueOf(holeScore.getStrokes()));
        }
        return String.join(",", values);
    }

    public static class ScorecardSnapshot {
        private Long scorecardId;
        private String roundTeeName;
        private Integer courseHandicap;
        private Integer playingHandicap;
        private Integer grossScore;
        private Integer netScore;
        private String participationStatus;
        private Integer withdrawalHoleNumber;
        private String holes;
    }
}
