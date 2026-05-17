package com.aditya.Atomburg.persistence.entity;

import com.aditya.Atomburg.domain.PortalDomain.GoalDirection;
import com.aditya.Atomburg.domain.PortalDomain.SharedGoalOwnership;
import com.aditya.Atomburg.domain.PortalDomain.UomType;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "goal_item")
public class GoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_sheet_id", nullable = false)
    private GoalSheetEntity goalSheet;

    @Column(name = "thrust_area", nullable = false, length = 150)
    private String thrustArea;

    @Column(name = "goal_title", nullable = false, length = 200)
    private String title;

    @Column(name = "goal_description", nullable = false, length = 1500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "uom_type", nullable = false, length = 32)
    private UomType uomType;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_direction", nullable = false, length = 32)
    private GoalDirection direction;

    @Column(name = "target_value", length = 100)
    private String targetValue;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "weightage", nullable = false)
    private Integer weightage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_goal_id")
    private SharedGoalEntity sharedGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "shared_goal_ownership", nullable = false, length = 32)
    private SharedGoalOwnership sharedGoalOwnership = SharedGoalOwnership.NONE;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public GoalSheetEntity getGoalSheet() {
        return goalSheet;
    }

    public void setGoalSheet(GoalSheetEntity goalSheet) {
        this.goalSheet = goalSheet;
    }

    public String getThrustArea() {
        return thrustArea;
    }

    public void setThrustArea(String thrustArea) {
        this.thrustArea = thrustArea;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UomType getUomType() {
        return uomType;
    }

    public void setUomType(UomType uomType) {
        this.uomType = uomType;
    }

    public GoalDirection getDirection() {
        return direction;
    }

    public void setDirection(GoalDirection direction) {
        this.direction = direction;
    }

    public String getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(String targetValue) {
        this.targetValue = targetValue;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Integer getWeightage() {
        return weightage;
    }

    public void setWeightage(Integer weightage) {
        this.weightage = weightage;
    }

    public SharedGoalEntity getSharedGoal() {
        return sharedGoal;
    }

    public void setSharedGoal(SharedGoalEntity sharedGoal) {
        this.sharedGoal = sharedGoal;
    }

    public SharedGoalOwnership getSharedGoalOwnership() {
        return sharedGoalOwnership;
    }

    public void setSharedGoalOwnership(SharedGoalOwnership sharedGoalOwnership) {
        this.sharedGoalOwnership = sharedGoalOwnership;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public boolean isSharedGoal() {
        return sharedGoal != null;
    }
}
