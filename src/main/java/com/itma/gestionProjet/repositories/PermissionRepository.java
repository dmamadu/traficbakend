package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
    boolean existsByCode(String code);
    List<Permission> findByModule(String module);
}
