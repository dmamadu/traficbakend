package com.itma.gestionProjet.security;

import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.entities.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.itma.gestionProjet.entities.Project;

@Data
public class UserRegistrationDetails implements UserDetails {

    private String userName;
    private String password;
    private boolean isEnabled;
    private List<GrantedAuthority> authorities;
    private User user;
    private List<Long> projectIds;
    private List<String> roleNames;

    public UserRegistrationDetails(User user) {
        this.userName = user.getEmail();
        this.user = user;
        this.password = user.getPassword();
        this.isEnabled = user.getEnabled();
        this.authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
        // Chargé pendant la transaction active de loadUserByUsername
        this.projectIds = user.getProjects().stream()
                .map(Project::getId)
                .collect(Collectors.toList());
        this.roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Le compte n'expire jamais
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Le compte n'est jamais verrouillé
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Les identifiants n'expirent jamais
    }

    @Override
    public boolean isEnabled() {
        return isEnabled; // Statut d'activation du compte
    }
}