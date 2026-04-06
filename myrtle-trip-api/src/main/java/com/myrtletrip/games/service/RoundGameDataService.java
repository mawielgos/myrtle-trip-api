package com.myrtletrip.games.service;

import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTeamPlayer;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoundGameDataService {

    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;

    public RoundGameDataService(RoundTeamRepository roundTeamRepository,
                                RoundTeamPlayerRepository roundTeamPlayerRepository,
                                ScorecardRepository scorecardRepository,
                                HoleScoreRepository holeScoreRepository) {
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
    }

    public List<RoundTeam> getTeams(Long roundId) {
        return roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);
    }

    public List<RoundTeamPlayer> getTeamPlayers(Long roundTeamId) {
        return roundTeamPlayerRepository.findByRoundTeam_IdOrderByPlayerOrderAsc(roundTeamId);
    }

    public Map<Long, Scorecard> getScorecardsByPlayerId(Long roundId) {
        return scorecardRepository.findByRound_Id(roundId).stream()
                .collect(Collectors.toMap(sc -> sc.getPlayer().getId(), sc -> sc));
    }

    public Map<Long, List<HoleScore>> getHoleScoresByScorecardId(Long roundId) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        Map<Long, List<HoleScore>> result = new HashMap<>();

        for (Scorecard sc : scorecards) {
            result.put(
                    sc.getId(),
                    holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(sc.getId())
            );
        }

        return result;
    }
}
