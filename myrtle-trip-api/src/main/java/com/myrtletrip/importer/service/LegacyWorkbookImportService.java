package com.myrtletrip.importer.service;

import com.myrtletrip.course.entity.Course;
import com.myrtletrip.course.entity.CourseHole;
import com.myrtletrip.course.entity.CourseTee;
import com.myrtletrip.course.repository.CourseHoleRepository;
import com.myrtletrip.course.repository.CourseRepository;
import com.myrtletrip.course.repository.CourseTeeRepository;
import com.myrtletrip.player.entity.Player;
import com.myrtletrip.player.repository.PlayerRepository;
import com.myrtletrip.scorehistory.entity.ScoreHistoryEntry;
import com.myrtletrip.scorehistory.repository.ScoreHistoryEntryRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

@Service
public class LegacyWorkbookImportService {

    private static final Logger log = LoggerFactory.getLogger(LegacyWorkbookImportService.class);

    private final PlayerRepository playerRepository;
    private final CourseRepository courseRepository;
    private final CourseTeeRepository courseTeeRepository;
    private final CourseHoleRepository courseHoleRepository;
    private final ScoreHistoryEntryRepository scoreHistoryEntryRepository;

    public LegacyWorkbookImportService(PlayerRepository playerRepository,
                                       CourseRepository courseRepository,
                                       CourseTeeRepository courseTeeRepository,
                                       CourseHoleRepository courseHoleRepository,
                                       ScoreHistoryEntryRepository scoreHistoryEntryRepository) {
        this.playerRepository = playerRepository;
        this.courseRepository = courseRepository;
        this.courseTeeRepository = courseTeeRepository;
        this.courseHoleRepository = courseHoleRepository;
        this.scoreHistoryEntryRepository = scoreHistoryEntryRepository;
    }

    @Transactional
    public void importPlayersCoursesAndScores(String workbookPath) throws Exception {
        try (InputStream in = Files.newInputStream(Path.of(workbookPath));
             Workbook workbook = WorkbookFactory.create(in)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            importPlayers(workbook.getSheet("Players"), evaluator);
            importCourses(workbook.getSheet("Courses"), evaluator);
            importScores(workbook.getSheet("Scores"), evaluator);
        }
    }

