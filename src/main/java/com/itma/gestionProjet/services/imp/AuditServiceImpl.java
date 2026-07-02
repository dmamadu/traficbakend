package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.entities.AuditEntry;
import com.itma.gestionProjet.repositories.AuditRepository;
import com.itma.gestionProjet.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditServiceImpl implements AuditService {

    @Autowired
    private AuditRepository auditRepository;

    @Override
    public AApiResponse<AuditEntry> search(String typeAction, String utilisateur, int offset, int max) {
        Page<AuditEntry> page = auditRepository.search(typeAction, utilisateur, PageRequest.of(offset, max));
        AApiResponse<AuditEntry> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Journal d'audit récupéré avec succès");
        response.setData(page.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(page.getTotalElements());
        return response;
    }
}
