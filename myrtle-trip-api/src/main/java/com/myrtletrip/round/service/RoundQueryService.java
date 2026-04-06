package com.myrtletrip.round.service;

import com.myrtletrip.round.dto.RoundPlayerStatusResponse;
import com.myrtletrip.round.dto.RoundStatusResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoundQueryService {

    private final RoundRepository roundRepository;
    private final ScorecardRepository scorecardRepository;

    public RoundQueryService(RoundRepository roundRepository,
                             ScorecardRepository scorecardRepository) {
        this.roundRepository = roundRepository;
        this.scorecardRepository = scorecardRepository;
    }

    @Transactional(readOnly = true)
    public RoundStatusResponse getRoundStatus(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found"));

        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        RoundStatusResponse dto = new RoundStatusResponse();
        dto.setRoundId(round.getId());
        dto.setCourseName(round.getCourse().getName());
        dto.setTeeName(round.getCourseTee().getTeeName());
        dto.setRoundDate(round.getRoundDate());
        dto.setFinalized(round.getFinalized());

        dto.setPlayers(scorecards.stream().map(this::toPlayerStatus).toList());

        return dto;
    }

    private RoundPlayerStatusResponse toPlayerStatus(Scorecard scorecard) {
        RoundPlayerStatusResponse dto = new RoundPlayerStatusResponse();
        dto.setScorecardId(scorecard.getId());
        dto.setPlayerId(scorecard.getPlayer().getId());
        dto.setPlayerName(scorecard.getPlayer().getDisplayName());
        dto.setCourseHandicap(scorecard.getCourseHandicap());
        dto.setPlayingHandicap(scorecard.getPlayingHandicap());
        return dto;
    }
}