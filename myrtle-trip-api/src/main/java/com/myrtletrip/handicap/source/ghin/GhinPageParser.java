package com.myrtletrip.handicap.source.ghin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GhinPageParser {

    private static final Pattern SCORE_PATTERN = Pattern.compile("^(\\d{1,3})(?:\\s*\\((\\d{1,2})\\))?$");

    public ParsedGhinProfile parse(String html) {
        Document doc = Jsoup.parse(html);

        ParsedGhinProfile profile = new ParsedGhinProfile();
        profile.setHandicapIndex(parseDisplayedHandicapIndex(doc));

        Element scoreTable = findScoreTable(doc);
        if (scoreTable == null) {
            return profile;
        }

        Elements rows = scoreTable.select("tbody tr");

        int rowNum = 1;
        for (Element row : rows) {
            Elements cells = row.select("th, td");
            if (cells.size() < 5) {
                continue;
            }

            String periodText = clean(cells.get(0).text());
            if (!periodText.matches("\\d{1,2}/\\d{2}")) {
                continue;
            }

            ParsedGhinScore score = new ParsedGhinScore();
            score.setDisplayOrder(rowNum);
            score.setRawPeriodText(periodText);

            parseScoreCell(cells.get(1).text(), score);
            score.setScoreType(clean(cells.get(2).text()).toUpperCase());
            parseCourseRatingSlope(cells.get(3).text(), score);

            BigDecimal parsedDifferential = parseDecimal(cells.get(4).text());

            if ("N".equalsIgnoreCase(score.getScoreType())) {
                score.setManualDifferentialRequired(true);
                score.setDifferential(null);
            } else {
                if (score.getCourseRating() != null
                        && score.getCourseRating().compareTo(BigDecimal.valueOf(50)) < 0
                        && profile.getHandicapIndex() != null
                        && parsedDifferential != null) {
                    parsedDifferential = adjustShortCourseDifferential(
                            parsedDifferential,
                            profile.getHandicapIndex()
                    );
                }

                score.setManualDifferentialRequired(false);
                score.setDifferential(parsedDifferential);
            }

            if (score.getGrossScore() == null) {
                continue;
            }

            profile.getScores().add(score);
            rowNum++;
        }

        return profile;
    }

    private Element findScoreTable(Document doc) {
        for (Element table : doc.select("table")) {
            String text = table.text();
            if (text.contains("MM/YY")
                    && text.contains("Score")
                    && text.contains("Type")
                    && text.contains("CR/Slope")
                    && text.contains("Diff")) {
                return table;
            }
        }
        return null;
    }

    private BigDecimal parseDisplayedHandicapIndex(Document doc) {
        Elements elements = doc.getElementsByClass("labelbar right");
        if (elements.isEmpty()) {
            return BigDecimal.ZERO;
        }

        String text = clean(elements.get(0).text());
        String[] parts = text.split("\\s{2,}");

        if (parts.length < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal parsed = parseDecimal(parts[1]);
        return parsed != null ? parsed : BigDecimal.ZERO;
    }

    private void parseScoreCell(String text, ParsedGhinScore score) {
        String cleaned = clean(text);
        Matcher matcher = SCORE_PATTERN.matcher(cleaned);

        if (!matcher.matches()) {
            score.setGrossScore(null);
            score.setHolesPlayed(null);
            return;
        }

        score.setGrossScore(Integer.parseInt(matcher.group(1)));

        if (matcher.group(2) != null) {
            score.setHolesPlayed(Integer.parseInt(matcher.group(2)));
        }
    }

    private void parseCourseRatingSlope(String text, ParsedGhinScore score) {
        String cleaned = clean(text);
        String[] parts = cleaned.split("/");

        if (parts.length >= 1) {
            score.setCourseRating(parseDecimal(parts[0]));
        }
        if (parts.length >= 2) {
            score.setSlope(Integer.parseInt(parts[1].trim()));
        }
    }

    private BigDecimal parseDecimal(String text) {
        String cleaned = clean(text)
                .replace(",", "")
                .replace("$", "");

        if (cleaned.isBlank()) {
            return null;
        }

        return new BigDecimal(cleaned);
    }

    private BigDecimal adjustShortCourseDifferential(BigDecimal rawDifferential, BigDecimal handicapIndex) {
        return rawDifferential
                .add(handicapIndex.multiply(BigDecimal.valueOf(0.52)))
                .add(BigDecimal.valueOf(1.2))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private String clean(String text) {
        return text == null ? "" : text.trim();
    }
}