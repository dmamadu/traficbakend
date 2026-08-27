package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.TacheDTO;
import com.itma.gestionProjet.entities.PartieInteresse;
import com.itma.gestionProjet.entities.Tache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface ITacheService {
    Tache createTache(Tache tache,Long projectId);
    TacheDTO updateTache(Long id, Tache tache, Long projectId);
    void deleteTache(Long id, Long projectId);
    List<Tache> getAllTaches();
    Tache getTacheById(Long id, Long projectId);

    Page<TacheDTO> getAllTaches(PageRequest pageRequest, String search, String statut);

    Page<TacheDTO> getTachesByProjectId(Long projectId, PageRequest pageRequest, String search, String statut);

}
