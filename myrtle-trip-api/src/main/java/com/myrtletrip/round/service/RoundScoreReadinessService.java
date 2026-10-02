package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoundScoreReadinessService {

    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundScoreReadinessService(
            TeamHoleScoreRepository teamHoleScoreRepository,
            RoundTeeResolver roundTeeResolver,
            RoundEventCapabilityService roundEventCapabilityService
    ) {
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    public boolean isActiveScorecard(Scorecard scorecard) {
        if (scorecard == null || scorecard.getParticipationStatus() == null) {
            return true;
        }
        if (scorecard.getParticipationStatus() == ScorecardParticipationStatus.ACTIVE) {
            return true;
        }
        return scorecard.getParticipationStatus() == ScorecardParticipationStatus.WITHDRAWN
                && scorecard.getWithdrawalHoleNumber() != null
                && scorecard.getWithdrawalHoleNumber() > 0;
    }

    public boolean allScorecardsHaveTee(List<Scorecard> scorecards) {
        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                return false;
            }
            try {
                RoundTee effectiveRoundTee = roundTeeResolver.resolve(scorecard);
                if (effectiveRoundTee == null || effectiveRoundTee.getId() == null) {
                    return false;
                }
            } catch (RuntimeException ex) {
                return false;
            }
        }
        return true;
    }

    public boolean areHandicapsReady(Round round, List<Scorecard> scorecards) {
        if (!roundEventCapabilityService.requiresNetScores(round)) {
            return true;
        }

        for (Scorecard scorecard : scorecards) {
            if (scorecard == null) {
                return false;
            }
            if (scorecard.getCourseHandicap() == null || scorecard.getPlayingHandicap() == null) {
                return false;
            }
        }

        return true;
    }

    public int calculateMissingScoreCount(Round round, List<Scorecard> scorecards, List<RoundTeam> teams) {
        if (round == null) {
            return 1;
        }

        RoundEventCapabilityService.RoundEventCapabilities capabilities = roundEventCapabilityService.getCapabilities(round);
        int missing = 0;

        if (capabilities.hasScrambleEvent()) {
            missing += calculateMissingScrambleTeamScoreCount(teams);
        }

        if (capabilities.requiresPlayerScorecards()) {
            missing += calculateMissingPlayerScoreCount(scorecards);
        }

        return missing;
    }

    private int calculateMissingScrambleTeamScoreCount(List<RoundTeam> teams) {
        int missing = 0;
        if (teams == null || teams.isEmpty()) {
            return 1;
        }
        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                missing++;
                continue;
            }
            if (team.getScrambleTotalScore() != null) {
                continue;
            }
            List<TeamHoleScore> holeScores = teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(team.getId());
            if (holeScores == null || holeScores.size() != 18) {
                missing++;
                continue;
            }
            Set<Integer> holes = new HashSet<>();
            for (TeamHoleScore holeScore : holeScores) {
                if (holeScore != null && holeScore.getHoleNumber() != null && holeScore.getStrokes() != null) {
                    holes.add(holeScore.getHoleNumber());
                }
            }
            if (holes.size() != 18) {
                missing++;
            }
        }
        return missing;
    }

    private int calculateMissingPlayerScoreCount(List<Scorecard> scorecards) {
        if (scorecards == null || scorecards.isEmpty()) {
            return 1;
        }

        int missing = 0;
        for (Scorecard scorecard : scorecards) {
            if (!isActiveScorecard(scorecard)) {
                continue;
            }
            if (scorecard == null
                    || scorecard.getGrossScore() == null
                    || scorecard.getAdjustedGrossScore() == null
                    || scorecard.getNetScore() == null) {
                missing++;
            }
        }
        return missing;
    }
}
