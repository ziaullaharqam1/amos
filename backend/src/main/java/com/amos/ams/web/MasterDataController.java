package com.amos.ams.web;

import com.amos.ams.dto.Dtos;
import com.amos.ams.security.CurrentUser;
import com.amos.ams.service.MasterDataService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MasterDataController {
    private final MasterDataService masters;
    private final CurrentUser currentUser;

    public MasterDataController(MasterDataService masters, CurrentUser currentUser) {
        this.masters = masters;
        this.currentUser = currentUser;
    }

    @GetMapping("/aircraft-types")
    @PreAuthorize("hasAuthority('AIRCRAFT_VIEW')")
    public List<Dtos.AircraftTypeView> types() { return masters.listTypes(); }

    @PostMapping("/aircraft-types")
    @PreAuthorize("hasAuthority('AIRCRAFT_MANAGE')")
    public Dtos.AircraftTypeView createType(@Valid @RequestBody Dtos.AircraftTypeUpsert req) {
        return masters.saveType(null, req, currentUser.require());
    }

    @PutMapping("/aircraft-types/{id}")
    @PreAuthorize("hasAuthority('AIRCRAFT_MANAGE')")
    public Dtos.AircraftTypeView updateType(@PathVariable Long id, @Valid @RequestBody Dtos.AircraftTypeUpsert req) {
        return masters.saveType(id, req, currentUser.require());
    }

    @GetMapping("/aircraft")
    @PreAuthorize("hasAuthority('AIRCRAFT_VIEW')")
    public List<Dtos.AircraftView> aircraft() { return masters.listAircraft(); }

    @PostMapping("/aircraft")
    @PreAuthorize("hasAuthority('AIRCRAFT_MANAGE')")
    public Dtos.AircraftView createAircraft(@Valid @RequestBody Dtos.AircraftUpsert req) {
        return masters.saveAircraft(null, req, currentUser.require());
    }

    @PutMapping("/aircraft/{id}")
    @PreAuthorize("hasAuthority('AIRCRAFT_MANAGE')")
    public Dtos.AircraftView updateAircraft(@PathVariable Long id, @Valid @RequestBody Dtos.AircraftUpsert req) {
        return masters.saveAircraft(id, req, currentUser.require());
    }

    @GetMapping("/components")
    @PreAuthorize("hasAuthority('COMPONENT_VIEW')")
    public List<Dtos.ComponentView> components() { return masters.listComponents(); }

    @PostMapping("/components")
    @PreAuthorize("hasAuthority('COMPONENT_MANAGE')")
    public Dtos.ComponentView createComponent(@Valid @RequestBody Dtos.ComponentUpsert req) {
        return masters.saveComponent(null, req, currentUser.require());
    }

    @PutMapping("/components/{id}")
    @PreAuthorize("hasAuthority('COMPONENT_MANAGE')")
    public Dtos.ComponentView updateComponent(@PathVariable Long id, @Valid @RequestBody Dtos.ComponentUpsert req) {
        return masters.saveComponent(id, req, currentUser.require());
    }

    @GetMapping("/check-types")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public List<Dtos.CheckTypeView> checkTypes() { return masters.listCheckTypes(); }

    @PostMapping("/check-types")
    @PreAuthorize("hasAuthority('TASK_MANAGE')")
    public Dtos.CheckTypeView createCheck(@Valid @RequestBody Dtos.CheckTypeUpsert req) {
        return masters.saveCheckType(null, req, currentUser.require());
    }

    @PutMapping("/check-types/{id}")
    @PreAuthorize("hasAuthority('TASK_MANAGE')")
    public Dtos.CheckTypeView updateCheck(@PathVariable Long id, @Valid @RequestBody Dtos.CheckTypeUpsert req) {
        return masters.saveCheckType(id, req, currentUser.require());
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('TASK_VIEW')")
    public List<Dtos.TaskView> tasks() { return masters.listTasks(); }

    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority('TASK_MANAGE')")
    public Dtos.TaskView createTask(@Valid @RequestBody Dtos.TaskUpsert req) {
        return masters.saveTask(null, req, currentUser.require());
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('TASK_MANAGE')")
    public Dtos.TaskView updateTask(@PathVariable Long id, @Valid @RequestBody Dtos.TaskUpsert req) {
        return masters.saveTask(id, req, currentUser.require());
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public List<Dtos.ScheduleView> schedules() { return masters.listSchedules(); }

    @PostMapping("/schedules")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public Dtos.ScheduleView createSchedule(@Valid @RequestBody Dtos.ScheduleUpsert req) {
        return masters.saveSchedule(null, req, currentUser.require());
    }

    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public Dtos.ScheduleView updateSchedule(@PathVariable Long id, @Valid @RequestBody Dtos.ScheduleUpsert req) {
        return masters.saveSchedule(id, req, currentUser.require());
    }
}
