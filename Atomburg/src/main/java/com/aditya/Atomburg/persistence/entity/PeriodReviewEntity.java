package com.aditya.Atomburg.persistence.entity;

import com.aditya.Atomburg.domain.PortalDomain.ReviewPeriod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(
        name = "period_review",
        uniqueConstraints = @UniqueConstraint(name = "uk_period_review_sheet_period", columnNames = {"goal_sheet_id", "review_period"})
)
public class PeriodReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_sheet_id", nullable = false)
    private GoalSheetEntity goalSheet;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_period", nullable = false, length = 32)
    private ReviewPeriod period;

    @Column(name = "manager_comment", length = 1500)
    private String managerComment;

    @Column(name = "manager_id", length = 64)
    private String managerId;

    @Column(name = "manager_reviewed_at")
    private LocalDateTime managerReviewedAt;

    @OneToMany(mappedBy = "periodReview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalCheckInEntity> checkIns = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public GoalSheetEntity getGoalSheet() {
        return goalSheet;
    }

    public void setGoalSheet(GoalSheetEntity goalSheet) {
        this.goalSheet = goalSheet;
    }

    public ReviewPeriod getPeriod() {
        return period;
    }

    public void setPeriod(ReviewPeriod period) {
        this.period = period;
    }

    public String getManagerComment() {
        return managerComment;
    }

    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public LocalDateTime getManagerReviewedAt() {
        return managerReviewedAt;
    }

    public void setManagerReviewedAt(LocalDateTime managerReviewedAt) {
        this.managerReviewedAt = managerReviewedAt;
    }

    public List<GoalCheckInEntity> getCheckIns() {
        return checkIns;
    }

    public GoalCheckInEntity addCheckIn(GoalCheckInEntity checkIn) {
        checkIns.add(checkIn);
        checkIn.setPeriodReview(this);
        return checkIn;
    }

    public Optional<GoalCheckInEntity> findCheckIn(Long goalId) {
        if (goalId == null) {
            return Optional.empty();
        }
        return checkIns.stream()
                .filter(checkIn -> checkIn.getGoal() != null)
                .filter(checkIn -> checkIn.getGoal().getId() != null)
                .filter(checkIn -> goalId.equals(checkIn.getGoal().getId()))
                .findFirst();
    }
}
