package com.amos.ams.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "maintenance_tasks")
public class MaintenanceTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "task_card", nullable = false, unique = true)
    private String taskCard;
    @Column(nullable = false)
    private String title;
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_type_id")
    private CheckType checkType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_type_id")
    private AircraftType aircraftType;
    @Column(name = "ata_chapter")
    private String ataChapter;
    @Column(name = "estimated_hours")
    private BigDecimal estimatedHours;
    private String skill;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTaskCard() { return taskCard; }
    public void setTaskCard(String taskCard) { this.taskCard = taskCard; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public CheckType getCheckType() { return checkType; }
    public void setCheckType(CheckType checkType) { this.checkType = checkType; }
    public AircraftType getAircraftType() { return aircraftType; }
    public void setAircraftType(AircraftType aircraftType) { this.aircraftType = aircraftType; }
    public String getAtaChapter() { return ataChapter; }
    public void setAtaChapter(String ataChapter) { this.ataChapter = ataChapter; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }
    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }
}
