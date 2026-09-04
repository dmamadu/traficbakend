package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.TacheDTO;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.entities.Tache;
import com.itma.gestionProjet.exceptions.TacheNotFoundException;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.repositories.TacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TacheServiceImplTest {

    @Mock
    private TacheRepository tacheRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TacheServiceImpl tacheService;

    private Project project;
    private Tache tache;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);

        tache = new Tache();
        tache.setId(10L);
        tache.setLibelle("Étude d'impact");
        tache.setStatut("en-cours");
        tache.setProgression(20);
        tache.setProject(project);
        tache.setUtilisateurs(Collections.emptySet());
    }

    // ── Sécurité : une tâche d'un autre projet ne doit jamais être accessible ──

    @Test
    void getTacheById_rejects_whenTacheBelongsToAnotherProject() {
        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));

        assertThatThrownBy(() -> tacheService.getTacheById(10L, 999L))
                .isInstanceOf(TacheNotFoundException.class);
    }

    @Test
    void getTacheById_returnsTache_whenProjectMatches() {
        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));

        Tache result = tacheService.getTacheById(10L, 1L);

        assertThat(result).isSameAs(tache);
    }

    @Test
    void deleteTache_rejects_whenTacheBelongsToAnotherProject() {
        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));

        assertThatThrownBy(() -> tacheService.deleteTache(10L, 999L))
                .isInstanceOf(TacheNotFoundException.class);
    }

    // ── Validation progression ──

    @Test
    void createTache_rejects_progressionOutOfRange() {
        tache.setProgression(150);

        assertThatThrownBy(() -> tacheService.createTache(tache, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Validation statut ──

    @Test
    void createTache_rejects_invalidStatut() {
        tache.setStatut("statut-inconnu");

        assertThatThrownBy(() -> tacheService.createTache(tache, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createTache_accepts_validStatutAndProgression() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(tacheRepository.save(any(Tache.class))).thenAnswer(inv -> inv.getArgument(0));

        Tache result = tacheService.createTache(tache, 1L);

        assertThat(result.getProject()).isEqualTo(project);
    }

    // ── Progression à 100% force le statut "complete" ──

    @Test
    void updateTache_forcesStatutComplete_whenProgressionReaches100() {
        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));

        Tache updated = new Tache();
        updated.setLibelle("Étude d'impact");
        updated.setStatut("en-cours");
        updated.setProgression(100);

        TacheDTO result = tacheService.updateTache(10L, updated, 1L);

        assertThat(result.getStatut()).isEqualTo("complete");
        assertThat(result.getProgression()).isEqualTo(100);
    }
}
