package com.aditya.Atomburg.repository;

import com.aditya.Atomburg.persistence.entity.AuditEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEntryRepository extends JpaRepository<AuditEntryEntity, Long> {
    List<AuditEntryEntity> findAllByOrderByTimestampDesc();

    List<AuditEntryEntity> findByEmployeeIdOrderByTimestampDesc(String employeeId);

    List<AuditEntryEntity> findByYearOrderByTimestampDesc(Integer year);

    List<AuditEntryEntity> findByEmployeeIdAndYearOrderByTimestampDesc(String employeeId, Integer year);
}
