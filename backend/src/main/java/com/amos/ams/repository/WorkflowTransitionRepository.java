package com.amos.ams.repository;

import com.amos.ams.domain.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {
    List<WorkflowTransition> findByActivityIdOrderByCreatedAtAsc(Long activityId);
}
