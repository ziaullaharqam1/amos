package com.amos.ams.repository;

import com.amos.ams.domain.ActivityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityAssignmentRepository extends JpaRepository<ActivityAssignment, Long> {
    List<ActivityAssignment> findByActivityId(Long activityId);
}
