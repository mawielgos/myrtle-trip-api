package com.myrtletrip.handicap.service;

import com.myrtletrip.round.entity.RoundTee;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CourseHandicapService {

    public Integer calculateCourseHandicap(BigDecimal handicapIndex, RoundTee roundTee) {
        if (handicapIndex == null || roundTee == null) {
            return null;
        }

        BigDecimal slope = roundTee.getSlope() == null
                ? null
                : BigDecimal.valueOf(roundTee.getSlope());

        BigDecimal courseRating = roundTee.getCourseRating();
        Integer parTotal = roundTee.getParTotal();

        if (slope == null || courseRating == null || parTotal == null) {
            return null;
        }

        BigDecimal courseHandicap = handicapIndex
                .multiply(slope)
                .divide(BigDecimal.valueOf(113), 10, RoundingMode.HALF_UP)
                .add(courseRating.subtract(BigDecimal.valueOf(parTotal)));

        return courseHandicap.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
