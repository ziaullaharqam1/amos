package com.amos.ams.repository;

import com.amos.ams.domain.MaintenanceTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceTaskRepository extends JpaRepository<MaintenanceTask, Long> {
}
