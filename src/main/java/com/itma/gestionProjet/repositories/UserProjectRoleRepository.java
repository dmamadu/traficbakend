package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.UserProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProjectRoleRepository extends JpaRepository<UserProjectRole, Long> {

    List<UserProjectRole> findByUserId(Long userId);

    List<UserProjectRole> findByUserIdAndProjectId(Long userId, Long projectId);

    List<UserProjectRole> findByUserIdAndProjectIdIsNull(Long userId);

    Optional<UserProjectRole> findByUserIdAndProjectIdAndRoleId(Long userId, Long projectId, Long roleId);

    void deleteByUserIdAndProjectIdAndRoleId(Long userId, Long projectId, Long roleId);
}
