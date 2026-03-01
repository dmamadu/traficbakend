package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.*;
import com.itma.gestionProjet.entities.DatabasePapAgricole;
import com.itma.gestionProjet.entities.DatabasePapPlaceAffaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DatabasePapAgricoleService {
    List<DatabasePapAgricoleResponseDTO> getAllDatabasePapAgricole(int page, int size);

    List<DatabasePapAgricoleResponseDTO> getDatabasePapAgricoleByProjectId(Long projectId, int page, int size);
    DatabasePapAgricole getDatabasePapAgricoleById(Long id);
    void createDatabasePapAgricole(List<DatabasePapAgricoleRequestDTO> requestDTOs);
    void updateDatabasePapAgricole(Long id, DatabasePapAgricoleRequestDTO requestDTO);
    void deleteDatabasePapAgricole(Long id);

     long getTotalCount();

    DatabasePapAgricole getByCodePap(String codePap);

    long getTotalCountByProjectId(Long projectId);


    public Double calculateTotalPerte(Long projectId);


    // Nouvelle méthode pour la recherche globale

    List<DatabasePapAgricoleResponseDTO> searchGlobalDatabasePapAgricole(String searchTerm, Optional<Long> projectId, int page, int size);

    long getTotalCountForSearch(String searchTerm,Optional<Long> projectId);


    Map<String, Object> getVulnerabilityStats(Long projectId);

    void deleteAllByIds(List<Long> ids);

    void deleteAllByProjectId(Long projectId);

    CategoryStats getCategoryStats(Long projectId); // ← ajouter ça

}

