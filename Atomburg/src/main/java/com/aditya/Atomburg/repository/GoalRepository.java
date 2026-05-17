package com.aditya.Atomburg.repository;

import com.aditya.Atomburg.persistence.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalRepository extends JpaRepository<GoalEntity, Long> {
    List<GoalEntity> findBySharedGoal_ReferenceKey(String referenceKey);
}
