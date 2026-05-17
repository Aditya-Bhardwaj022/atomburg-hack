package com.aditya.Atomburg.service;

import com.aditya.Atomburg.api.PortalDtos;
import com.aditya.Atomburg.domain.PortalDomain.GoalSheetStatus;
import com.aditya.Atomburg.domain.PortalDomain.ReviewPeriod;
import com.aditya.Atomburg.persistence.entity.AppUserEntity;
import com.aditya.Atomburg.persistence.entity.AuditEntryEntity;
import com.aditya.Atomburg.persistence.entity.CycleWindowEntity;
import com.aditya.Atomburg.persistence.entity.GoalCheckInEntity;
import com.aditya.Atomburg.persistence.entity.GoalCycleEntity;
import com.aditya.Atomburg.persistence.entity.GoalEntity;
import com.aditya.Atomburg.persistence.entity.GoalSheetEntity;
import com.aditya.Atomburg.persistence.entity.PeriodReviewEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class GoalPortalMapper {
    private static final List<ReviewPeriod> ORDERED_PERIODS = List.of(
            ReviewPeriod.GOAL_SETTING,
            ReviewPeriod.Q1,
            ReviewPeriod.Q2,
            ReviewPeriod.Q3,
            ReviewPeriod.Q4
    );

    public PortalDtos.UserResponse toUserResponse(AppUserEntity user) {
        if (user == null) {
            return null;
        }
        return new PortalDtos.UserResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getManager() == null ? null : user.getManager().getId(),
                user.getDepartment()
        );
    }

    public PortalDtos.GoalCycleResponse toGoalCycleResponse(GoalCycleEntity cycle) {
        return new PortalDtos.GoalCycleResponse(
                cycle.getYear(),
                cycle.getWindows().stream()
                        .sorted(Comparator.comparingInt(this::windowSortKey))
                        .map(window -> new PortalDtos.CycleWindowResponse(
                                window.getPeriod(),
                                window.getMonth(),
                                window.getDayOfMonth(),
                                window.getYearOffset(),
                                window.getAction()
                        ))
                        .toList()
        );
    }

    public PortalDtos.GoalSheetResponse toGoalSheetResponse(GoalSheetEntity sheet) {
        return new PortalDtos.GoalSheetResponse(
                "sheet-" + sheet.getId(),
                sheet.getYear(),
                toUserResponse(sheet.getEmployee()),
                toUserResponse(sheet.getEmployee().getManager()),
                sheet.getStatus(),
                sheet.totalWeightage(),
                sheet.getSubmittedAt(),
                sheet.getApprovedAt(),
                sheet.getLockedAt(),
                sheet.isEditableAfterUnlock(),
                sheet.getLastManagerComment(),
                sheet.getLastUnlockedReason(),
                sheet.getGoals().stream()
                        .sorted(Comparator.comparingLong(GoalEntity::getId))
                        .map(this::toGoalResponse)
                        .toList(),
                sheet.getPeriodReviews().stream()
                        .sorted(Comparator.comparingInt(review -> ORDERED_PERIODS.indexOf(review.getPeriod())))
                        .map(review -> toPeriodReviewResponse(sheet, review))
                        .toList()
        );
    }

    public PortalDtos.AuditEntryResponse toAuditEntryResponse(AuditEntryEntity entry) {
        return new PortalDtos.AuditEntryResponse(
                entry.getId(),
                entry.getActorId(),
                entry.getActorName(),
                entry.getEmployeeId(),
                entry.getYear(),
                entry.getGoalId(),
                entry.getAction(),
                entry.getFieldName(),
                entry.getOldValue(),
                entry.getNewValue(),
                entry.getTimestamp()
        );
    }

    private PortalDtos.GoalResponse toGoalResponse(GoalEntity goal) {
        return new PortalDtos.GoalResponse(
                goal.getId(),
                goal.getThrustArea(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getUomType(),
                goal.getDirection(),
                goal.getTargetValue(),
                goal.getTargetDate(),
                goal.getWeightage(),
                goal.getSharedGoal() == null ? null : goal.getSharedGoal().getReferenceKey(),
                goal.getSharedGoalOwnership(),
                goal.getCreatedBy(),
                goal.getCreatedAt(),
                goal.getUpdatedBy(),
                goal.getUpdatedAt()
        );
    }

    private PortalDtos.PeriodReviewResponse toPeriodReviewResponse(GoalSheetEntity sheet, PeriodReviewEntity review) {
        return new PortalDtos.PeriodReviewResponse(
                review.getPeriod(),
                review.getCheckIns().stream()
                        .sorted(Comparator.comparing(checkIn -> checkIn.getGoal().getId()))
                        .map(this::toCheckInResponse)
                        .toList(),
                review.getManagerComment(),
                review.getManagerId(),
                review.getManagerReviewedAt(),
                isEmployeeReviewComplete(sheet, review.getPeriod()),
                isManagerReviewComplete(sheet, review.getPeriod())
        );
    }

    private PortalDtos.GoalCheckInResponse toCheckInResponse(GoalCheckInEntity checkIn) {
        return new PortalDtos.GoalCheckInResponse(
                checkIn.getGoal().getId(),
                checkIn.getActualValue(),
                checkIn.getActualDate(),
                checkIn.getStatus(),
                checkIn.getProgressScore(),
                checkIn.getUpdatedBy(),
                checkIn.getUpdatedAt()
        );
    }

    public boolean isEmployeeReviewComplete(GoalSheetEntity sheet, ReviewPeriod period) {
        if (sheet.getStatus() != GoalSheetStatus.APPROVED || sheet.getGoals().isEmpty()) {
            return false;
        }
        PeriodReviewEntity review = sheet.findReview(period).orElse(null);
        if (review == null) {
            return false;
        }
        return sheet.getGoals().stream()
                .allMatch(goal -> review.findCheckIn(goal.getId()).isPresent());
    }

    public boolean isManagerReviewComplete(GoalSheetEntity sheet, ReviewPeriod period) {
        PeriodReviewEntity review = sheet.findReview(period).orElse(null);
        return review != null && isEmployeeReviewComplete(sheet, period)
                && review.getManagerComment() != null && !review.getManagerComment().isBlank();
    }

    private int windowSortKey(CycleWindowEntity window) {
        return window.getYearOffset() * 10000 + window.getMonth() * 100 + window.getDayOfMonth();
    }
}
