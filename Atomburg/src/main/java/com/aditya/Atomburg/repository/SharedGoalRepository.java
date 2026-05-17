package com.aditya.Atomburg.repository;

import com.aditya.Atomburg.persistence.entity.SharedGoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedGoalRepository extends JpaRepository<SharedGoalEntity, Long> {
    Optional<SharedGoalEntity> findByReferenceKey(String referenceKey);
}
