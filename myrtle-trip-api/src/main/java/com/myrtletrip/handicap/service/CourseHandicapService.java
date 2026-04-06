package com.myrtletrip.handicap.service;

import com.myrtletrip.course.entity.CourseTee;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(readOnly = true)
public class CourseHandicapService {

    private static final BigDecimal STANDARD_SLOPE = BigDecimal.valueOf(113);

    /**
     * Standard course handicap formula:
     *
     * course handicap = round(index * slope / 113 + (course rating - par))
     */
    public Integer calculateCourseHandicap(BigDecimal tripIndex, CourseTee courseTee) {
        if (tripIndex == null) {
            return null;
        }
        if (courseTee == null) {
            throw new IllegalArgumentException("courseTee is required");
        }

        BigDecimal courseRating = courseTee.getCourseRating();
        Integer slope = courseTee.getSlope();
        Integer parTotal = courseTee.getParTotal();

        if (courseRating == null) {
            throw new IllegalArgumentException("courseTee.courseRating is required");
        }
        if (slope == null || slope <= 0) {
            throw new IllegalArgumentException("courseTee.slope must be > 0");
        }
        if (parTotal == null) {
            throw new IllegalArgumentException("courseTee.parTotal is required");
        }

        BigDecimal result = tripIndex
                .multiply(BigDecimal.valueOf(slope))
                .divide(STANDARD_SLOPE, 10, RoundingMode.HALF_UP)
                .add(courseRating.subtract(BigDecimal.valueOf(parTotal)));

        return result.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Alternate-tee version.
     * Falls back to the normal rating/slope if alternate values are missing.
     */
    public Integer calculateAlternateCourseHandicap(BigDecimal tripIndex, CourseTee courseTee) {
        if (tripIndex == null) {
            return null;
        }
        if (courseTee == null) {
            throw new IllegalArgumentException("courseTee is required");
        }

        BigDecimal courseRating = courseTee.getAlternateCourseRating() != null
                ? courseTee.getAlternateCourseRating()
                : courseTee.getCourseRating();

        Integer slope = courseTee.getAlternateSlope() != null
                ? courseTee.getAlternateSlope()
                : courseTee.getSlope();

        Integer parTotal = courseTee.getParTotal();

        if (courseRating == null) {
            throw new IllegalArgumentException("alternate or standard courseRating is required");
        }
        if (slope == null || slope <= 0) {
            throw new IllegalArgumentException("alternate or standard slope must be > 0");
        }
        if (parTotal == null) {
            throw new IllegalArgumentException("courseTee.parTotal is required");
        }

        BigDecimal result = tripIndex
                .multiply(BigDecimal.valueOf(slope))
                .divide(STANDARD_SLOPE, 10, RoundingMode.HALF_UP)
                .add(courseRating.subtract(BigDecimal.valueOf(parTotal)));

        return result.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
