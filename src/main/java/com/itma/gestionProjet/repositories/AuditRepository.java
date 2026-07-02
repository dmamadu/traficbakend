package com.itma.gestionProjet.repositories;


import com.itma.gestionProjet.entities.AuditEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<AuditEntry, Long> {

    @Query("SELECT a FROM AuditEntry a WHERE " +
            "(:typeAction IS NULL OR a.typeAction = :typeAction) AND " +
            "(:utilisateur IS NULL OR LOWER(a.utilisateur) LIKE LOWER(CONCAT('%', :utilisateur, '%'))) " +
            "ORDER BY a.dateAction DESC")
    Page<AuditEntry> search(@Param("typeAction") String typeAction,
                             @Param("utilisateur") String utilisateur,
                             Pageable pageable);
}