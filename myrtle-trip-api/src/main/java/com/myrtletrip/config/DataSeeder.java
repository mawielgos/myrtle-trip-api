package com.myrtletrip.config;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.handicap.service.RoundHandicapService;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.round.entity.Round;
import com.myrtletrip.round.model.RoundFormat;
import com.myrtletrip.round.repository.RoundRepository;
import com.myrtletrip.scoreentry.entity.Scorecard;
import com.myrtletrip.scoreentry.repository.ScorecardRepository;
import com.myrtletrip.trip.entity.Trip;
import com.myrtletrip.trip.repository.TripRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(
            TripRepository tripRepository,
            PlayerRepository playerRepository,
            CourseRepository courseRepository,
            CourseTeeRepository courseTeeRepository,
            RoundRepository roundRepository,
            ScorecardRepository scorecardRepository,
            RoundHandicapService roundHandicapService
    ) {
        return args -> {

            if (roundRepository.count() > 0) {
                System.out.println("Skipping seed: rounds already exist.");
                return;
            }

            List<Player> players = playerRepository.findAllById(List.of(1L, 3L, 6L, 7L));
            if (players.size() != 4) {
                throw new IllegalStateException("One or more player IDs not found.");
            }

            Course course = courseRepository.findById(32L)
                    .orElseThrow(() -> new IllegalStateException("Course 32 not found"));

            CourseTee courseTee = courseTeeRepository.findAll()
                    .stream()
                    .filter(ct -> ct.getCourse() != null && ct.getCourse().getId().equals(32L))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No course tee found for course 32"));

            Trip trip = tripRepository.findAll()
                    .stream()
                    .filter(t -> "MYRTLE2026DEMO".equals(t.getTripCode()))
                    .findFirst()
                    .orElseGet(() -> {
                        Trip newTrip = new Trip();
                        newTrip.setName("2026 Myrtle Demo Trip");
                        newTrip.setTripYear(2026);
                        newTrip.setTripCode("MYRTLE2026DEMO");
                        newTrip.setInitialized(true);
                        return tripRepository.save(newTrip);
                    });

            trip.setName("2026 Myrtle Demo Trip");
            trip.setTripYear(2026);
            trip.setTripCode("MYRTLE2026DEMO");
            trip.setInitialized(true);
            trip = tripRepository.save(trip);

            Round round = new Round();
            round.setTrip(trip);
            round.setCourse(course);
            round.setCourseTee(courseTee);
            round.setRoundDate(LocalDate.now().plusDays(1));
            round.setFinalized(false);
            round.setFormat(RoundFormat.MIDDLE_MAN);

            round = roundRepository.save(round);

            String handicapGroupCode = "MYRTLE_2026";

            for (Player player : players) {
                Scorecard sc = new Scorecard();
                sc.setRound(round);
                sc.setPlayer(player);
                sc.setUseAlternateTee(false);

                // Example test case: player 6 moves up to alternate tee
                if (player.getId().equals(6L)) {
                    sc.setUseAlternateTee(true);
                }

                roundHandicapService.populateCurrentHandicaps(sc, handicapGroupCode);

                scorecardRepository.save(sc);

                System.out.println("Seeded scorecard for player "
                        + player.getId()
                        + " useAlternateTee=" + sc.getUseAlternateTee()
                        + " courseHandicap=" + sc.getCourseHandicap()
                        + " playingHandicap=" + sc.getPlayingHandicap());
            }

            System.out.println("Seed complete. Round ID: " + round.getId());
        };
    }
}