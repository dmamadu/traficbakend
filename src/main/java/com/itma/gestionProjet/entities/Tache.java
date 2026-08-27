package com.itma.gestionProjet.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "taches")
public class Tache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String libelle;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private Date dateDebut;

    private Date dateFin;

    private String statut;

    private Integer progression = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "tache_user",
            joinColumns = @JoinColumn(name = "tache_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> utilisateurs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String observation;

}
