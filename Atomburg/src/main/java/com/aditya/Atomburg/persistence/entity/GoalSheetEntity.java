package com.aditya.Atomburg.persistence.entity;

import com.aditya.Atomburg.domain.PortalDomain.GoalSheetStatus;
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
        name = "goal_sheet",
        uniqueConstraints = @UniqueConstraint(name = "uk_goal_sheet_employee_year", columnNames = {"employee_id", "cycle_year"})
)
public class GoalSheetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cycle_year", nullable = false)
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private AppUserEntity employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "sheet_status", nullable = false, length = 32)
    private GoalSheetStatus status = GoalSheetStatus.DRAFT;

    @OneToMany(mappedBy = "goalSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalEntity> goals = new ArrayList<>();

    @OneToMany(mappedBy = "goalSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PeriodReviewEntity> periodReviews = new ArrayList<>();

    @Column(name = "submitted_by", length = 64)
    private String submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_by", length = 64)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "last_manager_comment", length = 1000)
    private String lastManagerComment;

    @Column(name = "last_unlocked_reason", length = 1000)
    private String lastUnlockedReason;

    @Column(name = "unlocked_by", length = 64)
    private String unlockedBy;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @Column(name = "editable_after_unlock", nullable = false)
    private boolean editableAfterUnlock;

    public Long getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public AppUserEntity getEmployee() {
        return employee;
    }

    public void setEmployee(AppUserEntity employee) {
        this.employee = employee;
    }

    public GoalSheetStatus getStatus() {
        return status;
    }

    public void setStatus(GoalSheetStatus status) {
        this.status = status;
    }

    public List<GoalEntity> getGoals() {
        return goals;
    }

    public List<PeriodReviewEntity> getPeriodReviews() {
        return periodReviews;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLastManagerComment() {
        return lastManagerComment;
    }

    public void setLastManagerComment(String lastManagerComment) {
        this.lastManagerComment = lastManagerComment;
    }

    public String getLastUnlockedReason() {
        return lastUnlockedReason;
    }

    public void setLastUnlockedReason(String lastUnlockedReason) {
        this.lastUnlockedReason = lastUnlockedReason;
    }

    public String getUnlockedBy() {
        return unlockedBy;
    }

    public void setUnlockedBy(String unlockedBy) {
        this.unlockedBy = unlockedBy;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }

    public boolean isEditableAfterUnlock() {
        return editableAfterUnlock;
    }

    public void setEditableAfterUnlock(boolean editableAfterUnlock) {
        this.editableAfterUnlock = editableAfterUnlock;
    }

    public void addGoal(GoalEntity goal) {
        goals.add(goal);
        goal.setGoalSheet(this);
    }

    public void removeGoal(GoalEntity goal) {
        goals.remove(goal);
        goal.setGoalSheet(null);
    }

    public PeriodReviewEntity addReview(PeriodReviewEntity review) {
        periodReviews.add(review);
        review.setGoalSheet(this);
        return review;
    }

    public Optional<PeriodReviewEntity> findReview(ReviewPeriod period) {
        return periodReviews.stream().filter(review -> review.getPeriod() == period).findFirst();
    }

    public PeriodReviewEntity getOrCreateReview(ReviewPeriod period) {
        return findReview(period).orElseGet(() -> {
            PeriodReviewEntity review = new PeriodReviewEntity();
            review.setPeriod(period);
            addReview(review);
            return review;
        });
    }

    public boolean hasEverBeenLocked() {
        return lockedAt != null;
    }

    public int totalWeightage() {
        return goals.stream().mapToInt(GoalEntity::getWeightage).sum();
    }
}
