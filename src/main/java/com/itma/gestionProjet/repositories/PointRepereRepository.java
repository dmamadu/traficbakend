package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.PointRepere;
import com.itma.gestionProjet.entities.PointRepereType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointRepereRepository extends JpaRepository<PointRepere, Long> {

    List<PointRepere> findByQuartierIdAndActifTrue(Long quartierId);
    Page<PointRepere> findByActifTrue(Pageable pageable);
    List<PointRepere> findByTypeAndActifTrue(PointRepereType type);

    @Query("SELECT p FROM PointRepere p WHERE LOWER(p.nom) LIKE LOWER(CONCAT('%', :query, '%')) AND p.actif = true")
    Page<PointRepere> searchByNom(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT p FROM PointRepere p
        WHERE p.actif = true
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude))
        * cos(radians(p.longitude) - radians(:lng))
        + sin(radians(:lat)) * sin(radians(p.latitude)))) < :rayonKm
        """)
    List<PointRepere> findPointsProches(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("rayonKm") Double rayonKm
    );
}
