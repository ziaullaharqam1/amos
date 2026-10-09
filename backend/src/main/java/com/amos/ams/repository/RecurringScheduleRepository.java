package com.amos.ams.repository;

import com.amos.ams.domain.RecurringSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecurringScheduleRepository extends JpaRepository<RecurringSchedule, Long> {
    List<RecurringSchedule> findByActiveTrue();
    List<RecurringSchedule> findByAircraftId(Long aircraftId);
}
