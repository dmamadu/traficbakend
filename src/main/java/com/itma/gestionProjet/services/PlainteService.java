package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.PlainteDto;
import com.itma.gestionProjet.dtos.PlainteImportResultDto;
import com.itma.gestionProjet.requests.PlainteRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface PlainteService {
    PlainteDto createPlainte(PlainteRequest plainteRequest);
    Page<PlainteDto> getAllPlaintes(Pageable pageable);
    Page<PlainteDto> getPlaintesByProjectId(Long projectId, Pageable pageable);
    Page<PlainteDto> getByProject(Long projectId, Pageable pageable);
    PlainteDto getPlainteById(Long id);
    PlainteDto updatePlainte(Long id, PlainteRequest plainteRequest);
    void deletePlainte(Long id);
    void deleteAllByIds(List<Long> ids);
    List<PlainteDto> createPlaintes(List<PlainteRequest> plainteRequests);
    AApiResponse<PlainteDto> getPlainteByCodePap(String codePap, int page, int size);
    AApiResponse<PlainteDto> searchGlobalPlaintes(String searchTerm, Optional<Long> projectId, int page, int size);
    PlainteImportResultDto importerDepuisExcel(MultipartFile file, Long projectId);
}