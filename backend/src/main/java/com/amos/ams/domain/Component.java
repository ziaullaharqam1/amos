package com.amos.ams.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "components")
public class Component {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "part_number", nullable = false)
    private String partNumber;
    @Column(name = "serial_number", nullable = false)
    private String serialNumber;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String category;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id")
    private Aircraft aircraft;
    @Column(nullable = false)
    private String status = "INSTALLED";
    @Column(name = "life_limit_hours")
    private BigDecimal lifeLimitHours;
    @Column(name = "life_limit_cycles")
    private Integer lifeLimitCycles;
    @Column(name = "accumulated_hours", nullable = false)
    private BigDecimal accumulatedHours = BigDecimal.ZERO;
    @Column(name = "accumulated_cycles", nullable = false)
    private Integer accumulatedCycles = 0;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Aircraft getAircraft() { return aircraft; }
    public void setAircraft(Aircraft aircraft) { this.aircraft = aircraft; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getLifeLimitHours() { return lifeLimitHours; }
    public void setLifeLimitHours(BigDecimal lifeLimitHours) { this.lifeLimitHours = lifeLimitHours; }
    public Integer getLifeLimitCycles() { return lifeLimitCycles; }
    public void setLifeLimitCycles(Integer lifeLimitCycles) { this.lifeLimitCycles = lifeLimitCycles; }
    public BigDecimal getAccumulatedHours() { return accumulatedHours; }
    public void setAccumulatedHours(BigDecimal accumulatedHours) { this.accumulatedHours = accumulatedHours; }
    public Integer getAccumulatedCycles() { return accumulatedCycles; }
    public void setAccumulatedCycles(Integer accumulatedCycles) { this.accumulatedCycles = accumulatedCycles; }
}
