package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.dtos.UserDTO;
import com.itma.gestionProjet.entities.User;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    // Recherche insensible à la casse : « Jean@… » et « jean@… » désignent le même compte
    Optional<User> findFirstByEmailIgnoreCase(String email);
    Optional<User> findByContact(String contact);


    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findUsersByRoleName(@Param("roleName") String roleName);


    Optional<User> findById(Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM user_project WHERE user_id = :userId", nativeQuery = true)
    void deleteUserAssociations(@Param("userId") Long userId);

    // Lignes qui référencent l'utilisateur et bloquent sa suppression (clés étrangères)
    @Modifying
    @Query(value = "DELETE FROM user_role WHERE user_id = :userId", nativeQuery = true)
    void deleteUserRoles(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM tache_user WHERE user_id = :userId", nativeQuery = true)
    void deleteUserTaches(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserProjectRole r WHERE r.user.id = :userId")
    void deleteUserProjectRoles(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM VerificationToken t WHERE t.user.id = :userId")
    void deleteUserVerificationTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user.id = :userId")
    void deleteUserPasswordResetTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Image i WHERE i.user.id = :userId")
    void deleteUserImages(@Param("userId") Long userId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN u.projects p WHERE p.id = :projectId")
    Page<User> findByProjects_Id(Long projectId, Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.roles r " +
            "JOIN u.projects p " +
            "WHERE r.name = :roleName " +
            "AND p.id = :projectId")
    Page<User> findUsersByRoleNameAndProjectId(
            @Param("roleName") String roleName,
            @Param("projectId") Long projectId,
            Pageable pageable
    );
}