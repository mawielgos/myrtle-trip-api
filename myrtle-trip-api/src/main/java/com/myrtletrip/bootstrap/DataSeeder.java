package com.myrtletrip.bootstrap;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.entity.RoundTee;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.model.RoundTeeRole;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.round.repository.RoundTeeRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.entity.TripPlayer;
import com.myrtletrip.trip.repository.TripPlayerRepository;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Configuration
public class DataSeeder {

    private static final String DEMO_TRIP_CODE = "MYRTLE2026DEMO";

    private static final List<Long> DEMO_PLAYER_IDS = List.of(
            1L, 3L, 6L, 7L, 8L, 11L, 17L, 15L,
            16L, 40L, 52L, 42L, 50L, 38L, 53L, 49L
    );

    @Bean
    CommandLineRunner seedData(
            TripRepository tripRepository,
            TripPlayerRepository tripPlayerRepository,
            PlayerRepository playerRepository,
            CourseRepository courseRepository,
            CourseTeeRepository courseTeeRepository,
            RoundRepository roundRepository,
            RoundTeeRepository roundTeeRepository,
            ScorecardRepository scorecardRepository,
            RoundHandicapService roundHandicapService
    ) {
        return args -> {

            Trip trip = tripRepository.findAll()
                    .stream()
                    .filter(t -> DEMO_TRIP_CODE.equals(t.getTripCode()))
                    .findFirst()
                    .orElseGet(() -> {
                        Trip newTrip = new Trip();
                        newTrip.setName("2026 Myrtle Demo Trip");
                        newTrip.setTripYear(2026);
                        newTrip.setTripCode(DEMO_TRIP_CODE);
                        newTrip.setInitialized(true);
                        return tripRepository.save(newTrip);
                    });

            trip.setName("2026 Myrtle Demo Trip");
            trip.setTripYear(2026);
            trip.setTripCode(DEMO_TRIP_CODE);
            trip.setInitialized(true);
            trip = tripRepository.save(trip);

            List<Player> players = loadSeedPlayers(playerRepository);
            ensureTripRoster(trip, players, tripPlayerRepository);
 
            final Long tripId = trip.getId();

            Optional<Round> existingRound = roundRepository.findAll()
                    .stream()
                    .filter(r -> r.getTrip() != null
                            && r.getTrip().getId() != null
                            && r.getTrip().getId().equals(tripId))
                    .findFirst();

            if (existingRound.isPresent()) {
                System.out.println("Skipping round seed: demo trip already has a round. Trip ID: " + trip.getId());
                return;
            }

            Course course = courseRepository.findById(32L)
                    .orElseThrow(() -> new IllegalStateException("Course 32 not found"));

            CourseTee courseTee = courseTeeRepository.findAll()
                    .stream()
                    .filter(ct -> ct.getCourse() != null && ct.getCourse().getId().equals(32L))
                    .sorted(Comparator.comparing(CourseTee::getId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No course tee found for course 32"));

            Round round = new Round();
            round.setTrip(trip);
            round.setCourse(course);
            round.setRoundDate(LocalDate.now().plusDays(1));
            round.setFinalized(false);
            round.setFormat(RoundFormat.MIDDLE_MAN);
            round.setHandicapPercent(100);
            round = roundRepository.save(round);

            RoundTee standardRoundTee = new RoundTee();
            standardRoundTee.setRound(round);
            standardRoundTee.setSourceCourseTee(courseTee);
            standardRoundTee.setTeeRole(RoundTeeRole.STANDARD);
            standardRoundTee.setCourseName(course.getName());
            standardRoundTee.setTeeName(courseTee.getTeeName());
            standardRoundTee.setCourseRating(courseTee.getCourseRating());
            standardRoundTee.setSlope(courseTee.getSlope());
            standardRoundTee.setParTotal(courseTee.getParTotal());
            standardRoundTee = roundTeeRepository.save(standardRoundTee);

            round.setStandardRoundTee(standardRoundTee);
            round = roundRepository.save(round);

            for (Player player : players) {
                Scorecard existingScorecard = scorecardRepository.findByRound_Id(round.getId())
                        .stream()
                        .filter(sc -> sc.getPlayer() != null
                                && sc.getPlayer().getId() != null
                                && sc.getPlayer().getId().equals(player.getId()))
                        .findFirst()
                        .orElse(null);

                if (existingScorecard != null) {
                    continue;
                }

                Scorecard scorecard = new Scorecard();
                scorecard.setRound(round);
                scorecard.setPlayer(player);
                scorecard.setRoundTee(standardRoundTee);

                try {
                    roundHandicapService.populateCurrentHandicaps(scorecard, DEMO_TRIP_CODE);
                } catch (Exception e) {
                    scorecard.setCourseHandicap(null);
                    scorecard.setPlayingHandicap(null);
                    System.out.println("Handicap seed skipped for player " + player.getDisplayName() + ": " + e.getMessage());
                }

                scorecardRepository.save(scorecard);

                System.out.println("Seeded scorecard for player "
                        + player.getDisplayName()
                        + " courseHandicap=" + scorecard.getCourseHandicap()
                        + " playingHandicap=" + scorecard.getPlayingHandicap());
            }

            System.out.println("Seed complete. Trip ID: " + trip.getId() + ", Round ID: " + round.getId());
        };
    }

    private List<Player> loadSeedPlayers(PlayerRepository playerRepository) {
        List<Player> players = new ArrayList<>();

        for (Long playerId : DEMO_PLAYER_IDS) {
            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalStateException("Seed player not found: " + playerId));
            players.add(player);
        }

        return players;
    }

    private void ensureTripRoster(Trip trip,
                                  List<Player> players,
                                  TripPlayerRepository tripPlayerRepository) {

        List<TripPlayer> existingRoster = tripPlayerRepository.findByTripOrderByDisplayOrderAsc(trip);

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);

            boolean alreadyExists = existingRoster.stream()
                    .anyMatch(tp -> tp.getPlayer() != null
                            && tp.getPlayer().getId() != null
                            && tp.getPlayer().getId().equals(player.getId()));

            if (alreadyExists) {
                continue;
            }

            TripPlayer tripPlayer = new TripPlayer();
            tripPlayer.setTrip(trip);
            tripPlayer.setPlayer(player);
            tripPlayer.setDisplayOrder(i + 1);
            tripPlayerRepository.save(tripPlayer);
        }
    }
}
