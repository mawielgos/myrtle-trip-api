package com.myrtletrip.scorehistory.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DifferentialService {

    public BigDecimal calculate(Integer adjustedScore, BigDecimal courseRating, Integer slope) {
        if (adjustedScore == null || courseRating == null || slope == null || slope == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(adjustedScore)
                .subtract(courseRating)
                .multiply(BigDecimal.valueOf(113))
                .divide(BigDecimal.valueOf(slope), 1, RoundingMode.HALF_UP);
    }
}
