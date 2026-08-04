package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.DatabasePapEconomique;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatabasePapEconomiqueRepository extends JpaRepository<DatabasePapEconomique, Long> {

    Optional<DatabasePapEconomique> findByCodePap(String codePap);

    Page<DatabasePapEconomique> findByProjectId(Long projectId, Pageable pageable);
}
