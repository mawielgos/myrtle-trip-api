package com.myrtletrip.games.service;

import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamHoleScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundGroup;
import com.myrtletrip.round.entity.RoundGroupPlayer;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.exceptionmodel.entity.RoundTeamException;
import com.myrtletrip.round.exceptionmodel.repository.RoundTeamExceptionRepository;
import com.myrtletrip.round.repository.RoundGroupRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.entity.TeamHoleScore;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scoreentry.repository.TeamHoleScoreRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoundScoringDataService {

    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final RoundGroupRepository roundGroupRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;
    private final RoundTeamExceptionRepository roundTeamExceptionRepository;

    public RoundScoringDataService(RoundTeamRepository roundTeamRepository,
                                   RoundTeamPlayerRepository roundTeamPlayerRepository,
                                   RoundGroupRepository roundGroupRepository,
                                   ScorecardRepository scorecardRepository,
                                   HoleScoreRepository holeScoreRepository,
                                   TeamHoleScoreRepository teamHoleScoreRepository,
                                   RoundTeamExceptionRepository roundTeamExceptionRepository) {
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.roundGroupRepository = roundGroupRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
        this.roundTeamExceptionRepository = roundTeamExceptionRepository;
    }

    public RoundScoringData build(Round round) {
        RoundScoringData data = new RoundScoringData();
        data.setRoundId(round.getId());
        data.setFormat(round.getFormat());

        List<Scorecard> roundScorecards = scorecardRepository.findByRound_Id(round.getId());
        Map<Long, Scorecard> scorecardByPlayerId = new HashMap<>();
        for (Scorecard scorecard : roundScorecards) {
            scorecardByPlayerId.put(scorecard.getPlayer().getId(), scorecard);
        }

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());
        if (!teams.isEmpty()) {
            buildFromRoundTeams(round, data, teams, scorecardByPlayerId);
            return data;
        }

        List<RoundGroup> groups = roundGroupRepository.findByRound_IdOrderByGroupNumberAsc(round.getId());
        if (!groups.isEmpty()) {
            buildFromPlayingGroups(round, data, groups, scorecardByPlayerId);
            return data;
        }

        throw new IllegalStateException("No teams or playing groups assigned for round " + round.getId());
    }

    private void buildFromRoundTeams(
            Round round,
            RoundScoringData data,
            List<RoundTeam> teams,
            Map<Long, Scorecard> scorecardByPlayerId
    ) {
        for (RoundTeam roundTeam : teams) {
            TeamScoringData teamData = new TeamScoringData();
            teamData.setTeamId(roundTeam.getId());
            teamData.setTeamName(resolveTeamName(roundTeam));
            teamData.setScrambleTotalScore(roundTeam.getScrambleTotalScore());

            List<RoundTeamPlayer> teamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(roundTeam.getId());

            for (RoundTeamPlayer teamPlayer : teamPlayers) {
                teamData.getPlayers().add(
                        buildPlayerScoringData(
                                round,
                                teamPlayer.getPlayer().getId(),
                                teamPlayer.getPlayer().getDisplayName(),
                                scorecardByPlayerId
                        )
                );
            }

            addGhostPlayerIfConfigured(round, roundTeam, teamData, scorecardByPlayerId);

            List<TeamHoleScore> scrambleScores =
                    teamHoleScoreRepository.findByRoundTeam_IdOrderByHoleNumberAsc(roundTeam.getId());

            for (TeamHoleScore teamHoleScore : scrambleScores) {
                TeamHoleScoringData holeData = new TeamHoleScoringData();
                holeData.setHoleNumber(teamHoleScore.getHoleNumber());
                holeData.setGross(teamHoleScore.getStrokes());
                holeData.setNet(teamHoleScore.getStrokes());
                teamData.getScrambleHoleScores().add(holeData);
            }

            data.getTeams().add(teamData);
        }
    }

    private void addGhostPlayerIfConfigured(
            Round round,
            RoundTeam roundTeam,
            TeamScoringData teamData,
            Map<Long, Scorecard> scorecardByPlayerId
    ) {
        if (round == null || round.getId() == null || roundTeam == null || roundTeam.getId() == null) {
            return;
        }

        List<RoundTeamException> exceptions = roundTeamExceptionRepository.findByRoundTeam_IdAndActiveTrueOrderByIdAsc(roundTeam.getId());
        for (RoundTeamException exception : exceptions) {
            if (exception == null || exception.getExceptionType() == null || exception.getGhostPlayer() == null) {
                continue;
            }
            if (!"GHOST_PLAYER".equals(exception.getExceptionType().name())) {
                continue;
            }

            Long ghostPlayerId = exception.getGhostPlayer().getId();
            if (ghostPlayerId == null) {
                continue;
            }

            boolean alreadyPresent = false;
            for (PlayerScoringData player : teamData.getPlayers()) {
                if (player != null && ghostPlayerId.equals(player.getPlayerId())) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (alreadyPresent) {
                continue;
            }

            teamData.getPlayers().add(
                    buildPlayerScoringData(
                            round,
                            ghostPlayerId,
                            "Ghost Player",
                            scorecardByPlayerId
                    )
            );
        }
    }

    private void buildFromPlayingGroups(
            Round round,
            RoundScoringData data,
            List<RoundGroup> groups,
            Map<Long, Scorecard> scorecardByPlayerId
    ) {
        for (RoundGroup group : groups) {
            TeamScoringData groupData = new TeamScoringData();
            groupData.setTeamId(group.getId());
            groupData.setTeamName(resolveGroupName(group));

            for (RoundGroupPlayer groupPlayer : group.getPlayers()) {
                groupData.getPlayers().add(
                        buildPlayerScoringData(
                                round,
                                groupPlayer.getPlayer().getId(),
                                groupPlayer.getPlayer().getDisplayName(),
                                scorecardByPlayerId
                        )
                );
            }

            data.getTeams().add(groupData);
        }
    }

    private PlayerScoringData buildPlayerScoringData(
            Round round,
            Long playerId,
            String playerName,
            Map<Long, Scorecard> scorecardByPlayerId
    ) {
        Scorecard scorecard = scorecardByPlayerId.get(playerId);
        if (scorecard == null) {
            throw new IllegalStateException(
                    "Missing scorecard for roundId=" + round.getId()
                            + ", playerId=" + playerId
            );
        }

        PlayerScoringData playerData = new PlayerScoringData();
        playerData.setPlayerId(playerId);
        playerData.setPlayerName(playerName);
        playerData.setScorecardId(scorecard.getId());
        playerData.setCourseHandicap(scorecard.getCourseHandicap());
        playerData.setPlayingHandicap(scorecard.getPlayingHandicap());

        List<HoleScore> holeScores = holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(scorecard.getId());
        for (HoleScore holeScore : holeScores) {
            PlayerHoleScoringData holeData = new PlayerHoleScoringData();
            holeData.setHoleNumber(holeScore.getHoleNumber());
            holeData.setGross(holeScore.getStrokes());
            holeData.setNet(holeScore.getNetStrokes());
            playerData.getHoles().add(holeData);
        }

        return playerData;
    }

    private String resolveGroupName(RoundGroup group) {
        if (group.getGroupNumber() != null) {
            return "Group " + group.getGroupNumber();
        }
        return "Group";
    }

    private String resolveTeamName(RoundTeam roundTeam) {
        if (roundTeam.getTeamName() != null && !roundTeam.getTeamName().isBlank()) {
            return roundTeam.getTeamName();
        }
        if (roundTeam.getTeamNumber() != null) {
            return "Team " + roundTeam.getTeamNumber();
        }
        return "Team";
    }
}
