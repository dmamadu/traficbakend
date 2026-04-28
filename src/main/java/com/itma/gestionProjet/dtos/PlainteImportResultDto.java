package com.itma.gestionProjet.dtos;

import java.util.List;

public record PlainteImportResultDto(
        int totalLignes,
        int importees,
        int doublons,
        int erreurs,
        List<String> erreurDetails
) {}