package com.aditya.Atomburg.repository;

import com.aditya.Atomburg.persistence.entity.GoalCycleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalCycleRepository extends JpaRepository<GoalCycleEntity, Integer> {
}
