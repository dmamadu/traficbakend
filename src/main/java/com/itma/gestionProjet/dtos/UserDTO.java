package com.itma.gestionProjet.dtos;

import com.itma.gestionProjet.entities.Categorie;
import com.itma.gestionProjet.entities.Image;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class UserDTO {
    private Long id;
    private String lastname;
    private String firstname;
    private String email;
    private  String contact;
    private  String locality;
    private Boolean enabled;
    private  String imageUrl;
    private List<RoleDTO> role;
    private Categorie categorie;
    private DtoFonction fonction;
   // private  List<ProjectDTOUSER> projects;
   private Set<ProjectDTO> projects;
   private Boolean mustChangePassword;
}
