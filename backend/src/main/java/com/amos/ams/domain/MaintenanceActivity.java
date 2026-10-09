package com.amos.ams.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "maintenance_activities")
public class MaintenanceActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "activity_number", nullable = false, unique = true)
    private String activityNumber;
    @Column(nullable = false)
    private String title;
    private String description;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_type_id")
    private CheckType checkType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private MaintenanceTask task;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private Component component;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private MaintenanceActivity parent;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private RecurringSchedule schedule;
    @Column(nullable = false)
    private String state = "PENDING";
    @Column(nullable = false)
    private String priority = "NORMAL";
    @Column(name = "due_at")
    private Instant dueAt;
    @Column(name = "planned_start")
    private Instant plannedStart;
    @Column(name = "planned_end")
    private Instant plannedEnd;
    @Column(name = "actual_start")
    private Instant actualStart;
    @Column(name = "actual_end")
    private Instant actualEnd;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id")
    private User requestedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;
    private String station;
    private String findings;
    @Column(name = "actions_taken")
    private String actionsTaken;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityAssignment> assignments = new ArrayList<>();
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityLog> logs = new ArrayList<>();
    @OneToMany(mappedBy = "parent")
    private List<MaintenanceActivity> children = new ArrayList<>();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getActivityNumber() { return activityNumber; }
    public void setActivityNumber(String activityNumber) { this.activityNumber = activityNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Aircraft getAircraft() { return aircraft; }
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }
    public CheckType getCheckType() { return checkType; }
    public void setCheckType(CheckType checkType) { this.checkType = checkType; }
    public MaintenanceTask getTask() { return task; }
    public void setTask(MaintenanceTask task) { this.task = task; }
    public Component getComponent() { return component; }
    public void setComponent(Component component) { this.component = component; }
    public MaintenanceActivity getParent() { return parent; }
    public void setParent(MaintenanceActivity parent) { this.parent = parent; }
    public RecurringSchedule getSchedule() { return schedule; }
    public void setSchedule(RecurringSchedule schedule) { this.schedule = schedule; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getPlannedStart() { return plannedStart; }
    public void setPlannedStart(Instant plannedStart) { this.plannedStart = plannedStart; }
    public Instant getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(Instant plannedEnd) { this.plannedEnd = plannedEnd; }
    public Instant getActualStart() { return actualStart; }
    public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }
    public Instant getActualEnd() { return actualEnd; }
    public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public User getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(User verifiedBy) { this.verifiedBy = verifiedBy; }
    public String getStation() { return station; }
    public void setStation(String station) { this.station = station; }
    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }
    public String getActionsTaken() { return actionsTaken; }
    public void setActionsTaken(String actionsTaken) { this.actionsTaken = actionsTaken; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<ActivityAssignment> getAssignments() { return assignments; }
    public List<ActivityLog> getLogs() { return logs; }
    public List<MaintenanceActivity> getChildren() { return children; }
}
