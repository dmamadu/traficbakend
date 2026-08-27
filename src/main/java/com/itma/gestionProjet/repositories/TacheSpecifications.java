package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.Tache;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class TacheSpecifications {

    private TacheSpecifications() {
    }

    public static Specification<Tache> hasProjectId(Long projectId) {
        return (root, query, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Tache> hasStatut(String statut) {
        return (root, query, cb) -> StringUtils.hasText(statut) ? cb.equal(root.get("statut"), statut) : null;
    }

    public static Specification<Tache> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(search)) {
                return null;
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("libelle")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
