package com.aditya.Atomburg.api;

import com.aditya.Atomburg.domain.PortalDomain.GoalDirection;
import com.aditya.Atomburg.domain.PortalDomain.GoalSheetStatus;
import com.aditya.Atomburg.domain.PortalDomain.GoalStatus;
import com.aditya.Atomburg.domain.PortalDomain.ReviewPeriod;
import com.aditya.Atomburg.domain.PortalDomain.Role;
import com.aditya.Atomburg.domain.PortalDomain.SharedGoalOwnership;
import com.aditya.Atomburg.domain.PortalDomain.UomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class PortalDtos {

    private PortalDtos() {
    }

    public record CreateGoalRequest(
            String thrustArea,
            String title,
            String description,
            UomType uomType,
            GoalDirection direction,
            String targetValue,
            LocalDate targetDate,
            Integer weightage
    ) {
    }

    public record UpdateGoalRequest(
            String thrustArea,
            String title,
            String description,
            UomType uomType,
            GoalDirection direction,
            String targetValue,
            LocalDate targetDate,
            Integer weightage
    ) {
    }

    public record ManagerGoalUpdateRequest(
            String targetValue,
            LocalDate targetDate,
            Integer weightage
    ) {
    }

    public record DecisionRequest(String comment) {
    }

    public record CheckInRequest(
            String actualValue,
            LocalDate actualDate,
            GoalStatus status
    ) {
    }

    public record ManagerCommentRequest(String comment) {
    }

    public record PushSharedGoalRequest(
            Integer year,
            String primaryOwnerId,
            List<String> recipientIds,
            String thrustArea,
            String title,
            String description,
            UomType uomType,
            GoalDirection direction,
            String targetValue,
            LocalDate targetDate,
            Integer primaryOwnerWeightage,
            Map<String, Integer> recipientWeightages
    ) {
    }

    public record UnlockSheetRequest(String reason) {
    }

    public record UpsertUserRequest(
            String userId,
            String name,
            Role role,
            String managerId,
            String department
    ) {
    }

    public record UpdateManagerRequest(String managerId) {
    }

    public record CycleWindowRequest(
            ReviewPeriod period,
            Integer month,
            Integer dayOfMonth,
            Integer yearOffset,
            String action
    ) {
    }

    public record UpdateCycleRequest(List<CycleWindowRequest> windows) {
    }

    public record UserResponse(
            String id,
            String name,
            Role role,
            String managerId,
            String department
    ) {
    }

    public record GoalCheckInResponse(
            long goalId,
            String actualValue,
            LocalDate actualDate,
            GoalStatus status,
            BigDecimal progressScore,
            String updatedBy,
            LocalDateTime updatedAt
    ) {
    }

    public record PeriodReviewResponse(
            ReviewPeriod period,
            List<GoalCheckInResponse> checkIns,
            String managerComment,
            String managerId,
            LocalDateTime managerReviewedAt,
            boolean employeeCompleted,
            boolean managerCompleted
    ) {
    }

    public record GoalResponse(
            long id,
            String thrustArea,
            String title,
            String description,
            UomType uomType,
            GoalDirection direction,
            String targetValue,
            LocalDate targetDate,
            Integer weightage,
            String sharedGoalId,
            SharedGoalOwnership sharedGoalOwnership,
            String createdBy,
            LocalDateTime createdAt,
            String updatedBy,
            LocalDateTime updatedAt
    ) {
    }

    public record GoalSheetResponse(
            String sheetId,
            Integer year,
            UserResponse employee,
            UserResponse manager,
            GoalSheetStatus status,
            Integer totalWeightage,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            LocalDateTime lockedAt,
            boolean editableAfterUnlock,
            String lastManagerComment,
            String lastUnlockedReason,
            List<GoalResponse> goals,
            List<PeriodReviewResponse> reviews
    ) {
    }

    public record TeamMemberSummaryResponse(
            UserResponse employee,
            GoalSheetStatus status,
            Integer goalCount,
            Integer totalWeightage,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            boolean employeeCheckInCompleted,
            boolean managerCheckInCompleted
    ) {
    }

    public record AuditEntryResponse(
            long id,
            String actorId,
            String actorName,
            String employeeId,
            int year,
            Long goalId,
            String action,
            String fieldName,
            String oldValue,
            String newValue,
            LocalDateTime timestamp
    ) {
    }

    public record CycleWindowResponse(
            ReviewPeriod period,
            Integer month,
            Integer dayOfMonth,
            Integer yearOffset,
            String action
    ) {
    }

    public record GoalCycleResponse(
            Integer year,
            List<CycleWindowResponse> windows
    ) {
    }

    public record CompletionRowResponse(
            UserResponse employee,
            UserResponse manager,
            Integer goalCount,
            boolean employeeCompleted,
            boolean managerCompleted
    ) {
    }

    public record CompletionDashboardResponse(
            Integer year,
            ReviewPeriod period,
            Integer totalEmployees,
            long employeesCompleted,
            long managersCompleted,
            BigDecimal employeeCompletionRate,
            BigDecimal managerCompletionRate,
            List<CompletionRowResponse> rows
    ) {
    }

    public record SessionResponse(
            String actorHint,
            LocalDate businessDate,
            List<UserResponse> users,
            GoalCycleResponse cycle
    ) {
    }
}
