package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.PointRepereDto;
import com.itma.gestionProjet.dtos.QuartierDto;
import com.itma.gestionProjet.requests.PointRepereRequest;
import com.itma.gestionProjet.requests.QuartierRequest;
import com.itma.gestionProjet.services.GeolocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/geolocation")
public class GeolocationController {

    @Autowired
    private GeolocationService geolocationService;

    // ─── QUARTIERS ────────────────────────────────────────────────────────────

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_QUARTIERS_VOIR')")
    @GetMapping("/quartiers")
    public ResponseEntity<AApiResponse<QuartierDto>> getAllQuartiers(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int max) {
        Pageable pageable = PageRequest.of(offset, max);
        Page<QuartierDto> page = geolocationService.getAllQuartiers(pageable);
        AApiResponse<QuartierDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Liste des quartiers récupérée avec succès");
        response.setData(page.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(page.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_QUARTIERS_VOIR')")
    @GetMapping("/quartiers/all")
    public ResponseEntity<AApiResponse<QuartierDto>> getAllQuartiersActifs() {
        List<QuartierDto> quartiers = geolocationService.getAllQuartiersActifs();
        AApiResponse<QuartierDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Quartiers actifs récupérés avec succès");
        response.setData(quartiers);
        response.setLength(quartiers.size());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_QUARTIERS_VOIR')")
    @GetMapping("/quartiers/{id}")
    public ResponseEntity<AApiResponse<QuartierDto>> getQuartierById(@PathVariable Long id) {
        QuartierDto quartier = geolocationService.getQuartierById(id);
        AApiResponse<QuartierDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Quartier trouvé");
        response.setData(Collections.singletonList(quartier));
        response.setLength(1);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_QUARTIERS_CREER')")
    @PostMapping("/quartiers")
    public ResponseEntity<AApiResponse<QuartierDto>> createQuartier(@RequestBody QuartierRequest request) {
        QuartierDto created = geolocationService.createQuartier(request);
        AApiResponse<QuartierDto> response = new AApiResponse<>();
        response.setResponseCode(201);
        response.setMessage("Quartier créé avec succès");
        response.setData(Collections.singletonList(created));
        response.setLength(1);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_QUARTIERS_MODIFIER')")
    @PutMapping("/quartiers/{id}")
    public ResponseEntity<AApiResponse<QuartierDto>> updateQuartier(
            @PathVariable Long id,
            @RequestBody QuartierRequest request) {
        QuartierDto updated = geolocationService.updateQuartier(id, request);
        AApiResponse<QuartierDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Quartier mis à jour avec succès");
        response.setData(Collections.singletonList(updated));
        response.setLength(1);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_QUARTIERS_SUPPRIMER')")
    @DeleteMapping("/quartiers/{id}")
    public ResponseEntity<AApiResponse<Void>> deleteQuartier(@PathVariable Long id) {
        geolocationService.deleteQuartier(id);
        AApiResponse<Void> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Quartier supprimé avec succès");
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_QUARTIERS_VOIR')")
    @GetMapping("/quartiers/{id}/points")
    public ResponseEntity<AApiResponse<PointRepereDto>> getPointsByQuartier(@PathVariable Long id) {
        List<PointRepereDto> points = geolocationService.getPointsByQuartier(id);
        AApiResponse<PointRepereDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Points du quartier récupérés avec succès");
        response.setData(points);
        response.setLength(points.size());
        return ResponseEntity.ok(response);
    }

    // ─── POINTS DE REPÈRE ─────────────────────────────────────────────────────

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_POINTS_VOIR')")
    @GetMapping("/points")
    public ResponseEntity<AApiResponse<PointRepereDto>> getAllPoints(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int max) {
        Pageable pageable = PageRequest.of(offset, max);
        Page<PointRepereDto> page = geolocationService.getAllPoints(pageable);
        AApiResponse<PointRepereDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Liste des points de repère récupérée avec succès");
        response.setData(page.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(page.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_POINTS_VOIR')")
    @GetMapping("/points/search")
    public ResponseEntity<AApiResponse<PointRepereDto>> searchPoints(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int max) {
        Pageable pageable = PageRequest.of(offset, max);
        Page<PointRepereDto> page = geolocationService.searchPoints(q, pageable);
        AApiResponse<PointRepereDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Résultats de recherche récupérés avec succès");
        response.setData(page.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(page.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_POINTS_VOIR')")
    @GetMapping("/points/proches")
    public ResponseEntity<AApiResponse<PointRepereDto>> getPointsProches(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "2.0") Double rayonKm) {
        List<PointRepereDto> points = geolocationService.getPointsProches(latitude, longitude, rayonKm);
        AApiResponse<PointRepereDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Points proches récupérés avec succès");
        response.setData(points);
        response.setLength(points.size());
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_POINTS_CREER')")
    @PostMapping("/points")
    public ResponseEntity<AApiResponse<PointRepereDto>> createPointRepere(@RequestBody PointRepereRequest request) {
        PointRepereDto created = geolocationService.createPointRepere(request);
        AApiResponse<PointRepereDto> response = new AApiResponse<>();
        response.setResponseCode(201);
        response.setMessage("Point de repère créé avec succès");
        response.setData(Collections.singletonList(created));
        response.setLength(1);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_POINTS_MODIFIER')")
    @PutMapping("/points/{id}")
    public ResponseEntity<AApiResponse<PointRepereDto>> updatePointRepere(
            @PathVariable Long id,
            @RequestBody PointRepereRequest request) {
        PointRepereDto updated = geolocationService.updatePointRepere(id, request);
        AApiResponse<PointRepereDto> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Point de repère mis à jour avec succès");
        response.setData(Collections.singletonList(updated));
        response.setLength(1);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("@permissionChecker.has('GEOLOCALISATION_POINTS_SUPPRIMER')")
    @DeleteMapping("/points/{id}")
    public ResponseEntity<AApiResponse<Void>> deletePointRepere(@PathVariable Long id) {
        geolocationService.deletePointRepere(id);
        AApiResponse<Void> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Point de repère supprimé avec succès");
        return ResponseEntity.ok(response);
    }
}
