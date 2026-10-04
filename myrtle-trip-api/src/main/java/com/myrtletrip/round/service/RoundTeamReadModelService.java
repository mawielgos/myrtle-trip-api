package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeamResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTeam;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoundTeamReadModelService {

    private final RoundRepository roundRepository;
    private final RoundTeamRepository roundTeamRepository;
    private final RoundTeamPlayerRepository roundTeamPlayerRepository;
    private final ScorecardRepository scorecardRepository;
    private final RoundTeeResolver roundTeeResolver;
    private final RoundEventCapabilityService roundEventCapabilityService;

    public RoundTeamReadModelService(
            RoundRepository roundRepository,
            RoundTeamRepository roundTeamRepository,
            RoundTeamPlayerRepository roundTeamPlayerRepository,
            ScorecardRepository scorecardRepository,
            RoundTeeResolver roundTeeResolver,
            RoundEventCapabilityService roundEventCapabilityService
    ) {
        this.roundRepository = roundRepository;
        this.roundTeamRepository = roundTeamRepository;
        this.roundTeamPlayerRepository = roundTeamPlayerRepository;
        this.scorecardRepository = scorecardRepository;
        this.roundTeeResolver = roundTeeResolver;
        this.roundEventCapabilityService = roundEventCapabilityService;
    }

    @Transactional(readOnly = true)
    public List<RoundTeamResponse> getTeams(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        if (!roundEventCapabilityService.requiresTeams(round)) return List.of();

        List<RoundTeam> teams = roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(roundId);

        return teams.stream().map(team -> {
            RoundTeamResponse teamResponse = new RoundTeamResponse();
            teamResponse.setRoundTeamId(team.getId());
            teamResponse.setTeamNumber(team.getTeamNumber());
            teamResponse.setTeamName(team.getTeamName());

            List<RoundTeamPlayerResponse> players = roundTeamPlayerRepository
                    .findByRoundTeam_IdOrderByPlayerOrderAsc(team.getId())
                    .stream()
                    .map(tp -> {
                        RoundTeamPlayerResponse playerResponse = new RoundTeamPlayerResponse();
                        playerResponse.setPlayerId(tp.getPlayer().getId());
                        playerResponse.setPlayerName(tp.getPlayer().getDisplayName());
                        playerResponse.setPlayerOrder(tp.getPlayerOrder());
                        playerResponse.setGender(normalizeGender(tp.getPlayer().getGender()));

                        scorecardRepository.findByRound_IdAndPlayer_Id(roundId, tp.getPlayer().getId())
                                .ifPresent(scorecard -> applyScorecardTee(playerResponse, scorecard, round));

                        return playerResponse;
                    })
                    .toList();

            teamResponse.setPlayers(players);
            return teamResponse;
        }).toList();
    }

    private void applyScorecardTee(RoundTeamPlayerResponse playerResponse, Scorecard scorecard, Round round) {
        playerResponse.setScorecardId(scorecard.getId());
        RoundTee resolved = roundTeeResolver.resolve(scorecard);
        playerResponse.setRoundTeeId(resolved.getId());
        playerResponse.setRoundTeeName(resolved.getTeeName());
        Long defaultId = round.getDefaultRoundTee() == null ? null : round.getDefaultRoundTee().getId();
        playerResponse.setTeeOverride(defaultId != null && resolved.getId() != null && !defaultId.equals(resolved.getId()));
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) return "M";
        return gender.trim().toUpperCase();
    }
}
