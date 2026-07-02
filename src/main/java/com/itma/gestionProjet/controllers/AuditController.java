package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.entities.AuditEntry;
import com.itma.gestionProjet.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('Super Admin', 'Admin')")
    public ResponseEntity<AApiResponse<AuditEntry>> search(
            @RequestParam(required = false) String typeAction,
            @RequestParam(required = false) String utilisateur,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int max) {
        return ResponseEntity.ok(auditService.search(typeAction, utilisateur, offset, max));
    }
}
