package com.itma.gestionProjet.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private Boolean enabled;
    @JsonIgnore
    private String password;
    private String locality;
    private String contact;
    private String imageUrl;
    private String sexe;

    // Relation ManyToOne avec Fonction
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fonction_id")
    private Fonction fonction;

    // Relation ManyToOne avec Categorie
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    // Relation ManyToMany avec Role
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private List<Role> roles;

    //@JsonIgnore
    // Relation ManyToMany avec Project
    /*
    @ManyToMany(mappedBy = "users", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Project> projects = new ArrayList<>();
     */


    @ManyToMany(mappedBy = "users", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Project> projects = new ArrayList<>();


    @Column(columnDefinition = "boolean default false")
    private Boolean mustChangePassword;

    // Relation ManyToOne avec PartieInteresse
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partie_interesse_id")
    private PartieInteresse partieInteresse;
}

