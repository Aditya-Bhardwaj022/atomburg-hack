package com.aditya.Atomburg.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shared_goal")
public class SharedGoalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_key", nullable = false, unique = true, length = 64)
    private String referenceKey;

    @Column(name = "cycle_year", nullable = false)
    private Integer year;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_owner_id", nullable = false)
    private AppUserEntity primaryOwner;

    @OneToMany(mappedBy = "sharedGoal")
    private List<GoalEntity> goals = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getReferenceKey() {
        return referenceKey;
    }

    public void setReferenceKey(String referenceKey) {
        this.referenceKey = referenceKey;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public AppUserEntity getPrimaryOwner() {
        return primaryOwner;
    }

    public void setPrimaryOwner(AppUserEntity primaryOwner) {
        this.primaryOwner = primaryOwner;
    }

    public List<GoalEntity> getGoals() {
        return goals;
    }
}
