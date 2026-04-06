package com.myrtletrip.games.service;

import com.myrtletrip.games.dto.RoundGameResult;
import com.myrtletrip.games.model.RoundScoringData;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoundGameScoringService {

    private final RoundRepository roundRepository;
    private final RoundScoringDataService roundScoringDataService;
    private final Map<com.myrtletrip.round.model.RoundFormat, RoundGameScorer> scorers;

    public RoundGameScoringService(RoundRepository roundRepository,
                                   RoundScoringDataService roundScoringDataService,
                                   List<RoundGameScorer> scorerList) {
        this.roundRepository = roundRepository;
        this.roundScoringDataService = roundScoringDataService;
        this.scorers = scorerList.stream()
                .collect(Collectors.toMap(RoundGameScorer::supports, Function.identity()));
    }

    public RoundGameResult scoreRound(Long roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new IllegalArgumentException("Round not found: " + roundId));

        RoundGameScorer scorer = scorers.get(round.getFormat());
        if (scorer == null) {
            throw new IllegalStateException("No scorer registered for format " + round.getFormat());
        }

        RoundScoringData data = roundScoringDataService.build(round);

        return scorer.scoreRound(data);
    }
}