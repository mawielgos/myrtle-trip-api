package com.myrtletrip.scoreentry.service;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.entity.RoundTeeHole;
import com.myrtletrip.round.repository.RoundTeeHoleRepository;
import com.myrtletrip.round.service.RoundEventCapabilityService;
import com.myrtletrip.round.service.RoundTeeResolver;
import com.myrtletrip.scoreentry.dto.RoundScorecardResponse;
import com.myrtletrip.scoreentry.dto.ScorecardResponse;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.scorehistory.service.RoundScoreHistorySyncService;
import com.myrtletrip.trip.service.TripEditingGuardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoringServiceTest {

    @Mock private HoleScoreRepository holeRepo;
    @Mock private ScorecardRepository scorecardRepo;
    @Mock private RoundTeeHoleRepository roundTeeHoleRepository;
    @Mock private RoundScoreHistorySyncService roundScoreHistorySyncService;
    @Mock private RoundTeeResolver roundTeeResolver;
    @Mock private TripEditingGuardService tripEditingGuardService;
    @Mock private RoundEventCapabilityService roundEventCapabilityService;

    private ScoringService service;
    private ScoringCommandService scoringCommandService;
    private ScoringReadModelService scoringReadModelService;

    @BeforeEach
    void setUp() {
        scoringCommandService = new ScoringCommandService(
                holeRepo,
                scorecardRepo,
                roundTeeHoleRepository,
                roundScoreHistorySyncService,
                roundTeeResolver,
                tripEditingGuardService,
                roundEventCapabilityService);
        scoringReadModelService = new ScoringReadModelService(holeRepo, scorecardRepo);
        service = new ScoringService(scoringCommandService, scoringReadModelService);
    }

    @Test
    void updateHoleScoreUpdatesExistingHoleAndRecalculatesScorecard() {
        Round round = mock(Round.class);
        when(round.getFinalized()).thenReturn(false);

        RoundTee roundTee = mock(RoundTee.class);
        when(roundTee.getId()).thenReturn(77L);

        Scorecard scorecard = scorecard(10L, round, player(3L, "Alice"));
        scorecard.setPlayingHandicap(0);
        scorecard.setCourseHandicap(0);
        scorecard.setRoundTee(roundTee);

        HoleScore hole = hole(scorecard, 1, 4);

        when(scorecardRepo.findById(10L)).thenReturn(Optional.of(scorecard));
        when(holeRepo.findByScorecard_IdAndHoleNumber(10L, 1)).thenReturn(Optional.of(hole));
        when(roundTeeResolver.resolve(scorecard)).thenReturn(roundTee);
        when(roundTeeHoleRepository.findByRoundTee_IdOrderByHoleNumberAsc(77L)).thenReturn(roundTeeHoles());
        when(holeRepo.findByScorecard_IdOrderByHoleNumberAsc(10L)).thenReturn(new ArrayList<>(List.of(hole)));
        when(roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);

        service.updateHoleScore(10L, 1, 5);

        assertEquals(5, hole.getStrokes());
        assertEquals(5, scorecard.getGrossScore());
        assertEquals(5, scorecard.getNetScore());
        assertEquals(5, scorecard.getAdjustedGrossScore());
        assertEquals(1, scorecard.getThruHole());
        verify(tripEditingGuardService).assertCorrectionAllowedForRound(round);
        verify(holeRepo, atLeastOnce()).save(hole);
        verify(scorecardRepo).save(scorecard);
    }

    @Test
    void recalculateNoShowClearsScoresAndSyncsFinalizedHistory() {
        Round round = mock(Round.class);
        when(round.getFinalized()).thenReturn(true);

        Scorecard scorecard = scorecard(20L, round, player(4L, "Bob"));
        scorecard.setParticipationStatus(ScorecardParticipationStatus.NO_SHOW);
        scorecard.setGrossScore(88);
        scorecard.setNetScore(74);
        scorecard.setAdjustedGrossScore(82);
        scorecard.setThruHole(18);

        HoleScore hole = hole(scorecard, 1, 5);
        hole.setNetStrokes(4);
        hole.setAdjustedStrokes(5);

        when(scorecardRepo.findById(20L)).thenReturn(Optional.of(scorecard));
        when(holeRepo.findByScorecard_IdOrderByHoleNumberAsc(20L)).thenReturn(new ArrayList<>(List.of(hole)));

        service.recalculate(20L);

        assertNull(hole.getStrokes());
        assertNull(hole.getNetStrokes());
        assertNull(hole.getAdjustedStrokes());
        assertNull(scorecard.getGrossScore());
        assertNull(scorecard.getNetScore());
        assertNull(scorecard.getAdjustedGrossScore());
        assertNull(scorecard.getThruHole());
        verify(tripEditingGuardService).assertCorrectionAllowedForRound(round);
        verify(scorecardRepo).save(scorecard);
        verify(roundScoreHistorySyncService).syncFinalizedScorecard(scorecard);
        verifyNoInteractions(roundTeeResolver);
    }

    @Test
    void recalculateAppliesPlayingAndCourseHandicapAcrossEighteenHoles() {
        Round round = mock(Round.class);
        when(round.getFinalized()).thenReturn(false);

        RoundTee roundTee = mock(RoundTee.class);
        when(roundTee.getId()).thenReturn(88L);

        Scorecard scorecard = scorecard(30L, round, player(5L, "Carol"));
        scorecard.setPlayingHandicap(18);
        scorecard.setCourseHandicap(18);
        scorecard.setRoundTee(roundTee);

        List<HoleScore> holes = new ArrayList<>();
        for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
            holes.add(hole(scorecard, holeNumber, 8));
        }

        when(scorecardRepo.findById(30L)).thenReturn(Optional.of(scorecard));
        when(roundTeeResolver.resolve(scorecard)).thenReturn(roundTee);
        when(roundTeeHoleRepository.findByRoundTee_IdOrderByHoleNumberAsc(88L)).thenReturn(roundTeeHoles());
        when(holeRepo.findByScorecard_IdOrderByHoleNumberAsc(30L)).thenReturn(holes);
        when(roundEventCapabilityService.isScrambleRound(round)).thenReturn(false);

        service.recalculate(30L);

        assertEquals(144, scorecard.getGrossScore());
        assertEquals(126, scorecard.getNetScore());
        assertEquals(126, scorecard.getAdjustedGrossScore());
        assertEquals(18, scorecard.getThruHole());
        assertTrue(holes.stream().allMatch(h -> Integer.valueOf(7).equals(h.getNetStrokes())));
        assertTrue(holes.stream().allMatch(h -> Integer.valueOf(7).equals(h.getAdjustedStrokes())));
        verify(scorecardRepo).save(scorecard);
    }

    @Test
    void getScorecardMapsScorecardAndHoleDetails() {
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(41L);

        Scorecard scorecard = scorecard(40L, round, player(6L, "Dan"));
        scorecard.setPlayingHandicap(9);
        scorecard.setGrossScore(82);
        scorecard.setNetScore(73);
        scorecard.setAdjustedGrossScore(80);

        HoleScore first = hole(scorecard, 1, 5);
        first.setNetStrokes(4);
        first.setAdjustedStrokes(5);
        HoleScore second = hole(scorecard, 2, 4);
        second.setNetStrokes(4);
        second.setAdjustedStrokes(4);

        when(scorecardRepo.findById(40L)).thenReturn(Optional.of(scorecard));
        when(holeRepo.findByScorecard_IdOrderByHoleNumberAsc(40L)).thenReturn(List.of(first, second));

        ScorecardResponse response = service.getScorecard(40L);

        assertEquals(40L, response.getScorecardId());
        assertEquals(41L, response.getRoundId());
        assertEquals(6L, response.getPlayerId());
        assertEquals(9, response.getPlayingHandicap());
        assertEquals(82, response.getGrossScore());
        assertEquals(73, response.getNetScore());
        assertEquals(80, response.getAdjustedGrossScore());
        assertEquals(2, response.getHoles().size());
        assertEquals(1, response.getHoles().get(0).getHoleNumber());
        assertEquals(5, response.getHoles().get(0).getStrokes());
        assertEquals(4, response.getHoles().get(0).getNetStrokes());
    }

    @Test
    void getRoundScorecardsSortsByNetScoreWithIncompleteCardsLast() {
        Round round = mock(Round.class);

        Scorecard high = scorecard(51L, round, player(11L, "High Net"));
        high.setPlayingHandicap(10);
        high.setGrossScore(90);
        high.setNetScore(80);
        high.setAdjustedGrossScore(88);

        Scorecard low = scorecard(52L, round, player(12L, "Low Net"));
        low.setPlayingHandicap(12);
        low.setGrossScore(84);
        low.setNetScore(72);
        low.setAdjustedGrossScore(82);

        Scorecard incomplete = scorecard(53L, round, player(13L, "Incomplete"));
        incomplete.setPlayingHandicap(8);

        when(scorecardRepo.findByRound_Id(99L)).thenReturn(new ArrayList<>(List.of(high, incomplete, low)));

        List<RoundScorecardResponse> result = service.getRoundScorecards(99L);

        assertEquals(List.of(52L, 51L, 53L), result.stream().map(RoundScorecardResponse::getScorecardId).toList());
        assertEquals("Low Net", result.get(0).getPlayerName());
        assertEquals(72, result.get(0).getNetScore());
        assertNull(result.get(2).getNetScore());
    }

    private static Scorecard scorecard(Long id, Round round, Player player) {
        Scorecard scorecard = new Scorecard();
        scorecard.setId(id);
        scorecard.setRound(round);
        scorecard.setPlayer(player);
        return scorecard;
    }

    private static Player player(Long id, String displayName) {
        Player player = new Player();
        player.setId(id);
        player.setDisplayName(displayName);
        return player;
    }

    private static HoleScore hole(Scorecard scorecard, int holeNumber, int strokes) {
        HoleScore hole = new HoleScore();
        hole.setScorecard(scorecard);
        hole.setHoleNumber(holeNumber);
        hole.setStrokes(strokes);
        return hole;
    }

    private static List<RoundTeeHole> roundTeeHoles() {
        List<RoundTeeHole> holes = new ArrayList<>();
        for (int holeNumber = 1; holeNumber <= 18; holeNumber++) {
            RoundTeeHole hole = new RoundTeeHole();
            hole.setHoleNumber(holeNumber);
            hole.setPar(4);
            hole.setHandicap(holeNumber);
            holes.add(hole);
        }
        return holes;
    }
}
