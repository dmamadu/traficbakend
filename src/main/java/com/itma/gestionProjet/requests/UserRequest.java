package com.itma.gestionProjet.requests;

import com.itma.gestionProjet.entities.Image;
import lombok.*;
import java.util.List;
import javax.validation.constraints.*;
import java.util.regex.Pattern;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {

    private Long id;

    @NotBlank(message = "Le nom de famille ne peut pas être vide.")
    private String lastname;

    @NotBlank(message = "Le prénom ne peut pas être vide.")
    private String firstname;

    @NotBlank(message = "L'email ne peut pas être vide.")
    @Email(message = "L'email doit être valide.")
    private String email;

    // Facultatif côté API : d'autres appelants (parties intéressées…) ne l'envoient pas
    private String locality;

    @NotBlank(message = "Le contact ne peut pas être vide.")
    private String contact;

    private Long fonction_id;
    @NotNull(message = "Le categorie_id ne peut pas être nul.")
    private Long categorie_id;

    private String imageUrl;

    private  Long partieInteresse_id;

    @NotNull(message = "Le role_id ne peut pas être nul.")
    private Long role_id;

  // private  Long projectId;
    private  Long projectId;

    public Long projectId() {
        return projectId;
    }

    // private  String password;
}

