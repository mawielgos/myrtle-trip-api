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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundTeamAutoAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final ScorecardRepository scorecardRepository;

    public RoundTeamAutoAssignmentService(
            RoundRepository roundRepository,
            RoundGroupRepository roundGroupRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            ScorecardRepository scorecardRepository
    ) {
        this.roundRepository = roundRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
    }

    @Transactional
    public void syncTeamsFromGroupsIfNeeded(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        RoundFormat format = round.getFormat();
        if (format == null) {
            return;
        }

        // Only auto-build teams from groups for the 4-man team formats.
        if (!format.requiresTeams() || format.expectedTeamSize() != 4) {
            return;
        }

        List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(roundId);
        if (groups == null || groups.isEmpty()) {
            return;
        }

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);
        Map<Long, Scorecard> scorecardByPlayerId = new HashMap<>();

        for (Scorecard scorecard : scorecards) {
            if (scorecard.getPlayer() != null && scorecard.getPlayer().getId() != null) {
                scorecardByPlayerId.put(scorecard.getPlayer().getId(), scorecard);
            }
        }

        // Clear existing scorecard team links first
        for (Scorecard scorecard : scorecards) {
            scorecard.setTeam(null);
        }
        scorecardRepository.saveAll(scorecards);

        // Remove existing round teams/players and rebuild from groups
        roundTeamPlayerRepository.deleteByRoundTeam_Round_Id(roundId);
        roundTeamRepository.deleteByRound_Id(roundId);

        for (RoundGroup group : groups) {
            RoundTeam team = new RoundTeam();
            team.setRound(round);
            team.setTeamNumber(group.getGroupNumber());
            team.setTeamName("Team " + group.getGroupNumber());
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
    }
}
