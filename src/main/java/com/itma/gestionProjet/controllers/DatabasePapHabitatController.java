package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.DatabasePapHabitatRequestDTO;
import com.itma.gestionProjet.dtos.DatabasePapHabitatResponseDTO;
import com.itma.gestionProjet.services.DatabasePapHabitatService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/databasePapHabitat")
public class DatabasePapHabitatController {

    @Autowired
    private DatabasePapHabitatService service;

    @PostMapping
    public ResponseEntity<AApiResponse<Object>> create(@RequestBody List<DatabasePapHabitatRequestDTO> requestDTOs) {
        try {
            service.create(requestDTOs);
            AApiResponse<Object> success = new AApiResponse<>();
            success.setResponseCode(201);
            success.setMessage("Entities created successfully");
            success.setData(List.of("OK"));
            return ResponseEntity.status(HttpStatus.CREATED).body(success);
        } catch (Exception e) {
            AApiResponse<Object> error = new AApiResponse<>();
            error.setResponseCode(500);
            error.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping
    public AApiResponse<DatabasePapHabitatResponseDTO> getAll(@RequestParam(required = false) Long projectId,
                                                              @RequestParam(defaultValue = "0") int offset,
                                                              @RequestParam(defaultValue = "10") int max) {
        List<DatabasePapHabitatResponseDTO> data;
        long totalCount;
        if (projectId != null) {
            data = service.getByProjectId(projectId, offset, max);
            totalCount = service.getTotalCountByProjectId(projectId);
        } else {
            data = service.getAll(offset, max);
            totalCount = service.getTotalCount();
        }
        AApiResponse<DatabasePapHabitatResponseDTO> resp = new AApiResponse<>();
        resp.setResponseCode(200);
        resp.setData(data);
        resp.setOffset(offset);
        resp.setMax(max);
        resp.setLength(totalCount);
        resp.setMessage("Success");
        return resp;
    }

    @GetMapping("/search")
    public AApiResponse<DatabasePapHabitatResponseDTO> searchGlobal(@RequestParam String searchTerm,
                                                                    @RequestParam(required = false) Long projectId,
                                                                    @RequestParam(defaultValue = "0") int offset,
                                                                    @RequestParam(defaultValue = "100") int max) {
        List<DatabasePapHabitatResponseDTO> data = service.searchGlobal(searchTerm, Optional.ofNullable(projectId), offset, max);
        long totalCount = service.getTotalCountForSearch(searchTerm, Optional.ofNullable(projectId));
        AApiResponse<DatabasePapHabitatResponseDTO> resp = new AApiResponse<>();
        resp.setResponseCode(200);
        resp.setData(data);
        resp.setOffset(offset);
        resp.setMax(max);
        resp.setLength(totalCount);
        resp.setMessage("Success");
        return resp;
    }

    @GetMapping("/vulnerability-stats")
    public ResponseEntity<Map<String, Object>> getVulnerabilityStats(@RequestParam(required = false) Long projectId) {
        Map<String, Object> stats = service.getVulnerabilityStats(projectId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}")
    public DatabasePapHabitatResponseDTO getById(@PathVariable Long id) { return service.getById(id); }

    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<String>> update(@PathVariable Long id, @RequestBody DatabasePapHabitatRequestDTO dto) {
        try {
            service.update(id, dto);
            AApiResponse<String> r = new AApiResponse<>();
            r.setResponseCode(200);
            r.setMessage("Updated");
            r.setData(List.of("OK"));
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            AApiResponse<String> err = new AApiResponse<>();
            err.setResponseCode(500);
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @DeleteMapping("/{id}")
    public AApiResponse<String> delete(@PathVariable Long id) {
        service.delete(id);
        AApiResponse<String> r = new AApiResponse<>();
        r.setResponseCode(200);
        r.setMessage("Deleted");
        r.setData(List.of("OK"));
        return r;
    }

    @DeleteMapping("/project/{projectId}")
    public ResponseEntity<AApiResponse<String>> deleteAllByProjectId(@PathVariable Long projectId) {
        try {
            long countBefore = service.getTotalCountByProjectId(projectId);
            if (countBefore == 0) {
                AApiResponse<String> resp = new AApiResponse<>();
                resp.setResponseCode(200);
                resp.setMessage("No data to delete");
                resp.setData(List.of("0"));
                return ResponseEntity.ok(resp);
            }
            service.deleteAllByProjectId(projectId);
            AApiResponse<String> resp = new AApiResponse<>();
            resp.setResponseCode(200);
            resp.setMessage("Deleted");
            resp.setData(List.of("OK"));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            AApiResponse<String> err = new AApiResponse<>();
            err.setResponseCode(500);
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAnyAuthority('Super Admin', 'Admin')")
    public ResponseEntity<AApiResponse<String>> deleteAllByIds(@RequestBody List<Long> ids) {
        try {
            // Validation
            if (ids == null || ids.isEmpty()) {
                AApiResponse<String> err = new AApiResponse<>();
                err.setResponseCode(400);
                err.setMessage("IDs list cannot be null or empty");
                return ResponseEntity.badRequest().body(err);
            }

            service.deleteAllByIds(ids);

            AApiResponse<String> resp = new AApiResponse<>();
            resp.setResponseCode(200);
            resp.setMessage("Successfully deleted " + ids.size() + " records");
            resp.setData(List.of("OK"));
            return ResponseEntity.ok(resp);

        } catch (EntityNotFoundException e) {
            AApiResponse<String> err = new AApiResponse<>();
            err.setResponseCode(404);
            err.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);

        } catch (IllegalArgumentException e) {
            AApiResponse<String> err = new AApiResponse<>();
            err.setResponseCode(400);
            err.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(err);

        } catch (Exception e) {
            AApiResponse<String> err = new AApiResponse<>();
            err.setResponseCode(500);
            err.setMessage("Error deleting records: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
