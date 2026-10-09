package com.amos.ams.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "aircraft")
public class Aircraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String registration;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_type_id")
    private AircraftType aircraftType;
    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;
    @Column(nullable = false)
    private String status = "IN_SERVICE";
    @Column(name = "total_flight_hours", nullable = false)
    private BigDecimal totalFlightHours = BigDecimal.ZERO;
    @Column(name = "total_cycles", nullable = false)
    private Integer totalCycles = 0;
    @Column(name = "base_station")
    private String baseStation;
    @Column(name = "in_service_date")
    private LocalDate inServiceDate;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRegistration() { return registration; }
    public void setRegistration(String registration) { this.registration = registration; }
    public AircraftType getAircraftType() { return aircraftType; }
    public void setAircraftType(AircraftType aircraftType) { this.aircraftType = aircraftType; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getTotalFlightHours() { return totalFlightHours; }
    public void setTotalFlightHours(BigDecimal totalFlightHours) { this.totalFlightHours = totalFlightHours; }
    public Integer getTotalCycles() { return totalCycles; }
    public void setTotalCycles(Integer totalCycles) { this.totalCycles = totalCycles; }
    public String getBaseStation() { return baseStation; }
    public void setBaseStation(String baseStation) { this.baseStation = baseStation; }
    public LocalDate getInServiceDate() { return inServiceDate; }
    public void setInServiceDate(LocalDate inServiceDate) { this.inServiceDate = inServiceDate; }
}
