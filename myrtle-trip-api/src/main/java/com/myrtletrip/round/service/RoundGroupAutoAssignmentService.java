package com.myrtletrip.round.service;

import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundGroupPlayerRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoundGroupAutoAssignmentService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final RoundGroupPlayerRepository roundGroupPlayerRepository;

    public RoundGroupAutoAssignmentService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            RoundGroupRepository roundGroupRepository,
            RoundGroupPlayerRepository roundGroupPlayerRepository
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.roundGroupPlayerRepository = roundGroupPlayerRepository;
    }

    @Transactional
    public void syncGroupsFromTeamsIfNeeded(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        if (round.getFormat() != RoundFormat.TWO_MAN_LOW_NET && round.getFormat() != RoundFormat.TEAM_SCRAMBLE) {
            return;
        }

        if (Boolean.TRUE.equals(round.getFinalized())) {
            throw new IllegalStateException("Cannot update groups after round is finalized.");
        }

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
        if (teams == null || teams.isEmpty()) {
            clearExistingGroups(roundId);
            return;
        }

        if (round.getFormat() == RoundFormat.TWO_MAN_LOW_NET) {
            syncTwoManGroups(round, roundId, teams);
            return;
        }

        if (round.getFormat() == RoundFormat.TEAM_SCRAMBLE) {
            syncScrambleGroups(round, roundId, teams);
        }
    }

    private void syncTwoManGroups(Round round, Long roundId, List<RoundTeam> teams) {
        List<RoundTeam> completeTeams = new ArrayList<>();

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                continue;
            }

            List<RoundTeamPlayer> teamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId());

            if (teamPlayers != null && teamPlayers.size() == 2) {
                completeTeams.add(team);
            }
        }

        clearExistingGroups(roundId);

        int groupNumber = 1;

        for (int i = 0; i + 1 < completeTeams.size(); i += 2) {
            RoundTeam firstTeam = completeTeams.get(i);
            RoundTeam secondTeam = completeTeams.get(i + 1);

            List<RoundTeamPlayer> firstTeamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(firstTeam.getId());
            List<RoundTeamPlayer> secondTeamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(secondTeam.getId());

            if (firstTeamPlayers.size() != 2 || secondTeamPlayers.size() != 2) {
                continue;
            }

            RoundGroup group = new RoundGroup();
            group.setRound(round);
            group.setGroupNumber(groupNumber);

            addGroupPlayer(group, firstTeamPlayers.get(0), 1);
            addGroupPlayer(group, firstTeamPlayers.get(1), 2);
            addGroupPlayer(group, secondTeamPlayers.get(0), 3);
            addGroupPlayer(group, secondTeamPlayers.get(1), 4);

            roundGroupRepository.save(group);
            groupNumber++;
        }
    }

    private void syncScrambleGroups(Round round, Long roundId, List<RoundTeam> teams) {
        clearExistingGroups(roundId);

        int expectedTeamSize = resolveScrambleTeamSize(round);
        int groupNumber = 1;

        for (RoundTeam team : teams) {
            if (team == null || team.getId() == null) {
                continue;
            }

            List<RoundTeamPlayer> teamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId());

            if (teamPlayers == null || teamPlayers.size() != expectedTeamSize) {
                continue;
            }

            RoundGroup group = new RoundGroup();
            group.setRound(round);
            group.setGroupNumber(groupNumber);

            int seatOrder = 1;
            for (RoundTeamPlayer teamPlayer : teamPlayers) {
                addGroupPlayer(group, teamPlayer, seatOrder);
                seatOrder++;
            }

            roundGroupRepository.save(group);
            groupNumber++;
        }
    }

    private int resolveScrambleTeamSize(Round round) {
        if (round == null || round.getScrambleTeamSize() == null || round.getScrambleTeamSize() < 1) {
            return 4;
        }
        return round.getScrambleTeamSize();
    }

    private void addGroupPlayer(RoundGroup group, RoundTeamPlayer teamPlayer, int seatOrder) {
        if (teamPlayer == null || teamPlayer.getPlayer() == null) {
            return;
        }

        RoundGroupPlayer groupPlayer = new RoundGroupPlayer();
        groupPlayer.setPlayer(teamPlayer.getPlayer());
        groupPlayer.setSeatOrder(seatOrder);
        group.addPlayer(groupPlayer);
    }

    private void clearExistingGroups(Long roundId) {
        roundGroupPlayerRepository.deleteByRoundGroup_Round_Id(roundId);
        roundGroupPlayerRepository.flush();

        roundGroupRepository.deleteByRound_Id(roundId);
        roundGroupRepository.flush();
    }
 }
