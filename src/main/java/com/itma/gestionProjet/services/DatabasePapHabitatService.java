package com.itma.gestionProjet.services;
import com.itma.gestionProjet.dtos.CategoryStats;
import com.itma.gestionProjet.dtos.DatabasePapHabitatRequestDTO;
import com.itma.gestionProjet.dtos.DatabasePapHabitatResponseDTO;


import java.util.List;
import java.util.Map;
import java.util.Optional;


public interface DatabasePapHabitatService {
    void create(List<DatabasePapHabitatRequestDTO> requestDTOs);
    List<DatabasePapHabitatResponseDTO> getAll(int page, int size);
    List<DatabasePapHabitatResponseDTO> getByProjectId(Long projectId, int page, int size);
    DatabasePapHabitatResponseDTO getById(Long id);
    void update(Long id, DatabasePapHabitatRequestDTO requestDTO);
    void delete(Long id);
    long getTotalCount();
    DatabasePapHabitatResponseDTO getByCodePap(String codePap);
    long getTotalCountByProjectId(Long projectId);
    Double calculateTotalPerte(Long projectId);
    List<DatabasePapHabitatResponseDTO> searchGlobal(String searchTerm, Optional<Long> projectId, int page, int size);
    long getTotalCountForSearch(String searchTerm, Optional<Long> projectId);
    Map<String, Object> getVulnerabilityStats(Long projectId);
    void deleteAllByProjectId(Long projectId);
    void deleteAllByIds(List<Long> ids);

    // Méthode utilitaire pour vérifier l'existence
    boolean existAllByIds(List<Long> ids);

    CategoryStats getCategoryStats(Long projectId);
}
