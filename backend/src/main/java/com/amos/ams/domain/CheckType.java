package com.amos.ams.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "check_types")
public class CheckType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String name;
    private String description;
    @Column(name = "typical_downtime_hours")
    private Integer typicalDowntimeHours;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getTypicalDowntimeHours() { return typicalDowntimeHours; }
    public void setTypicalDowntimeHours(Integer typicalDowntimeHours) { this.typicalDowntimeHours = typicalDowntimeHours; }
}
