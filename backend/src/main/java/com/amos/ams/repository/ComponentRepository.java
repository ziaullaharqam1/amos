package com.amos.ams.repository;

import com.amos.ams.domain.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComponentRepository extends JpaRepository<Component, Long> {
    List<Component> findByAircraftId(Long aircraftId);
}
