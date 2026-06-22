package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.Quartier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuartierRepository extends JpaRepository<Quartier, Long> {

    Page<Quartier> findByActifTrue(Pageable pageable);
    List<Quartier> findByActifTrue();
    boolean existsByNom(String nom);
}
