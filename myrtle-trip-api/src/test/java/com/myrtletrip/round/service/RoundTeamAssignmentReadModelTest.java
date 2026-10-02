package com.myrtletrip.round.service;

import com.myrtletrip.handicap.service.TripHandicapService;
import com.myrtletrip.permissions.dto.RoundCapabilityResponse;
import com.myrtletrip.permissions.service.RoundCapabilityService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.round.dto.RoundScrambleSeedingRoundResponse;
import com.myrtletrip.round.dto.RoundTeamAssignmentPageResponse;
import com.myrtletrip.round.dto.RoundTeamPlayerResponse;
import com.myrtletrip.round.dto.RoundTeeOptionResponse;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.repository.RoundTeamPlayerRepository;
import com.myrtletrip.round.repository.RoundTeamRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.model.ScorecardParticipationStatus;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoundTeamAssignmentReadModelTest {

    @Test
    void getAssignmentPage_rejectsMissingRound() {
        Fixture fixture = new Fixture();
        when(fixture.roundRepository.findById(77L)).thenReturn(Optional.empty());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.getAssignmentPage(77L));

        assertEquals("Round not found: 77", error.getMessage());
    }

    @Test
    void getAssignmentPage_mapsRoundMetadataAndDelegatesSupportingReadModels() {
        Fixture fixture = new Fixture();
        Round round = mock(Round.class);
        RoundTee defaultTee = mock(RoundTee.class);
        RoundCapabilityResponse capabilities = mock(RoundCapabilityResponse.class);
        LocalDate seedingDate = LocalDate.of(2026, 3, 2);
        List<RoundScrambleSeedingRoundResponse> seedingRounds =
                List.of(mock(RoundScrambleSeedingRoundResponse.class));

        when(round.getId()).thenReturn(40L);
        when(round.getDefaultRoundTee()).thenReturn(defaultTee);
        when(defaultTee.getId()).thenReturn(9L);

        when(fixture.roundRepository.findById(40L)).thenReturn(Optional.of(round));
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(40L)).thenReturn(List.of());
        when(fixture.scorecardRepository.findByRound_Id(40L)).thenReturn(List.of());
        when(fixture.roundTeeRepository.findByRound_IdOrderByTeeNameAsc(40L)).thenReturn(List.of());

        when(fixture.scrambleSeedingService.resolveScrambleTeamSize(round)).thenReturn(4);
        when(fixture.scrambleSeedingService.resolveScrambleSeedingMethod(round))
                .thenReturn("CURRENT_HANDICAP_INDEX");
        when(fixture.scrambleSeedingService.resolveScrambleHandicapDate(round))
                .thenReturn(LocalDate.of(2026, 3, 1));
        when(fixture.scrambleSeedingService.determineSeedingAsOfDate(round)).thenReturn(seedingDate);
        when(fixture.scrambleSeedingService.buildSeedingLabel(round, seedingDate))
                .thenReturn("Handicap Index as of Mar 2");
        when(fixture.scrambleSeedingService.mapScrambleSeedingRounds(round)).thenReturn(seedingRounds);
        when(fixture.roundCapabilityService.build(round)).thenReturn(capabilities);

        RoundTeamAssignmentPageResponse response = fixture.service.getAssignmentPage(40L);

        assertEquals(40L, response.getRoundId());
        assertEquals(9L, response.getDefaultRoundTeeId());
        assertEquals(4, response.getScrambleTeamSize());
        assertEquals("CURRENT_HANDICAP_INDEX", response.getScrambleSeedingMethod());
        assertEquals(LocalDate.of(2026, 3, 1), response.getScrambleHandicapDate());
        assertEquals(seedingDate, response.getSeedingAsOfDate());
        assertEquals("Handicap Index as of Mar 2", response.getSeedingLabel());
        assertSame(seedingRounds, response.getScrambleSeedingRounds());
        assertSame(capabilities, response.getCapabilities());

        verify(fixture.roundTeeProvisioningService).ensureRoundTeeOptions(round);
    }

    @Test
    void getAssignmentPage_partitionsAndSortsUnassignedAndInactivePlayers() {
        Fixture fixture = new Fixture();
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(50L);

        Scorecard activeZulu = scorecard(
                1L, "Zach", "Zulu", ScorecardParticipationStatus.ACTIVE, fixture, round, 11L, "Blue");
        Scorecard activeAlpha = scorecard(
                2L, "Amy", "Alpha", ScorecardParticipationStatus.ACTIVE, fixture, round, 12L, "White");
        Scorecard inactive = scorecard(
                3L, "Ian", "Inactive", ScorecardParticipationStatus.NO_SHOW, fixture, round, 13L, "Gold");

        when(fixture.roundRepository.findById(50L)).thenReturn(Optional.of(round));
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(50L)).thenReturn(List.of());
        when(fixture.scorecardRepository.findByRound_Id(50L))
                .thenReturn(List.of(activeZulu, inactive, activeAlpha));
        when(fixture.roundTeeRepository.findByRound_IdOrderByTeeNameAsc(50L)).thenReturn(List.of());

        RoundTeamAssignmentPageResponse response = fixture.service.getAssignmentPage(50L);

        assertEquals(2, response.getUnassignedPlayers().size());
        assertEquals("Amy Alpha", response.getUnassignedPlayers().get(0).getPlayerName());
        assertEquals("Zach Zulu", response.getUnassignedPlayers().get(1).getPlayerName());

        assertEquals(1, response.getInactivePlayers().size());
        RoundTeamPlayerResponse inactiveResponse = response.getInactivePlayers().get(0);
        assertEquals("Ian Inactive", inactiveResponse.getPlayerName());
        assertEquals("NO_SHOW", inactiveResponse.getParticipationStatus());
    }

    @Test
    void getAssignmentPage_sortsTeeOptionsByDescendingMensRatingAndBuildsDisplayName() {
        Fixture fixture = new Fixture();
        Round round = mock(Round.class);
        when(round.getId()).thenReturn(60L);

        RoundTee white = regularTee(1L, "White", "70.2", 125, 72);
        RoundTee blue = regularTee(2L, "Blue", "72.8", 132, 72);
        RoundTee unrated = regularTee(3L, "Gold", null, null, null);

        when(fixture.roundRepository.findById(60L)).thenReturn(Optional.of(round));
        when(fixture.roundTeamRepository.findByRound_IdOrderByTeamNumberAsc(60L)).thenReturn(List.of());
        when(fixture.scorecardRepository.findByRound_Id(60L)).thenReturn(List.of());
        when(fixture.roundTeeRepository.findByRound_IdOrderByTeeNameAsc(60L))
                .thenReturn(List.of(white, unrated, blue));

        RoundTeamAssignmentPageResponse response = fixture.service.getAssignmentPage(60L);

        List<RoundTeeOptionResponse> tees = response.getTeeOptions();
        assertEquals(3, tees.size());
        assertEquals("Blue", tees.get(0).getTeeName());
        assertEquals("White", tees.get(1).getTeeName());
        assertEquals("Gold", tees.get(2).getTeeName());

        assertEquals("Blue (CR 72.8 / Slope 132 / Par 72)", tees.get(0).getDisplayName());
        assertEquals(Boolean.TRUE, tees.get(0).getEligibleForMen());
        assertEquals(Boolean.FALSE, tees.get(0).getEligibleForWomen());
    }

    private static Scorecard scorecard(
            Long id,
            String firstName,
            String lastName,
            ScorecardParticipationStatus status,
            Fixture fixture,
            Round round,
            Long teeId,
            String teeName) {
        Scorecard scorecard = mock(Scorecard.class);
        Player player = mock(Player.class);
        RoundTee resolvedTee = mock(RoundTee.class);

        when(scorecard.getId()).thenReturn(id);
        when(scorecard.getPlayer()).thenReturn(player);
        when(scorecard.getParticipationStatus()).thenReturn(status);
        when(scorecard.getTeam()).thenReturn(null);

        when(player.getId()).thenReturn(id);
        when(player.getFirstName()).thenReturn(firstName);
        when(player.getLastName()).thenReturn(lastName);
        when(player.getGender()).thenReturn("M");

        when(fixture.roundTeeResolver.resolve(scorecard)).thenReturn(resolvedTee);
        when(resolvedTee.getId()).thenReturn(teeId);
        when(resolvedTee.getTeeName()).thenReturn(teeName);

        when(round.getDefaultRoundTee()).thenReturn(null);
        return scorecard;
    }

    private static RoundTee regularTee(
            Long id,
            String name,
            String rating,
            Integer slope,
            Integer par) {
        RoundTee tee = mock(RoundTee.class);
        when(tee.getId()).thenReturn(id);
        when(tee.getTeeName()).thenReturn(name);
        when(tee.getSourceCourseTee()).thenReturn(null);
        if (rating != null) {
            when(tee.getCourseRating()).thenReturn(new BigDecimal(rating));
        }
        if (slope != null) {
            when(tee.getSlope()).thenReturn(slope);
        }
        if (par != null) {
            when(tee.getParTotal()).thenReturn(par);
        }
        return tee;
    }

    private static final class Fixture {
        private final RoundRepository roundRepository = mock(RoundRepository.class);
        private final RoundTeamRepository roundTeamRepository = mock(RoundTeamRepository.class);
        private final RoundTeamPlayerRepository roundTeamPlayerRepository = mock(RoundTeamPlayerRepository.class);
        private final RoundTeeRepository roundTeeRepository = mock(RoundTeeRepository.class);
        private final ScorecardRepository scorecardRepository = mock(ScorecardRepository.class);
        private final RoundTeeResolver roundTeeResolver = mock(RoundTeeResolver.class);
        private final RoundTeeProvisioningService roundTeeProvisioningService =
                mock(RoundTeeProvisioningService.class);
        private final TripHandicapService tripHandicapService = mock(TripHandicapService.class);
        private final TripPlayerRepository tripPlayerRepository = mock(TripPlayerRepository.class);
        private final RoundCapabilityService roundCapabilityService = mock(RoundCapabilityService.class);
        private final RoundEventCapabilityService roundEventCapabilityService =
                mock(RoundEventCapabilityService.class);
        private final RoundScrambleSeedingService scrambleSeedingService =
                mock(RoundScrambleSeedingService.class);

        private final RoundTeamAssignmentReadModelService service = new RoundTeamAssignmentReadModelService(
                roundRepository,
                roundTeamRepository,
                roundTeamPlayerRepository,
                roundTeeRepository,
                scorecardRepository,
                roundTeeResolver,
                roundTeeProvisioningService,
                tripHandicapService,
                tripPlayerRepository,
                roundCapabilityService,
                roundEventCapabilityService,
                scrambleSeedingService
        );
    }
}
