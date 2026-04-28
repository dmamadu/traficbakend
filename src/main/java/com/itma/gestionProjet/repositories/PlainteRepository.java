package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.Plainte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlainteRepository extends JpaRepository<Plainte, Long> {

    boolean existsByNumeroReference(String numeroReference);

    Page<Plainte> findByProjectId(Long projectId, Pageable pageable);

    Page<Plainte> findByCodePap(String codePap, Pageable pageable);

    Optional<List<Plainte>> findByCodePap(String codePap);

    List<Plainte> findByProjectIdAndStatut(Long projectId, String statut);

    long countByProjectId(Long projectId);

    @Query("SELECT p FROM Plainte p WHERE " +
            "(:projectId IS NULL OR p.projectId = :projectId) AND (" +
            "LOWER(p.nomPrenom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.codePap) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.numeroReference) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.telephone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.statut) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.objetPlainte) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Plainte> searchGlobal(@Param("searchTerm") String searchTerm,
                               @Param("projectId") Optional<Long> projectId,
                               Pageable pageable);
}