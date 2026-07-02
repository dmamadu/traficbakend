package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.entities.AuditEntry;

public interface AuditService {
    AApiResponse<AuditEntry> search(String typeAction, String utilisateur, int offset, int max);
}
