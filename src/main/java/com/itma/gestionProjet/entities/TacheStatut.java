package com.itma.gestionProjet.entities;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Valeurs autorisées pour le champ "statut" d'une {@link Tache}.
 * Le champ reste stocké/échangé en String (compatibilité avec le front existant) ;
 * cet enum sert uniquement de whitelist de validation côté serveur.
 */
public enum TacheStatut {
    EN_ATTENTE("en-attente"),
    EN_COURS("en-cours"),
    APPROUVE("approuve"),
    COMPLETE("complete");

    private final String value;

    TacheStatut(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isValid(String value) {
        return Arrays.stream(values()).anyMatch(s -> s.value.equals(value));
    }

    public static Set<String> allowedValues() {
        return Arrays.stream(values()).map(s -> s.value).collect(Collectors.toSet());
    }
}
