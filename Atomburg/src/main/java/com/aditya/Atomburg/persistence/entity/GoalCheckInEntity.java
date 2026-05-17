package com.aditya.Atomburg.persistence.entity;

import com.aditya.Atomburg.domain.PortalDomain.GoalStatus;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.CascadeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "goal_check_in",
        uniqueConstraints = @UniqueConstraint(name = "uk_goal_check_in_review_goal", columnNames = {"period_review_id", "goal_id"})
)
public class GoalCheckInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_review_id", nullable = false)
    private PeriodReviewEntity periodReview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "goal_id", nullable = false)
    private GoalEntity goal;

    @Column(name = "actual_value", length = 100)
    private String actualValue;

    @Column(name = "actual_date")
    private LocalDate actualDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_status", nullable = false, length = 32)
    private GoalStatus status;

    @Column(name = "progress_score", precision = 10, scale = 2)
    private BigDecimal progressScore;

    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public PeriodReviewEntity getPeriodReview() {
        return periodReview;
    }

    public void setPeriodReview(PeriodReviewEntity periodReview) {
        this.periodReview = periodReview;
    }

    public GoalEntity getGoal() {
        return goal;
    }

    public void setGoal(GoalEntity goal) {
        this.goal = goal;
    }

    public String getActualValue() {
        return actualValue;
    }

    public void setActualValue(String actualValue) {
        this.actualValue = actualValue;
    }

    public LocalDate getActualDate() {
        return actualDate;
    }

    public void setActualDate(LocalDate actualDate) {
        this.actualDate = actualDate;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }

    public BigDecimal getProgressScore() {
        return progressScore;
    }

    public void setProgressScore(BigDecimal progressScore) {
        this.progressScore = progressScore;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
