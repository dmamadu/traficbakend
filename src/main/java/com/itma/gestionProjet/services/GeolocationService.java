package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.PointRepereDto;
import com.itma.gestionProjet.dtos.QuartierDto;
import com.itma.gestionProjet.requests.PointRepereRequest;
import com.itma.gestionProjet.requests.QuartierRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GeolocationService {

    Page<QuartierDto> getAllQuartiers(Pageable pageable);
    List<QuartierDto> getAllQuartiersActifs();
    QuartierDto getQuartierById(Long id);
    QuartierDto createQuartier(QuartierRequest request);
    QuartierDto updateQuartier(Long id, QuartierRequest request);
    void deleteQuartier(Long id);

    Page<PointRepereDto> getAllPoints(Pageable pageable);
    List<PointRepereDto> getPointsByQuartier(Long quartierId);
    Page<PointRepereDto> searchPoints(String query, Pageable pageable);
    List<PointRepereDto> getPointsProches(Double latitude, Double longitude, Double rayonKm);
    PointRepereDto createPointRepere(PointRepereRequest request);
    PointRepereDto updatePointRepere(Long id, PointRepereRequest request);
    void deletePointRepere(Long id);
}
