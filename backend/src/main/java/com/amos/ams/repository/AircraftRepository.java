package com.amos.ams.repository;

import com.amos.ams.domain.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {
    Optional<Aircraft> findByRegistration(String registration);
}
