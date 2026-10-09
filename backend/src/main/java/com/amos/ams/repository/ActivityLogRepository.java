package com.amos.ams.repository;

import com.amos.ams.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByActivityIdOrderByCreatedAtDesc(Long activityId);
}
