package com.myrtletrip.games.service;

import com.myrtletrip.games.model.PlayerHoleScoringData;
import com.myrtletrip.games.model.PlayerScoringData;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.games.model.TeamHoleScoringData;
import com.myrtletrip.games.model.TeamScoringData;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
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
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final TeamHoleScoreRepository teamHoleScoreRepository;

    public RoundScoringDataService(RoundTeamRepository roundTeamRepository,
                                   RoundTeamPlayerRepository roundTeamPlayerRepository,
                                   ScorecardRepository scorecardRepository,
                                   HoleScoreRepository holeScoreRepository,
                                   TeamHoleScoreRepository teamHoleScoreRepository) {
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.teamHoleScoreRepository = teamHoleScoreRepository;
    }

    public RoundScoringData build(Round round) {
        RoundScoringData data = new RoundScoringData();
        data.setRoundId(round.getId());
        data.setFormat(round.getFormat());

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(round.getId());
        if (teams.isEmpty()) {
            throw new IllegalStateException("No teams assigned for round " + round.getId());
        }

        List<Scorecard> roundScorecards = scorecardRepository.findByRound_Id(round.getId());
        Map<Long, Scorecard> scorecardByPlayerId = new HashMap<>();
        for (Scorecard scorecard : roundScorecards) {
            scorecardByPlayerId.put(scorecard.getPlayer().getId(), scorecard);
        }

        for (RoundTeam roundTeam : teams) {
            TeamScoringData teamData = new TeamScoringData();
            teamData.setTeamId(roundTeam.getId());
            teamData.setTeamName(resolveTeamName(roundTeam));

            List<RoundTeamPlayer> teamPlayers =
                    roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(roundTeam.getId());

            for (RoundTeamPlayer teamPlayer : teamPlayers) {
                Scorecard scorecard = scorecardByPlayerId.get(teamPlayer.getPlayer().getId());
                if (scorecard == null) {
                    throw new IllegalStateException(
                            "Missing scorecard for roundId=" + round.getId()
                                    + ", playerId=" + teamPlayer.getPlayer().getId()
                    );
                }

                PlayerScoringData playerData = new PlayerScoringData();
                playerData.setPlayerId(teamPlayer.getPlayer().getId());
                playerData.setPlayerName(teamPlayer.getPlayer().getDisplayName());
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

                teamData.getPlayers().add(playerData);
            }

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

        return data;
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