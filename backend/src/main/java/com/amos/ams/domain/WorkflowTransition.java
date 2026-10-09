package com.amos.ams.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workflow_transitions")
public class WorkflowTransition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private MaintenanceActivity activity;
    @Column(name = "from_state", nullable = false)
    private String fromState;
    @Column(name = "to_state", nullable = false)
    private String toState;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;
    private String comment;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MaintenanceActivity getActivity() { return activity; }
    public void setActivity(MaintenanceActivity activity) { this.activity = activity; }
    public String getFromState() { return fromState; }
    public void setFromState(String fromState) { this.fromState = fromState; }
    public String getToState() { return toState; }
    public void setToState(String toState) { this.toState = toState; }
    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
