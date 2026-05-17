package com.aditya.Atomburg.domain;

public final class PortalDomain {

    private PortalDomain() {
    }

    public enum Role {
        EMPLOYEE,
        MANAGER,
        ADMIN
    }

    public enum GoalSheetStatus {
        DRAFT,
        SUBMITTED,
        RETURNED_FOR_REWORK,
        APPROVED
    }

    public enum GoalStatus {
        NOT_STARTED,
        ON_TRACK,
        COMPLETED
    }

    public enum UomType {
        NUMERIC,
        PERCENTAGE,
        TIMELINE,
        ZERO_BASED
    }

    public enum GoalDirection {
        HIGHER_IS_BETTER,
        LOWER_IS_BETTER,
        TIMELINE,
        ZERO_BASED
    }

    public enum SharedGoalOwnership {
        NONE,
        PRIMARY_OWNER,
        RECIPIENT
    }

    public enum ReviewPeriod {
        GOAL_SETTING,
        Q1,
        Q2,
        Q3,
        Q4;

        public boolean isCheckInPeriod() {
            return this != GOAL_SETTING;
        }
    }
}
