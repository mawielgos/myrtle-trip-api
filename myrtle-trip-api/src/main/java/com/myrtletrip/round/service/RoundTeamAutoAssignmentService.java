package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoundTeamAutoAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final ScorecardRepository scorecardRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;

    public RoundTeamAutoAssignmentService(
            RoundRepository roundRepository,
            RoundGroupRepository roundGroupRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            ScorecardRepository scorecardRepository,
            TeamHoleScoreRepository teamHoleScoreRepository
    ) {
        this.roundRepository = roundRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
    }

    @Transactional
    public void syncTeamsFromGroupsIfNeeded(Long roundId) {
        syncTeamsFromGroups(roundId, false);
    }

    @Transactional
    public void rebuildTeamsFromGroups(Long roundId) {
        syncTeamsFromGroups(roundId, true);
    }

    private void syncTeamsFromGroups(Long roundId, boolean forceRebuild) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        RoundFormat format = round.getFormat();
        if (format == null) {
            return;
        }

        // Auto-build teams from tee-sheet groups for team formats where a group is also the competition team.
        if (!format.requiresTeams()) {
            return;
        }

        int expectedTeamSize = round.getFormat().expectedTeamSize();
        if (format != RoundFormat.TEAM_SCRAMBLE && expectedTeamSize != 4) {
            return;
        }

        List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        if (groups == null || groups.isEmpty()) {
            return;
        }

        List<RoundTeam> existingTeams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        if (!forceRebuild && existingTeams != null && !existingTeams.isEmpty()) {
            return;
        }

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        Map<Long, Scorecard> scorecardByPlayerId = new HashMap<Long, Scorecard>();

        for (Scorecard scorecard : scorecards) {
            if (scorecard.getPlayer() != null && scorecard.getPlayer().getId() != null) {
                scorecardByPlayerId.put(scorecard.getPlayer().getId(), scorecard);
            }
        }

        Map<Integer, RoundTeam> existingTeamByNumber = new HashMap<Integer, RoundTeam>();
        if (existingTeams != null) {
            for (RoundTeam existingTeam : existingTeams) {
                if (existingTeam.getTeamNumber() != null) {
                    existingTeamByNumber.put(existingTeam.getTeamNumber(), existingTeam);
                }
            }
        }

        Set<Integer> activeTeamNumbers = new HashSet<Integer>();

        // Clear scorecard team links first. We will re-link below using stable RoundTeam rows.
        for (Scorecard scorecard : scorecards) {
            scorecard.setTeam(null);
        }
        scorecardRepository.saveAll(scorecards);

        // Rebuild team membership, but do not delete/recreate RoundTeam rows just because
        // groups are being synced. Scramble scores live on RoundTeam and TeamHoleScore, so
        // preserving the RoundTeam id is what keeps saved scramble scores from disappearing
        // after returning through Trip Detail or refreshing round list data.
        roundTeamPlayerRepository.deleteByRoundTeam_Round_Id(roundId);

        for (RoundGroup group : groups) {
            if (group.getGroupNumber() == null) {
                continue;
            }

            Integer teamNumber = group.getGroupNumber();
            activeTeamNumbers.add(teamNumber);

            RoundTeam team = existingTeamByNumber.get(teamNumber);
            if (team == null) {
                team = new RoundTeam();
                team.setRound(round);
                team.setTeamNumber(teamNumber);
            }

            team.setTeamName("Team " + teamNumber);
            team = roundTeamRepository.save(team);

            List<RoundGroupPlayer> groupPlayers = group.getPlayers();
            if (groupPlayers == null) {
                continue;
            }

            int fallbackOrder = 1;

            for (RoundGroupPlayer groupPlayer : groupPlayers) {
                if (groupPlayer == null || groupPlayer.getPlayer() == null || groupPlayer.getPlayer().getId() == null) {
                    continue;
                }

                RoundTeamPlayer teamPlayer = new RoundTeamPlayer();
                teamPlayer.setRoundTeam(team);
                teamPlayer.setPlayer(groupPlayer.getPlayer());
                teamPlayer.setPlayerOrder(
                        groupPlayer.getSeatOrder() != null ? groupPlayer.getSeatOrder() : fallbackOrder
                );
                roundTeamPlayerRepository.save(teamPlayer);

                Scorecard scorecard = scorecardByPlayerId.get(groupPlayer.getPlayer().getId());
                if (scorecard != null) {
                    scorecard.setTeam(team);
                }

                fallbackOrder++;
            }
        }

        scorecardRepository.saveAll(scorecards);

        // Remove truly obsolete teams only after preserving/reusing matching team numbers.
        // Delete team-hole scores first so obsolete scramble teams do not leave FK rows behind.
        if (existingTeams != null) {
            for (RoundTeam existingTeam : existingTeams) {
                Integer teamNumber = existingTeam.getTeamNumber();
                if (teamNumber != null && activeTeamNumbers.contains(teamNumber)) {
                    continue;
                }

                teamHoleScoreRepository.deleteByRoundTeam_Id(existingTeam.getId());
                roundTeamRepository.delete(existingTeam);
            }
        }
    }
}
