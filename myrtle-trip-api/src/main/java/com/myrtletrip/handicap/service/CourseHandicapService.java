package com.myrtletrip.handicap.service;

import com.myrtletrip.round.entity.RoundTee;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CourseHandicapService {

    public Integer calculateCourseHandicap(BigDecimal handicapIndex, RoundTee roundTee) {
        return calculateCourseHandicap(handicapIndex, roundTee, "M");
    }

    public Integer calculateCourseHandicap(BigDecimal handicapIndex, RoundTee roundTee, String gender) {
        if (handicapIndex == null || roundTee == null) {
            return null;
        }

        BigDecimal courseRating = resolveCourseRating(roundTee, gender);
        Integer slopeValue = resolveSlope(roundTee, gender);
        Integer parTotal = resolveParTotal(roundTee, gender);
        BigDecimal slope = slopeValue == null ? null : BigDecimal.valueOf(slopeValue);

        if (slope == null || courseRating == null || parTotal == null) {
            return null;
        }

        BigDecimal courseHandicap = handicapIndex
                .multiply(slope)
                .divide(BigDecimal.valueOf(113), 10, RoundingMode.HALF_UP)
                .add(courseRating.subtract(BigDecimal.valueOf(parTotal)));

        return courseHandicap.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal resolveCourseRating(RoundTee roundTee, String gender) {
        if (roundTee.getSourceCourseTee() != null) {
            return roundTee.getSourceCourseTee().getRatingForGender(gender);
        }
        return roundTee.getCourseRating();
    }

    private Integer resolveSlope(RoundTee roundTee, String gender) {
        if (roundTee.getSourceCourseTee() != null) {
            return roundTee.getSourceCourseTee().getSlopeForGender(gender);
        }
        return roundTee.getSlope();
    }

    private Integer resolveParTotal(RoundTee roundTee, String gender) {
        if (roundTee.getSourceCourseTee() != null) {
            return roundTee.getSourceCourseTee().getParForGender(gender);
        }
        return roundTee.getParTotal();
    }
}
