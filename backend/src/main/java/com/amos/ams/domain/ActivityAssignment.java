package com.amos.ams.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity_assignments")
public class ActivityAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private MaintenanceActivity activity;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "role_on_job")
    private String roleOnJob;
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt = Instant.now();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MaintenanceActivity getActivity() { return activity; }
    public void setActivity(MaintenanceActivity activity) { this.activity = activity; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRoleOnJob() { return roleOnJob; }
    public void setRoleOnJob(String roleOnJob) { this.roleOnJob = roleOnJob; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }
}
