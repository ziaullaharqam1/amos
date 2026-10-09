package com.amos.ams.repository;

import com.amos.ams.domain.MaintenanceActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface MaintenanceActivityRepository extends JpaRepository<MaintenanceActivity, Long> {
    List<MaintenanceActivity> findByAssignedToIdOrderByDueAtAsc(Long userId);
    List<MaintenanceActivity> findByState(String state);
    List<MaintenanceActivity> findByParentId(Long parentId);
    List<MaintenanceActivity> findByAircraftId(Long aircraftId);
    boolean existsByScheduleIdAndStateNotIn(Long scheduleId, Collection<String> states);

    @Query("SELECT a FROM MaintenanceActivity a WHERE a.dueAt < :now AND a.state NOT IN ('COMPLETED','VERIFIED','CANCELLED')")
    List<MaintenanceActivity> findOverdue(Instant now);

    long countByState(String state);

    @Query("SELECT COUNT(a) FROM MaintenanceActivity a WHERE a.dueAt < :now AND a.state NOT IN ('COMPLETED','VERIFIED','CANCELLED')")
    long countOverdue(Instant now);

    @Query("SELECT a.state, COUNT(a) FROM MaintenanceActivity a GROUP BY a.state")
    List<Object[]> countByStateGrouped();
}
