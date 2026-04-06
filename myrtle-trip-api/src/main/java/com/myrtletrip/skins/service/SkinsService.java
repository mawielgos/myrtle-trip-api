package com.myrtletrip.skins.service;

import com.myrtletrip.contest.entity.ContestResult;
import com.myrtletrip.contest.repository.ContestResultRepository;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.skins.dto.PlayerSkinSummaryResponse;
import com.myrtletrip.skins.dto.SkinHoleResultResponse;
import com.myrtletrip.skins.dto.SkinsResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SkinsService {

    private static final String NET_SKINS = "NET_SKINS";

    private final ScorecardRepository scorecardRepository;
    private final HoleScoreRepository holeScoreRepository;
    private final ContestResultRepository contestResultRepository;
    private final RoundRepository roundRepository;

    public SkinsService(ScorecardRepository scorecardRepository,
                        HoleScoreRepository holeScoreRepository,
                        ContestResultRepository contestResultRepository,
                        RoundRepository roundRepository) {
        this.scorecardRepository = scorecardRepository;
        this.holeScoreRepository = holeScoreRepository;
        this.contestResultRepository = contestResultRepository;
        this.roundRepository = roundRepository;
    }

    public SkinsResultResponse getNetSkins(Long roundId) {
        List<Scorecard> scorecards = scorecardRepository.findByRound_Id(roundId);

        List<SkinHoleResultResponse> holeResults = new ArrayList<>();
        Map<Long, Integer> skinTotals = new HashMap<>();

        int carryover = 1;

        for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
            List<PlayerHoleNet> candidates = new ArrayList<>();

            for (Scorecard scorecard : scorecards) {
                HoleScore holeScore = holeScoreRepository
                        .findByScorecard_IdAndHoleNumber(scorecard.getId(), holeNumber)
                        .orElse(null);

                if (holeScore == null || holeScore.getNetStrokes() == null) {
                    continue;
                }

                candidates.add(new PlayerHoleNet(
                        scorecard.getPlayer().getId(),
                        scorecard.getPlayer().getDisplayName(),
                        holeScore.getNetStrokes()
                ));
            }

            SkinHoleResultResponse holeResult = new SkinHoleResultResponse();
            holeResult.setHoleNumber(holeNumber);
            holeResult.setSkinValue(carryover);

            if (candidates.isEmpty()) {
                holeResult.setWon(false);
                holeResult.setCarryover(true);
                holeResults.add(holeResult);
                carryover++;
                continue;
            }

            candidates.sort(Comparator.comparing(PlayerHoleNet::netStrokes));

            PlayerHoleNet best = candidates.get(0);
            boolean tie = candidates.size() > 1 &&
                    best.netStrokes().equals(candidates.get(1).netStrokes());

            if (tie) {
                holeResult.setWon(false);
                holeResult.setCarryover(true);
                holeResult.setWinningNetStrokes(best.netStrokes());
                carryover++;
            } else {
                holeResult.setWon(true);
                holeResult.setCarryover(false);
                holeResult.setWinningPlayerId(best.playerId());
                holeResult.setWinningPlayerName(best.playerName());
                holeResult.setWinningNetStrokes(best.netStrokes());

                skinTotals.merge(best.playerId(), carryover, Integer::sum);
                carryover = 1;
            }

            holeResults.add(holeResult);
        }

        List<PlayerSkinSummaryResponse> summaries = scorecards.stream()
                .map(sc -> {
                    PlayerSkinSummaryResponse dto = new PlayerSkinSummaryResponse();
                    dto.setPlayerId(sc.getPlayer().getId());
                    dto.setPlayerName(sc.getPlayer().getDisplayName());
                    dto.setSkinsWon(skinTotals.getOrDefault(sc.getPlayer().getId(), 0));
                    return dto;
                })
                .sorted(Comparator.comparing(PlayerSkinSummaryResponse::getSkinsWon).reversed()
                        .thenComparing(PlayerSkinSummaryResponse::getPlayerName))
                .toList();

        SkinsResultResponse response = new SkinsResultResponse();
        response.setHoles(holeResults);
        response.setPlayerSummaries(summaries);
        return response;
    }

    @Transactional
    public List<ContestResult> persistNetSkins(Long roundId) {
        SkinsResultResponse skins = getNetSkins(roundId);
        Round round = roundRepository.findById(roundId).orElseThrow();

        contestResultRepository.deleteByRound_IdAndContestType(roundId, NET_SKINS);

        List<PlayerSkinSummaryResponse> summaries = skins.getPlayerSummaries();
        List<ContestResult> saved = new ArrayList<>();

        int rank = 1;

        for (int i = 0; i < summaries.size(); i++) {
            PlayerSkinSummaryResponse current = summaries.get(i);

            if (i > 0) {
                PlayerSkinSummaryResponse previous = summaries.get(i - 1);
                if (!current.getSkinsWon().equals(previous.getSkinsWon())) {
                    rank = i + 1;
                }
            }

            Player playerRef = new Player();
            playerRef.setId(current.getPlayerId());

            ContestResult result = new ContestResult();
            result.setRound(round);
            result.setContestType(NET_SKINS);
            result.setPlayer(playerRef);
            result.setRank(rank);
            result.setResultScore(BigDecimal.valueOf(current.getSkinsWon()));
            result.setPayout(null);

            saved.add(contestResultRepository.save(result));
        }

        return saved;
    }
    private record PlayerHoleNet(Long playerId, String playerName, Integer netStrokes) {}
    }