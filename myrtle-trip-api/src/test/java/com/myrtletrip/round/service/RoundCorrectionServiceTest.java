package com.myrtletrip.round.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.BulkRoundScoreRequest;
import com.myrtletrip.round.dto.RoundCorrectionRequest;
import com.myrtletrip.round.dto.RoundCorrectionResponse;
import com.myrtletrip.round.dto.RoundTeeCorrectionRequest;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundCorrectionType;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.HoleScore;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.HoleScoreRepository;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.service.TripEditingGuardService;

@ExtendWith(MockitoExtension.class)
class RoundCorrectionServiceTest {

    @Mock private RoundRepository roundRepository;
    @Mock private ScorecardRepository scorecardRepository;
    @Mock private HoleScoreRepository holeScoreRepository;
    @Mock private BulkScoreEntryService bulkScoreEntryService;
    @Mock private ScorecardHandicapService scorecardHandicapService;
    @Mock private RoundRecalculationOrchestrationService roundRecalculationOrchestrationService;
    @Mock private RoundCorrectionLogService roundCorrectionLogService;
    @Mock private TripEditingGuardService tripEditingGuardService;

    private RoundCorrectionService service;
    private RoundCorrectionAuditService roundCorrectionAuditService;
    private RoundCorrectionExecutionService roundCorrectionExecutionService;

    @BeforeEach
    void setUp() {
        roundCorrectionAuditService = new RoundCorrectionAuditService(scorecardRepository, holeScoreRepository, roundCorrectionLogService);
        roundCorrectionExecutionService = new RoundCorrectionExecutionService(
                roundRepository,
                scorecardRepository,
                bulkScoreEntryService,
                scorecardHandicapService,
                roundRecalculationOrchestrationService,
                roundCorrectionAuditService,
                tripEditingGuardService);
        service = new RoundCorrectionService(roundCorrectionExecutionService);
    }

    @Test
    void applyCorrection_shouldRejectMissingRound() {
        when(roundRepository.findById(77L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.applyCorrection(77L, new RoundCorrectionRequest()));

        assertEquals("Round not found: 77", ex.getMessage());
        verify(tripEditingGuardService, never()).assertCorrectionAllowedForRound(any());
    }

    @Test
    void applyCorrection_shouldRejectEmptyCorrectionRequestAfterGuard() {
        Round round = round(5L, false);
        when(roundRepository.findById(5L)).thenReturn(Optional.of(round));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.applyCorrection(5L, new RoundCorrectionRequest()));

        assertEquals("At least one score, tee, participation, or handicap correction is required", ex.getMessage());
        verify(tripEditingGuardService).assertCorrectionAllowedForRound(round);
    }

    @Test
    void applyCorrection_shouldApplyWithdrawnParticipationAndRecalculate() {
        Round round = round(9L, false);
        Scorecard scorecard = scorecard(21L, round);
        scorecard.setParticipationStatus(ScorecardParticipationStatus.ACTIVE);

        when(roundRepository.findById(9L)).thenReturn(Optional.of(round));
        when(scorecardRepository.findById(21L)).thenReturn(Optional.of(scorecard));
        when(holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(21L)).thenReturn(new ArrayList<>());

        RoundCorrectionRequest.ParticipationCorrectionDto change = new RoundCorrectionRequest.ParticipationCorrectionDto();
        change.setScorecardId(21L);
        change.setParticipationStatus("withdrawn");
        change.setWithdrawalHoleNumber(7);

        RoundCorrectionRequest request = new RoundCorrectionRequest();
        request.setParticipationCorrections(List.of(change));

        RoundCorrectionResponse response = service.applyCorrection(9L, request);

        assertTrue(response.isSuccess());
        assertEquals(ScorecardParticipationStatus.WITHDRAWN, scorecard.getParticipationStatus());
        assertEquals(7, scorecard.getWithdrawalHoleNumber());
        verify(scorecardRepository).save(scorecard);
        verify(roundRecalculationOrchestrationService).handlePostRoundChange(9L);
        verify(roundCorrectionLogService).logCorrectionSafely(
                eq(round), eq(scorecard.getPlayer()), eq(RoundCorrectionType.PARTICIPATION_CHANGE), any(), any());
    }

