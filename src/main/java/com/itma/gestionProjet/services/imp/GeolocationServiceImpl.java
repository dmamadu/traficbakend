package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.PointRepereDto;
import com.itma.gestionProjet.dtos.QuartierDto;
import com.itma.gestionProjet.entities.PointRepere;
import com.itma.gestionProjet.entities.Quartier;
import com.itma.gestionProjet.repositories.PointRepereRepository;
import com.itma.gestionProjet.repositories.QuartierRepository;
import com.itma.gestionProjet.requests.PointRepereRequest;
import com.itma.gestionProjet.requests.QuartierRequest;
import com.itma.gestionProjet.services.GeolocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GeolocationServiceImpl implements GeolocationService {

    @Autowired
    private QuartierRepository quartierRepository;

    @Autowired
    private PointRepereRepository pointRepereRepository;

    @Override
    public Page<QuartierDto> getAllQuartiers(Pageable pageable) {
        return quartierRepository.findByActifTrue(pageable).map(QuartierDto::from);
    }

    @Override
    public List<QuartierDto> getAllQuartiersActifs() {
        return quartierRepository.findByActifTrue().stream()
                .map(QuartierDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public QuartierDto getQuartierById(Long id) {
        Quartier quartier = quartierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quartier non trouvé avec l'id : " + id));
        return QuartierDto.from(quartier);
    }

    @Override
    @Transactional
    public QuartierDto createQuartier(QuartierRequest request) {
        if (quartierRepository.existsByNom(request.getNom())) {
            throw new RuntimeException("Un quartier avec ce nom existe déjà : " + request.getNom());
        }
        Quartier quartier = new Quartier();
        quartier.setNom(request.getNom());
        quartier.setLatitudeCentre(request.getLatitudeCentre());
        quartier.setLongitudeCentre(request.getLongitudeCentre());
        quartier.setActif(true);
        return QuartierDto.from(quartierRepository.save(quartier));
    }

    @Override
    @Transactional
    public QuartierDto updateQuartier(Long id, QuartierRequest request) {
        Quartier quartier = quartierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quartier non trouvé avec l'id : " + id));
        if (!quartier.getNom().equals(request.getNom()) && quartierRepository.existsByNom(request.getNom())) {
            throw new RuntimeException("Un quartier avec ce nom existe déjà : " + request.getNom());
        }
        quartier.setNom(request.getNom());
        quartier.setLatitudeCentre(request.getLatitudeCentre());
        quartier.setLongitudeCentre(request.getLongitudeCentre());
        return QuartierDto.from(quartierRepository.save(quartier));
    }

    @Override
    @Transactional
    public void deleteQuartier(Long id) {
        Quartier quartier = quartierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quartier non trouvé avec l'id : " + id));
        quartier.setActif(false);
        quartierRepository.save(quartier);
    }

    @Override
    public Page<PointRepereDto> getAllPoints(Pageable pageable) {
        return pointRepereRepository.findByActifTrue(pageable).map(PointRepereDto::from);
    }

    @Override
    public List<PointRepereDto> getPointsByQuartier(Long quartierId) {
        return pointRepereRepository.findByQuartierIdAndActifTrue(quartierId).stream()
                .map(PointRepereDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PointRepereDto> searchPoints(String query, Pageable pageable) {
        return pointRepereRepository.searchByNom(query, pageable).map(PointRepereDto::from);
    }

    @Override
    public List<PointRepereDto> getPointsProches(Double latitude, Double longitude, Double rayonKm) {
        return pointRepereRepository.findPointsProches(latitude, longitude, rayonKm).stream()
                .map(PointRepereDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PointRepereDto createPointRepere(PointRepereRequest request) {
        Quartier quartier = quartierRepository.findById(request.getQuartierId())
                .orElseThrow(() -> new RuntimeException("Quartier non trouvé avec l'id : " + request.getQuartierId()));
        PointRepere point = new PointRepere();
        point.setNom(request.getNom());
        point.setType(request.getType());
        point.setQuartier(quartier);
        point.setLatitude(request.getLatitude());
        point.setLongitude(request.getLongitude());
        point.setPhotoUrl(request.getPhotoUrl());
        point.setDescription(request.getDescription());
        point.setActif(true);
        return PointRepereDto.from(pointRepereRepository.save(point));
    }

    @Override
    @Transactional
    public PointRepereDto updatePointRepere(Long id, PointRepereRequest request) {
        PointRepere point = pointRepereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Point de repère non trouvé avec l'id : " + id));
        Quartier quartier = quartierRepository.findById(request.getQuartierId())
                .orElseThrow(() -> new RuntimeException("Quartier non trouvé avec l'id : " + request.getQuartierId()));
        point.setNom(request.getNom());
        point.setType(request.getType());
        point.setQuartier(quartier);
        point.setLatitude(request.getLatitude());
        point.setLongitude(request.getLongitude());
        point.setPhotoUrl(request.getPhotoUrl());
        point.setDescription(request.getDescription());
        return PointRepereDto.from(pointRepereRepository.save(point));
    }

    @Override
    @Transactional
    public void deletePointRepere(Long id) {
        PointRepere point = pointRepereRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Point de repère non trouvé avec l'id : " + id));
        point.setActif(false);
        pointRepereRepository.save(point);
    }
}
