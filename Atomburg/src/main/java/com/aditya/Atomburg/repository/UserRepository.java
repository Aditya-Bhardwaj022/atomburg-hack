package com.aditya.Atomburg.repository;

import com.aditya.Atomburg.domain.PortalDomain.Role;
import com.aditya.Atomburg.persistence.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<AppUserEntity, String> {
    List<AppUserEntity> findByRoleOrderByName(Role role);

    List<AppUserEntity> findByManager_IdAndRoleOrderByName(String managerId, Role role);
}
