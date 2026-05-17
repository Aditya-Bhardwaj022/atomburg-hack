package com.aditya.Atomburg.service;

import com.aditya.Atomburg.domain.PortalDomain.GoalDirection;
import com.aditya.Atomburg.domain.PortalDomain.GoalStatus;
import com.aditya.Atomburg.domain.PortalDomain.UomType;
import com.aditya.Atomburg.persistence.entity.GoalEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

@Component
public class ProgressScoreCalculator {

    private final Clock clock;

    public ProgressScoreCalculator(Clock clock) {
        this.clock = clock;
    }

    public BigDecimal computeProgress(GoalEntity goal, String actualValue, LocalDate actualDate, GoalStatus status) {
        return switch (goal.getUomType()) {
            case NUMERIC, PERCENTAGE -> computeNumericProgress(goal, actualValue);
            case ZERO_BASED -> "0".equals(normalize(actualValue)) ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
            case TIMELINE -> computeTimelineProgress(goal, actualDate, status);
        };
    }

    private BigDecimal computeNumericProgress(GoalEntity goal, String actualValue) {
        BigDecimal target = new BigDecimal(goal.getTargetValue());
        BigDecimal actual = new BigDecimal(normalize(actualValue));
        if (goal.getDirection() == GoalDirection.HIGHER_IS_BETTER) {
            if (target.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO;
            }
            return actual.divide(target, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (actual.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        return target.divide(actual, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeTimelineProgress(GoalEntity goal, LocalDate actualDate, GoalStatus status) {
        if (actualDate != null && goal.getTargetDate() != null) {
            return actualDate.isAfter(goal.getTargetDate()) ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        if (status == GoalStatus.ON_TRACK && goal.getTargetDate() != null && !LocalDate.now(clock).isAfter(goal.getTargetDate())) {
            return BigDecimal.valueOf(50);
        }
        return BigDecimal.ZERO;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
