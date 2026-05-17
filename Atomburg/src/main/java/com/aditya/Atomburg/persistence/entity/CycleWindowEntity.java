package com.aditya.Atomburg.persistence.entity;

import com.aditya.Atomburg.domain.PortalDomain.ReviewPeriod;
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

@Entity
@Table(
        name = "cycle_window",
        uniqueConstraints = @UniqueConstraint(name = "uk_cycle_window_period", columnNames = {"cycle_year", "review_period"})
)
public class CycleWindowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_year", nullable = false)
    private GoalCycleEntity cycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_period", nullable = false, length = 32)
    private ReviewPeriod period;

    @Column(name = "start_month", nullable = false)
    private Integer month;

    @Column(name = "start_day", nullable = false)
    private Integer dayOfMonth;

    @Column(name = "year_offset", nullable = false)
    private Integer yearOffset;

    @Column(name = "window_action", nullable = false, length = 180)
    private String action;

    public Long getId() {
        return id;
    }

    public GoalCycleEntity getCycle() {
        return cycle;
    }

    public void setCycle(GoalCycleEntity cycle) {
        this.cycle = cycle;
    }

    public ReviewPeriod getPeriod() {
        return period;
    }

    public void setPeriod(ReviewPeriod period) {
        this.period = period;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Integer getYearOffset() {
        return yearOffset;
    }

    public void setYearOffset(Integer yearOffset) {
        this.yearOffset = yearOffset;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