    private void importPlayers(Sheet sheet, FormulaEvaluator evaluator) {
        if (sheet == null) {
            throw new IllegalArgumentException("Players sheet not found");
        }

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            Integer legacyNumber = getInteger(row.getCell(0), evaluator);
            String playerName = trim(getString(row.getCell(1), evaluator));

            if (legacyNumber == null || legacyNumber == 0) {
                continue;
            }

            if (isBlank(playerName) || "Tiger Woods".equalsIgnoreCase(playerName)) {
                continue;
            }

            String email = trim(getString(row.getCell(2), evaluator));
            String cell = trim(getString(row.getCell(3), evaluator));
            String ghin = trim(getString(row.getCell(4), evaluator));
            String venmo = trim(getString(row.getCell(6), evaluator));
            String zelle = trim(getString(row.getCell(7), evaluator));

            Player player = playerRepository.findByLegacyPlayerNumber(legacyNumber)
                    .or(() -> playerRepository.findByDisplayNameIgnoreCase(playerName))
                    .orElseGet(Player::new);

            player.setLegacyPlayerNumber(legacyNumber);
            player.setDisplayName(playerName);

            NameParts parts = splitName(playerName);
            player.setFirstName(parts.firstName());
            player.setLastName(parts.lastName());

            player.setEmail(email);
            player.setCell(cell);
            player.setGhinNumber(ghin);
            player.setVenmoId(venmo);
            player.setZelleId(zelle);
            player.setActive(true);

            playerRepository.save(player);
        }
    }

    private void importCourses(Sheet sheet, FormulaEvaluator evaluator) {
        if (sheet == null) {
            throw new IllegalArgumentException("Courses sheet not found");
        }

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            Integer courseNumber = getInteger(row.getCell(0), evaluator);
            String courseName = trim(getString(row.getCell(1), evaluator));

            if (courseNumber == null || courseNumber == 0) {
                continue;
            }

            if (isBlank(courseName) || "Dummy".equalsIgnoreCase(courseName)) {
                continue;
            }

            BigDecimal rating = getDecimal(row.getCell(2), evaluator);
            Integer slope = getInteger(row.getCell(3), evaluator);
            BigDecimal altRating = getDecimal(row.getCell(4), evaluator);
            Integer altSlope = getInteger(row.getCell(5), evaluator);
            Integer coursePar = getInteger(row.getCell(6), evaluator);
            String teeName = trim(getString(row.getCell(8), evaluator));

            if (isBlank(teeName)) {
                teeName = "Default";
            }

            if (rating == null || slope == null) {
                log.debug("Skipping course row {} because rating/slope missing for course {}", r + 1, courseName);
                continue;
            }

            Course course = courseRepository.findByLegacyCourseNumber(courseNumber)
                    .or(() -> courseRepository.findByNameIgnoreCase(courseName))
                    .orElseGet(Course::new);

            course.setLegacyCourseNumber(courseNumber);
            course.setName(courseName);
            course.setLocation("Myrtle Beach");
            course.setActive(true);

            course = courseRepository.save(course);

            CourseTee tee = courseTeeRepository.findByCourse_IdAndTeeNameIgnoreCase(course.getId(), teeName)
                    .orElseGet(CourseTee::new);

            tee.setCourse(course);
            tee.setTeeName(teeName);
            tee.setCourseRating(rating);
            tee.setSlope(slope);
            tee.setAlternateCourseRating(altRating);
            tee.setAlternateSlope(altSlope);
            tee.setParTotal(coursePar != null ? coursePar : 72);
            tee.setActive(true);

            tee = courseTeeRepository.save(tee);

            for (int hole = 1; hole <= 18; hole++) {
                Integer par = getInteger(row.getCell(9 + hole), evaluator);
                Integer handicap = getInteger(row.getCell(27 + hole), evaluator);

                if (par == null || handicap == null) {
                    continue;
                }

                CourseHole courseHole = courseHoleRepository
                        .findByCourseTee_IdAndHoleNumber(tee.getId(), hole)
                        .orElseGet(CourseHole::new);

                courseHole.setCourseTee(tee);
                courseHole.setHoleNumber(hole);
                courseHole.setPar(par);
                courseHole.setHandicap(handicap);
                courseHole.setYardage(null);

                courseHoleRepository.save(courseHole);
            }
        }
    }

    private void importScores(Sheet sheet, FormulaEvaluator evaluator) {
        if (sheet == null) {
            throw new IllegalArgumentException("Scores sheet not found");
        }

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }

            Integer legacyPlayerNumber = getInteger(row.getCell(0), evaluator);
            String playerName = trim(getString(row.getCell(1), evaluator));
            Integer legacyCourseNumber = getInteger(row.getCell(2), evaluator);
            String courseName = trim(getString(row.getCell(3), evaluator));
            LocalDate scoreDate = getLocalDate(row.getCell(4), evaluator);
            Integer grossScore = getInteger(row.getCell(5), evaluator);
            BigDecimal courseRating = getDecimal(row.getCell(6), evaluator);
            Integer slope = getInteger(row.getCell(7), evaluator);
            BigDecimal differential = getDecimal(row.getCell(8), evaluator);

            if (legacyPlayerNumber == null || legacyPlayerNumber == 0) {
                continue;
            }

            if (isBlank(playerName) || scoreDate == null || grossScore == null) {
                continue;
            }

            Player player = playerRepository.findByLegacyPlayerNumber(legacyPlayerNumber)
                    .or(() -> playerRepository.findByDisplayNameIgnoreCase(playerName))
                    .orElse(null);

            if (player == null) {
                log.debug("Skipping score row {} because player not found: {} / {}", r + 1, legacyPlayerNumber, playerName);
                continue;
            }

            Course course = null;

            if (legacyCourseNumber != null) {
                course = courseRepository.findByLegacyCourseNumber(legacyCourseNumber).orElse(null);
            }

            if (course == null && !isBlank(courseName)) {
                course = courseRepository.findByNameIgnoreCase(courseName).orElse(null);
            }

            if (isBlank(courseName) && course != null) {
                courseName = course.getName();
            }

            if (isBlank(courseName) && legacyCourseNumber != null) {
                courseName = "Course #" + legacyCourseNumber;
            }

            if (isBlank(courseName)) {
                log.debug("Skipping score row {} because course could not be resolved", r + 1);
                continue;
            }

            boolean exists;
            if (course != null) {
                exists = scoreHistoryEntryRepository.existsByPlayerAndScoreDateAndCourseAndGrossScore(
                        player,
                        scoreDate,
                        course,
                        grossScore
                );
            } else {
                exists = scoreHistoryEntryRepository.existsByPlayerAndScoreDateAndCourseNameAndGrossScore(
                        player,
                        scoreDate,
                        courseName,
                        grossScore
                );
            }

            if (exists) {
                continue;
            }

            if (differential == null) {
                differential = calculateDifferential(grossScore, courseRating, slope);
            }

            ScoreHistoryEntry entry = new ScoreHistoryEntry();
            entry.setPlayer(player);
            entry.setRound(null);
            entry.setCourse(course);
            entry.setScoreDate(scoreDate);
            entry.setCourseName(course != null ? course.getName() : courseName);
            entry.setCourseRating(courseRating != null ? courseRating : BigDecimal.ZERO);
            entry.setSlope(slope != null ? slope : 0);
            entry.setGrossScore(grossScore);
            entry.setAdjustedGrossScore(null);
            entry.setDifferential(differential);
            entry.setSourceType("LEGACY_IMPORT");
            entry.setIncludedInMyrtleCalc(true);

            scoreHistoryEntryRepository.save(entry);
        }
    }

    private BigDecimal calculateDifferential(Integer grossScore, BigDecimal courseRating, Integer slope) {
        if (grossScore == null || courseRating == null || slope == null || slope == 0) {
            return null;
        }

        BigDecimal score = BigDecimal.valueOf(grossScore);
        BigDecimal numerator = score.subtract(courseRating).multiply(BigDecimal.valueOf(113));
        return numerator.divide(BigDecimal.valueOf(slope), 1, RoundingMode.HALF_UP);
    }

    private String getString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }

        try {
            return switch (cell.getCellType()) {
                case STRING -> trimToNull(cell.getStringCellValue());
                case NUMERIC, BOOLEAN -> trimToNull(DataFormatterHolder.FORMATTER.formatCellValue(cell));
                case FORMULA -> getFormulaStringValue(cell, evaluator);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private String getFormulaStringValue(Cell cell, FormulaEvaluator evaluator) {
        try {
            String evaluated = DataFormatterHolder.FORMATTER.formatCellValue(cell, evaluator);
            if (!isBlank(evaluated)) {
                return trimToNull(evaluated);
            }
        } catch (Exception ignored) {
        }

        try {
            return switch (cell.getCachedFormulaResultType()) {
                case STRING -> trimToNull(cell.getStringCellValue());
                case NUMERIC, BOOLEAN -> trimToNull(DataFormatterHolder.FORMATTER.formatCellValue(cell));
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getInteger(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) Math.round(cell.getNumericCellValue());
            }

            if (cell.getCellType() == CellType.FORMULA) {
                try {
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue != null && cellValue.getCellType() == CellType.NUMERIC) {
                        return (int) Math.round(cellValue.getNumberValue());
                    }
                } catch (Exception ignored) {
                }

                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    return (int) Math.round(cell.getNumericCellValue());
                }
            }

            String value = normalizeNumericString(getString(cell, evaluator));
            if (isBlank(value)) {
                return null;
            }

            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getDecimal(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }

            if (cell.getCellType() == CellType.FORMULA) {
                try {
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue != null && cellValue.getCellType() == CellType.NUMERIC) {
                        return BigDecimal.valueOf(cellValue.getNumberValue());
                    }
                } catch (Exception ignored) {
                }

                if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                }
            }

            String value = normalizeNumericString(getString(cell, evaluator));
            if (isBlank(value)) {
                return null;
            }

            return new BigDecimal(value);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getLocalDate(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            if (cell.getCellType() == CellType.FORMULA &&
                    cell.getCachedFormulaResultType() == CellType.NUMERIC &&
                    DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }

            String value = trim(getString(cell, evaluator));
            if (isBlank(value)) {
                return null;
            }

            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeNumericString(String value) {
        if (value == null) {
            return null;
        }

        return value.trim()
                .replace(",", "")
                .replace("$", "")
                .replace("%", "");
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private NameParts splitName(String fullName) {
        String cleaned = fullName == null ? "" : fullName.trim();
        if (cleaned.isBlank()) {
            return new NameParts("", "");
        }

        String[] parts = cleaned.split("\\s+");
        if (parts.length == 1) {
            return new NameParts(parts[0], "");
        }

        String lastName = parts[parts.length - 1];
        String firstName = cleaned.substring(0, cleaned.length() - lastName.length()).trim();
        return new NameParts(firstName, lastName);
    }

    private record NameParts(String firstName, String lastName) {
    }

    private static final class DataFormatterHolder {
        private static final DataFormatter FORMATTER = new DataFormatter(Locale.US);
    }
}