package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.PartieInteresse;
import com.itma.gestionProjet.entities.Tache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TacheRepository extends JpaRepository<Tache, Long>, JpaSpecificationExecutor<Tache> {

    @Query("SELECT t FROM Tache t JOIN t.utilisateurs u WHERE u.id = :userId AND t.project.id = :projectId")
    Page<Tache> findTachesByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId, Pageable pageable);

    Page<Tache> findByProjectId(Long projectId, Pageable pageable);

}
