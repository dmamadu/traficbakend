package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.EntenteCompensationPap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntenteCompensationPapRepository extends JpaRepository<EntenteCompensationPap, Long> {
    Page<EntenteCompensationPap> findByProjectId(Long projectId, Pageable pageable);
    Optional<EntenteCompensationPap> findByCodePap(String codePap);
    Optional<EntenteCompensationPap> findFirstByCodePapAndProjectIdOrderByIdDesc(String codePap, Long projectId);
    boolean existsByCodePap(String codePap);


    @Query("SELECT e FROM EntenteCompensationPap e WHERE " +
            "(:projectId IS NULL OR e.project.id = :projectId) AND " +
            "LOWER(e.nom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.prenom) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.codePap) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.commune) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.departement) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.categoriePap) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.sexe) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.typePni) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(e.numeroPni) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
    )
    Page<EntenteCompensationPap> searchGlobal(@Param("searchTerm") String searchTerm,@Param("projectId") Optional<Long> projectId, Pageable pageable);

}
