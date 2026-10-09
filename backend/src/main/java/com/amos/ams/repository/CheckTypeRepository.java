package com.amos.ams.repository;

import com.amos.ams.domain.CheckType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CheckTypeRepository extends JpaRepository<CheckType, Long> {
    Optional<CheckType> findByCode(String code);
}
