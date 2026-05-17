package com.aditya.Atomburg.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goal_cycle")
public class GoalCycleEntity {

    @Id
    @Column(name = "cycle_year", nullable = false)
    private Integer year;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CycleWindowEntity> windows = new ArrayList<>();

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<CycleWindowEntity> getWindows() {
        return windows;
    }

    public void addWindow(CycleWindowEntity window) {
        windows.add(window);
        window.setCycle(this);
    }

    public void replaceWindows(List<CycleWindowEntity> newWindows) {
        windows.clear();
        for (CycleWindowEntity window : newWindows) {
            addWindow(window);
        }
    }
}
