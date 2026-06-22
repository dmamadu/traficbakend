package com.itma.gestionProjet.repositories;


import com.itma.gestionProjet.entities.Entente;
import com.itma.gestionProjet.entities.EtatProcessusEntente;
import com.itma.gestionProjet.entities.StatutEntente;
import com.itma.gestionProjet.entities.TypePap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntenteRepository extends JpaRepository<Entente, Long> {
//    List<Entente> findByProjectId(Long projectId);
    List<Entente> findByPapIdAndPapType(Long papId, TypePap papType);
    List<Entente> findByStatut(StatutEntente statut);
    List<Entente> findByEtatProcessus(String etatProcessus);

    Page<Entente> findByProjectId(Long projectId, Pageable pageable);
    Page<Entente> findAll(Pageable pageable);
    Optional<Entente> findByCodePap(String codePap);

    // Comptages par projet + statut (pour les KPIs mise en œuvre)
    long countByProjectIdAndStatut(Long projectId, StatutEntente statut);
    long countByStatut(StatutEntente statut);
    long countByProjectIdAndEtatProcessus(Long projectId, EtatProcessusEntente etatProcessus);
    long countByEtatProcessus(EtatProcessusEntente etatProcessus);
    long countByProjectIdAndEtatProcessusIn(Long projectId, Collection<EtatProcessusEntente> etats);
    long countByEtatProcessusIn(Collection<EtatProcessusEntente> etats);
}