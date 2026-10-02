package com.myrtletrip.round.service;

import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.round.dto.ScorecardParticipationRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoundParticipationService {

    private final RoundRepository roundRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundCapabilityService roundCapabilityService;

    public RoundParticipationService(
            RoundRepository roundRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            ScorecardRepository scorecardRepository,
            RoundCapabilityService roundCapabilityService) {
        this.roundRepository = roundRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundCapabilityService = roundCapabilityService;
    }

    @Transactional
    public void updateScorecardParticipation(
            Long roundId,
            Long scorecardId,
            ScorecardParticipationRequest request) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));
        roundCapabilityService.assertCanAssignTeams(round);

        Scorecard scorecard = scorecardRepository.findById(scorecardId)
                .orElseThrow(() -> new IllegalArgumentException("Scorecard not found: " + scorecardId));
        if (scorecard.getRound() == null || scorecard.getRound().getId() == null
                || !scorecard.getRound().getId().equals(roundId)) {
            throw new IllegalArgumentException("Scorecard does not belong to this round.");
        }

        ScorecardParticipationStatus nextStatus =
                parseParticipationStatus(request == null ? null : request.getParticipationStatus());
        scorecard.setParticipationStatus(nextStatus);

        if (nextStatus == ScorecardParticipationStatus.WITHDRAWN) {
            scorecard.setWithdrawalHoleNumber(
                    normalizeWithdrawalHoleNumber(request == null ? null : request.getWithdrawalHoleNumber()));
        } else {
            scorecard.setWithdrawalHoleNumber(null);
        }

        if (nextStatus != ScorecardParticipationStatus.ACTIVE) {
            scorecard.setTeam(null);
            removeTeamPlayerRows(
                    roundId,
                    scorecard.getPlayer() == null ? null : scorecard.getPlayer().getId());
        }

        scorecardRepository.save(scorecard);
    }

    private ScorecardParticipationStatus parseParticipationStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ScorecardParticipationStatus.ACTIVE;
        }
        try {
            return ScorecardParticipationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported participation status: " + value);
        }
    }

    private Integer normalizeWithdrawalHoleNumber(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0 || value > 18) {
            throw new IllegalArgumentException("Withdrawal hole must be between 0 and 18.");
        }
        return value;
    }

    private void removeTeamPlayerRows(Long roundId, Long playerId) {
        if (roundId == null || playerId == null) {
            return;
        }

        List<RoundTeamPlayer> existingRows =
                roundTeamPlayerRepository.findForRoundOrderedByTeamNumberAndPlayerOrder(roundId);
        for (RoundTeamPlayer row : existingRows) {
            if (row != null && row.getPlayer() != null && playerId.equals(row.getPlayer().getId())) {
                roundTeamPlayerRepository.delete(row);
            }
        }
    }
}
