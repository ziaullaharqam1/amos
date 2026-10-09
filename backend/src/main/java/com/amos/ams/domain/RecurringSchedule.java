package com.amos.ams.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recurring_schedules")
public class RecurringSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private MaintenanceTask task;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_type_id")
    private CheckType checkType;
    @Column(name = "interval_hours")
    private BigDecimal intervalHours;
    @Column(name = "interval_days")
    private Integer intervalDays;
    @Column(name = "interval_cycles")
    private Integer intervalCycles;
    @Column(name = "last_performed_at")
    private Instant lastPerformedAt;
    @Column(name = "last_hours")
    private BigDecimal lastHours;
    @Column(name = "last_cycles")
    private Integer lastCycles;
    @Column(name = "next_due_at")
    private Instant nextDueAt;
    @Column(name = "next_due_hours")
    private BigDecimal nextDueHours;
    @Column(name = "next_due_cycles")
    private Integer nextDueCycles;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Aircraft getAircraft() { return aircraft; }
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }
    public MaintenanceTask getTask() { return task; }
    public void setTask(MaintenanceTask task) { this.task = task; }
    public CheckType getCheckType() { return checkType; }
    public void setCheckType(CheckType checkType) { this.checkType = checkType; }
    public BigDecimal getIntervalHours() { return intervalHours; }
    public void setIntervalHours(BigDecimal intervalHours) { this.intervalHours = intervalHours; }
    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
    public Integer getIntervalCycles() { return intervalCycles; }
    public void setIntervalCycles(Integer intervalCycles) { this.intervalCycles = intervalCycles; }
    public Instant getLastPerformedAt() { return lastPerformedAt; }
    public void setLastPerformedAt(Instant lastPerformedAt) { this.lastPerformedAt = lastPerformedAt; }
    public BigDecimal getLastHours() { return lastHours; }
    public void setLastHours(BigDecimal lastHours) { this.lastHours = lastHours; }
    public Integer getLastCycles() { return lastCycles; }
    public void setLastCycles(Integer lastCycles) { this.lastCycles = lastCycles; }
    public Instant getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Instant nextDueAt) { this.nextDueAt = nextDueAt; }
    public BigDecimal getNextDueHours() { return nextDueHours; }
    public void setNextDueHours(BigDecimal nextDueHours) { this.nextDueHours = nextDueHours; }
    public Integer getNextDueCycles() { return nextDueCycles; }
    public void setNextDueCycles(Integer nextDueCycles) { this.nextDueCycles = nextDueCycles; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