    @Test
    void applyCorrection_shouldDelegateTeeCorrectionAndRecalculate() {
        Round round = round(12L, false);
        Scorecard scorecard = scorecard(31L, round);
        RoundTee tee = new RoundTee();
        tee.setTeeName("Blue");
        scorecard.setRoundTee(tee);
        scorecard.setCourseHandicap(10);
        scorecard.setPlayingHandicap(9);

        when(roundRepository.findById(12L)).thenReturn(Optional.of(round));
        when(scorecardRepository.findById(31L)).thenReturn(Optional.of(scorecard));
        when(holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(31L)).thenReturn(new ArrayList<>());

        RoundTeeCorrectionRequest teeChange = new RoundTeeCorrectionRequest();
        teeChange.setScorecardId(31L);
        teeChange.setRoundTeeId(88L);
        List<RoundTeeCorrectionRequest> changes = new ArrayList<>();
        changes.add(teeChange);

        RoundCorrectionRequest request = new RoundCorrectionRequest();
        request.setTeeCorrections(changes);

        service.applyCorrection(12L, request);

        verify(scorecardHandicapService).applyTeeCorrections(12L, changes);
        verify(roundRecalculationOrchestrationService).handlePostRoundChange(12L);
        verify(roundCorrectionLogService).logCorrectionSafely(
                eq(round), eq(scorecard.getPlayer()), eq(RoundCorrectionType.TEE_CHANGE), any(), any());
    }

    @Test
    void applyCorrection_shouldDelegatePlayerScoresToBulkCorrectionWithoutExtraRecalculation() {
        Round round = round(15L, false);
        Scorecard scorecard = scorecard(41L, round);
        scorecard.setGrossScore(82);
        scorecard.setNetScore(72);
        when(roundRepository.findById(15L)).thenReturn(Optional.of(round));
        when(scorecardRepository.findByRound_IdAndPlayer_Id(15L, 501L)).thenReturn(Optional.of(scorecard));
        when(scorecardRepository.findById(41L)).thenReturn(Optional.of(scorecard));

        HoleScore beforeHole = new HoleScore();
        beforeHole.setHoleNumber(1);
        beforeHole.setStrokes(5);
        when(holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(41L))
                .thenReturn(new ArrayList<>(List.of(beforeHole)));

        RoundCorrectionRequest.PlayerCorrectionDto playerChange = new RoundCorrectionRequest.PlayerCorrectionDto();
        playerChange.setPlayerId(501L);
        playerChange.setHoles(Arrays.asList(4, 5, 4));

        RoundCorrectionRequest request = new RoundCorrectionRequest();
        request.setPlayerCorrections(List.of(playerChange));

        service.applyCorrection(15L, request);

        ArgumentCaptor<BulkRoundScoreRequest> captor = ArgumentCaptor.forClass(BulkRoundScoreRequest.class);
        verify(bulkScoreEntryService).saveBulkScoreCorrections(eq(15L), captor.capture());
        BulkRoundScoreRequest bulkRequest = captor.getValue();
        assertEquals(1, bulkRequest.getScorecards().size());
        assertEquals(501L, bulkRequest.getScorecards().get(0).getPlayerId());
        assertEquals(Arrays.asList(4, 5, 4), bulkRequest.getScorecards().get(0).getHoles());
        verify(roundRecalculationOrchestrationService, never()).handlePostRoundChange(15L);
        verify(roundCorrectionLogService).logCorrectionSafely(
                eq(round), eq(scorecard.getPlayer()), eq(RoundCorrectionType.SCORE_CHANGE), any(), any());
    }

    @Test
    void applyCorrection_shouldRestoreFinalizedFlagAfterHandicapRefresh() {
        Round originalRound = round(18L, true);
        Round refreshedRound = round(18L, false);
        Scorecard scorecard = scorecard(61L, originalRound);
        scorecard.setCourseHandicap(8);
        scorecard.setPlayingHandicap(8);
        scorecard.setNetScore(70);

        when(roundRepository.findById(18L))
                .thenReturn(Optional.of(originalRound), Optional.of(refreshedRound));
        when(scorecardRepository.findByRound_IdOrderByIdAsc(18L))
                .thenReturn(new ArrayList<>(List.of(scorecard)));
        when(scorecardRepository.findById(61L)).thenReturn(Optional.of(scorecard));
        when(holeScoreRepository.findByScorecard_IdOrderByHoleNumberAsc(61L)).thenReturn(new ArrayList<>());

        RoundCorrectionRequest request = new RoundCorrectionRequest();
        request.setRefreshHandicaps(true);

        service.applyCorrection(18L, request);

        verify(scorecardHandicapService).refreshRoundHandicapsForCorrection(18L);
        verify(refreshedRound).setFinalized(true);
        verify(roundRepository).save(refreshedRound);
        verify(roundRecalculationOrchestrationService, times(2)).handlePostRoundChange(18L);
        verify(roundCorrectionLogService).logCorrectionSafely(
                eq(originalRound), eq(scorecard.getPlayer()), eq(RoundCorrectionType.HANDICAP_REFRESH), any(), any());
    }

    private Round round(Long id, boolean finalized) {
        Round round = org.mockito.Mockito.mock(Round.class);
        org.mockito.Mockito.lenient().when(round.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(round.getFinalized()).thenReturn(finalized);
        return round;
    }

    private Scorecard scorecard(Long id, Round round) {
        Scorecard scorecard = new Scorecard();
        scorecard.setId(id);
        scorecard.setRound(round);
        scorecard.setPlayer(org.mockito.Mockito.mock(Player.class));
        return scorecard;
    }
}
