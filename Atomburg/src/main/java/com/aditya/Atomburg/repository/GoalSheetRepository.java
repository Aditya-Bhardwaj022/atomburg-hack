package com.aditya.Atomburg.repository;

import com.aditya.Atomburg.persistence.entity.GoalSheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoalSheetRepository extends JpaRepository<GoalSheetEntity, Long> {
    Optional<GoalSheetEntity> findByEmployee_IdAndYear(String employeeId, Integer year);
}
