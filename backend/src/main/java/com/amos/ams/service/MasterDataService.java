package com.amos.ams.service;

import com.amos.ams.audit.AuditService;
import com.amos.ams.domain.*;
import com.amos.ams.dto.Dtos;
import com.amos.ams.exception.ApiException;
import com.amos.ams.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MasterDataService {
    private final AircraftTypeRepository aircraftTypes;
    private final AircraftRepository aircraft;
    private final ComponentRepository components;
    private final CheckTypeRepository checkTypes;
    private final MaintenanceTaskRepository tasks;
    private final RecurringScheduleRepository schedules;
    private final AuditService audit;

    public MasterDataService(AircraftTypeRepository aircraftTypes, AircraftRepository aircraft,
                             ComponentRepository components, CheckTypeRepository checkTypes,
                             MaintenanceTaskRepository tasks, RecurringScheduleRepository schedules,
                             AuditService audit) {
        this.aircraftTypes = aircraftTypes;
        this.aircraft = aircraft;
        this.components = components;
        this.checkTypes = checkTypes;
        this.tasks = tasks;
        this.schedules = schedules;
        this.audit = audit;
    }

    public List<Dtos.AircraftTypeView> listTypes() {
        return aircraftTypes.findAll().stream().map(this::typeView).toList();
    }

    @Transactional
    public Dtos.AircraftTypeView saveType(Long id, Dtos.AircraftTypeUpsert req, User actor) {
        AircraftType t = id == null ? new AircraftType() : aircraftTypes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Aircraft type not found"));
        Object before = id == null ? null : typeView(t);
        t.setIcaoCode(req.icaoCode());
        t.setManufacturer(req.manufacturer());
        t.setModel(req.model());
        t.setDescription(req.description());
        aircraftTypes.save(t);
        audit.record(actor, id == null ? "CREATE" : "UPDATE", "AircraftType", t.getId(), before, typeView(t));
        return typeView(t);
    }

    public List<Dtos.AircraftView> listAircraft() {
        return aircraft.findAll().stream().map(this::aircraftView).toList();
    }

    @Transactional
    public Dtos.AircraftView saveAircraft(Long id, Dtos.AircraftUpsert req, User actor) {
        Aircraft a = id == null ? new Aircraft() : aircraft.findById(id)
                .orElseThrow(() -> ApiException.notFound("Aircraft not found"));
        Object before = id == null ? null : aircraftView(a);
        a.setRegistration(req.registration());
        a.setAircraftType(aircraftTypes.findById(req.aircraftTypeId())
                .orElseThrow(() -> ApiException.badRequest("Unknown aircraft type")));
        a.setSerialNumber(req.serialNumber());
        a.setStatus(req.status() == null ? "IN_SERVICE" : req.status());
        a.setTotalFlightHours(req.totalFlightHours());
        a.setTotalCycles(req.totalCycles());
        a.setBaseStation(req.baseStation());
        a.setInServiceDate(req.inServiceDate());
        aircraft.save(a);
        audit.record(actor, id == null ? "CREATE" : "UPDATE", "Aircraft", a.getId(), before, aircraftView(a));
        return aircraftView(a);
    }

    public List<Dtos.ComponentView> listComponents() {
        return components.findAll().stream().map(this::componentView).toList();
    }

    @Transactional
    public Dtos.ComponentView saveComponent(Long id, Dtos.ComponentUpsert req, User actor) {
        Component c = id == null ? new Component() : components.findById(id)
                .orElseThrow(() -> ApiException.notFound("Component not found"));
        Object before = id == null ? null : componentView(c);
        c.setPartNumber(req.partNumber());
        c.setSerialNumber(req.serialNumber());
        c.setName(req.name());
        c.setCategory(req.category());
        c.setAircraft(req.aircraftId() == null ? null : aircraft.findById(req.aircraftId())
                .orElseThrow(() -> ApiException.badRequest("Unknown aircraft")));
        c.setStatus(req.status() == null ? "INSTALLED" : req.status());
        c.setLifeLimitHours(req.lifeLimitHours());
        c.setLifeLimitCycles(req.lifeLimitCycles());
        c.setAccumulatedHours(req.accumulatedHours() == null ? java.math.BigDecimal.ZERO : req.accumulatedHours());
        c.setAccumulatedCycles(req.accumulatedCycles() == null ? 0 : req.accumulatedCycles());
        components.save(c);
        audit.record(actor, id == null ? "CREATE" : "UPDATE", "Component", c.getId(), before, componentView(c));
        return componentView(c);
    }

    public List<Dtos.CheckTypeView> listCheckTypes() {
        return checkTypes.findAll().stream().map(this::checkView).toList();
    }

    @Transactional
    public Dtos.CheckTypeView saveCheckType(Long id, Dtos.CheckTypeUpsert req, User actor) {
        CheckType c = id == null ? new CheckType() : checkTypes.findById(id)
                .orElseThrow(() -> ApiException.notFound("Check type not found"));
        Object before = id == null ? null : checkView(c);
        c.setCode(req.code());
        c.setName(req.name());
        c.setDescription(req.description());
        c.setTypicalDowntimeHours(req.typicalDowntimeHours());
        checkTypes.save(c);
        audit.record(actor, id == null ? "CREATE" : "UPDATE", "CheckType", c.getId(), before, checkView(c));
        return checkView(c);
    }

    public List<Dtos.TaskView> listTasks() {
        return tasks.findAll().stream().map(this::taskView).toList();
    }

    @Transactional
    public Dtos.TaskView saveTask(Long id, Dtos.TaskUpsert req, User actor) {
        MaintenanceTask t = id == null ? new MaintenanceTask() : tasks.findById(id)
                .orElseThrow(() -> ApiException.notFound("Task not found"));
        Object before = id == null ? null : taskView(t);
        t.setTaskCard(req.taskCard());
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setCheckType(req.checkTypeId() == null ? null : checkTypes.findById(req.checkTypeId()).orElse(null));
        t.setAircraftType(req.aircraftTypeId() == null ? null : aircraftTypes.findById(req.aircraftTypeId()).orElse(null));
        t.setAtaChapter(req.ataChapter());
        t.setEstimatedHours(req.estimatedHours());
        t.setSkill(req.skill());
        tasks.save(t);
        audit.record(actor, id == null ? "CREATE" : "UPDATE", "MaintenanceTask", t.getId(), before, taskView(t));
        return taskView(t);
    }

    public List<Dtos.ScheduleView> listSchedules() {
        return schedules.findAll().stream().map(this::scheduleView).toList();
    }

    @Transactional
    public Dtos.ScheduleView saveSchedule(Long id, Dtos.ScheduleUpsert req, User actor) {
        RecurringSchedule s = id == null ? new RecurringSchedule() : schedules.findById(id)
                .orElseThrow(() -> ApiException.notFound("Schedule not found"));
        Object before = id == null ? null : scheduleView(s);
        s.setAircraft(aircraft.findById(req.aircraftId()).orElseThrow(() -> ApiException.badRequest("Unknown aircraft")));
        s.setTask(req.taskId() == null ? null : tasks.findById(req.taskId()).orElse(null));
        s.setCheckType(req.checkTypeId() == null ? null : checkTypes.findById(req.checkTypeId()).orElse(null));
        s.setIntervalHours(req.intervalHours());
        s.setIntervalDays(req.intervalDays());
        s.setIntervalCycles(req.intervalCycles());
        s.setNextDueAt(req.nextDueAt());
        s.setActive(req.active());
        schedules.save(s);
        audit.record(actor, id == null ? "CREATE" : "UPDATE", "RecurringSchedule", s.getId(), before, scheduleView(s));
        return scheduleView(s);
    }

    private Dtos.AircraftTypeView typeView(AircraftType t) {
        return new Dtos.AircraftTypeView(t.getId(), t.getIcaoCode(), t.getManufacturer(), t.getModel(), t.getDescription());
    }

    public Dtos.AircraftView aircraftView(Aircraft a) {
        return new Dtos.AircraftView(a.getId(), a.getRegistration(), a.getAircraftType().getId(),
                a.getAircraftType().getIcaoCode(), a.getSerialNumber(), a.getStatus(), a.getTotalFlightHours(),
                a.getTotalCycles(), a.getBaseStation(), a.getInServiceDate());
    }

    private Dtos.ComponentView componentView(Component c) {
        return new Dtos.ComponentView(c.getId(), c.getPartNumber(), c.getSerialNumber(), c.getName(), c.getCategory(),
                c.getAircraft() == null ? null : c.getAircraft().getId(),
                c.getAircraft() == null ? null : c.getAircraft().getRegistration(),
                c.getStatus(), c.getLifeLimitHours(), c.getLifeLimitCycles(), c.getAccumulatedHours(), c.getAccumulatedCycles());
    }

    private Dtos.CheckTypeView checkView(CheckType c) {
        return new Dtos.CheckTypeView(c.getId(), c.getCode(), c.getName(), c.getDescription(), c.getTypicalDowntimeHours());
    }

    private Dtos.TaskView taskView(MaintenanceTask t) {
        return new Dtos.TaskView(t.getId(), t.getTaskCard(), t.getTitle(), t.getDescription(),
                t.getCheckType() == null ? null : t.getCheckType().getId(),
                t.getCheckType() == null ? null : t.getCheckType().getCode(),
                t.getAircraftType() == null ? null : t.getAircraftType().getId(),
                t.getAircraftType() == null ? null : t.getAircraftType().getIcaoCode(),
                t.getAtaChapter(), t.getEstimatedHours(), t.getSkill());
    }

    private Dtos.ScheduleView scheduleView(RecurringSchedule s) {
        return new Dtos.ScheduleView(s.getId(), s.getAircraft().getId(), s.getAircraft().getRegistration(),
                s.getTask() == null ? null : s.getTask().getId(),
                s.getTask() == null ? null : s.getTask().getTaskCard(),
                s.getCheckType() == null ? null : s.getCheckType().getId(),
                s.getCheckType() == null ? null : s.getCheckType().getCode(),
                s.getIntervalHours(), s.getIntervalDays(), s.getIntervalCycles(), s.getLastPerformedAt(),
                s.getNextDueAt(), s.getNextDueHours(), s.getNextDueCycles(), s.isActive());
    }
}
