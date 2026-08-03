package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.Password.PasswordResetTokenService;
import com.itma.gestionProjet.dtos.*;
import com.itma.gestionProjet.entities.*;
import com.itma.gestionProjet.exceptions.*;
import com.itma.gestionProjet.exceptions.FonctionNotFoundException;
import com.itma.gestionProjet.repositories.*;
import com.itma.gestionProjet.requests.MoRequest;
import com.itma.gestionProjet.requests.UserRequest;
import com.itma.gestionProjet.utils.EmailService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.itma.gestionProjet.services.IUserService;
import com.itma.gestionProjet.services.UserProjectRoleService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService  implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
  private RoleRepository roleRepository;

   @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    private FonctionRepository fonctionRepository;

    @Autowired
    private PartieInteresseRepository partieInteresseRepository;
    @Autowired
    private CategorieRepository categorieRepository;


    @Autowired
    private EmailService emailService;


    @Autowired
    private  VerificationTokenRepository tokenRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private UserProjectRoleService userProjectRoleService;

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    @Autowired
    PasswordEncoder bCryptPasswordEncoder;

    @Override
    @Transactional
    public User saveUser(UserRequest p) {
        // Vérification de l'unicité de l'email et du contact
        Optional<User> optionalUser = userRepository.findByEmail(p.getEmail());
        if (optionalUser.isPresent()) {
            throw new EmailAlreadyExistsException("Email déjà existant!");
        }
        Optional<User> optionalContact = userRepository.findByContact(p.getContact());
        if (optionalContact.isPresent()) {
            throw new ContactMobileAlreadyExistsException("Contact déjà existant!");
        }
        // Création de l'utilisateur
        User newUser = new User();
        newUser.setEmail(p.getEmail());
        newUser.setLastname(p.getLastname());
        newUser.setFirstname(p.getFirstname());
        newUser.setContact(p.getContact());
      //  newUser.setLocality(p.getLocality());
        newUser.setImageUrl(p.getImageUrl());
        newUser.setEnabled(true);
        newUser.setMustChangePassword(true);
        // Génération du mot de passe encodé
        String rawPassword ="Passer@123"; // ou générer un mot de passe aléatoire si vous préférez
        String encodedPassword = bCryptPasswordEncoder.encode(rawPassword);
        newUser.setPassword(encodedPassword);

       // newUser.setPassword(bCryptPasswordEncoder.encode(p.getPassword()));
        // Assignation des rôles
        Role role = roleRepository.findById((p.getRole_id()))
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        newUser.setRoles(roles);

        // Assignation de la fonction et de la catégorie
        Fonction fonction = fonctionRepository.findById(p.getFonction_id())
                .orElseThrow(() -> new FonctionNotFoundException("Fonction not found"));
        Categorie categorie = categorieRepository.findById(p.getCategorie_id())
                .orElseThrow(() -> new CategorieNotFoundException("Categorie not found"));
        newUser.setFonction(fonction);
        newUser.setCategorie(categorie);
        // Assignation du projet (si project_id est présent dans la requête)
        if (p.projectId() != null) {
            Project project = projectRepository.findById(p.projectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Synchronise les deux côtés de la relation
            newUser.getProjects().add(project);
            project.getUsers().add(newUser);
        } else {
            newUser.setProjects(new ArrayList<>()); // Aucun projet associé
        }


        // Sauvegarde de l'utilisateur
       // return userRepository.save(newUser);

        // Sauvegarde de l'utilisateur
        User savedUser = userRepository.save(newUser);

        // Affecte le rôle au projet via UserProjectRole : sans cette ligne, resolveGrantedCodes()
        // ne trouve aucune affectation et l'utilisateur voit une sidebar vide malgré son rôle légal.
        if (p.projectId() != null) {
            userProjectRoleService.assign(savedUser.getId(), p.projectId(), role.getId());
        }

        auditRepository.save(new AuditEntry(
                "CREATION_UTILISATEUR",
                String.format("Création de l'utilisateur %s (rôle: %s)", savedUser.getEmail(), role.getName()),
                currentUser(),
                LocalDateTime.now()
        ));

        // Envoi de l'email avec le mot de passe
        try {
            MailRequestDto mailRequest = new MailRequestDto();
            mailRequest.setToEmail(savedUser.getEmail());
            mailRequest.setSubject("Votre compte a été créé");
            mailRequest.setMessage("Bonjour " + savedUser.getFirstname() + ",\n\n"
                    + "Votre compte a été créé avec succès.\n"
                    + "Voici vos informations de connexion :\n"
                    + "Email: " + savedUser.getEmail() + "\n"
                    + "Mot de passe par défaut: " + rawPassword + "\n\n"
                    + "Nous vous recommandons de changer votre mot de passe après votre première connexion.\n\n"
                    );
            mailRequest.setTemplate("replay_mail"); // Remplacez par le nom de votre template

            emailService.sendMail(mailRequest);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de création de compte", e);
            // Vous pouvez choisir de ne pas lever d'exception ici pour ne pas interrompre le processus
        }

        return savedUser;
    }


    @Transactional
    @Override
    public User saveMo(MoRequest p) {
        // Check if a user with the given email already exists
        if (userRepository.findByEmail(p.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email déjà existant!");
        }

        // Check if a user with the given contact number already exists
        if (userRepository.findByContact(p.getContact()).isPresent()) {
            throw new ContactMobileAlreadyExistsException("Ce numero téléphone est déjà utilisé");
        }

        // Create a new Mo
        User newUser = new User();
        newUser.setEmail(p.getEmail());
        newUser.setLastname(p.getLastname());
        newUser.setFirstname(p.getFirstname());
        newUser.setContact(p.getContact());
        newUser.setLocality(p.getLocality());
        newUser.setEnabled(true);
        newUser.setMustChangePassword(true);
        newUser.setPassword(bCryptPasswordEncoder.encode("Passer@123"));
        newUser.setImageUrl(p.getImageUrl());

        Role role = roleRepository.findRoleByName("Maitre d'ouvrage");
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        newUser.setRoles(roles);

        Categorie categorie = categorieRepository.findByLibelle("Niveau 1");
        newUser.setCategorie(categorie);
        if (p.getProjectId() != null) {
            Project project = projectRepository.findById(p.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Synchronise les deux côtés de la relation
            newUser.getProjects().add(project);
            project.getUsers().add(newUser);
        } else {
            newUser.setProjects(new ArrayList<>()); // Aucun projet associé
        }

        // Sauvegarde de l'utilisateur
        User savedUser = userRepository.save(newUser);

        // Affecte le rôle au projet via UserProjectRole (voir saveUser() ci-dessus pour le pourquoi).
        if (p.getProjectId() != null) {
            userProjectRoleService.assign(savedUser.getId(), p.getProjectId(), role.getId());
        }

        auditRepository.save(new AuditEntry(
                "CREATION_UTILISATEUR",
                String.format("Création du maître d'ouvrage %s", savedUser.getEmail()),
                currentUser(),
                LocalDateTime.now()
        ));

        // Envoi de l'email avec le mot de passe
        try {
            MailRequestDto mailRequest = getMailRequestDto(savedUser);

            emailService.sendMail(mailRequest);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de création de compte", e);
            // Vous pouvez choisir de ne pas lever d'exception ici pour ne pas interrompre le processus
        }

        return savedUser;

      //  return userRepository.save(newUser);
    }

    private static MailRequestDto getMailRequestDto(User savedUser) {
        MailRequestDto mailRequest = new MailRequestDto();
        mailRequest.setToEmail(savedUser.getEmail());
        mailRequest.setSubject("Votre compte a été créé");
        mailRequest.setMessage("Bonjour " + savedUser.getFirstname() + ",\n\n"
                + "Votre compte a été créé avec succès.\n"
                + "Voici vos informations de connexion :\n"
                + "Email: " + savedUser.getEmail() + "\n"
                + "Mot de passe: " + "Passer@123" + "\n\n"
                + "Nous vous recommandons de changer votre mot de passe après votre première connexion.\n\n"
                + "Cordialement,\nL'équipe de support");
        mailRequest.setTemplate("replay_mail"); // Remplacez par le nom de votre template
        return mailRequest;
    }


    @Transactional
    @Override
    public User saveConsultant(UserRequest p) {
        // Vérification de l'unicité avec une transaction pour garantir l'intégrité des données
        if (userRepository.findByEmail(p.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email déjà existant!");
        }

        if (userRepository.findByContact(p.getContact()).isPresent()) {
            throw new ContactMobileAlreadyExistsException("Ce numéro de téléphone est déjà utilisé");
        }

        // Création de l'utilisateur
        User newUser = new User();
        newUser.setEmail(p.getEmail());
        newUser.setLastname(p.getLastname());
        newUser.setFirstname(p.getFirstname());
        newUser.setContact(p.getContact());
       // newUser.setLocality(p.getLocality());
        newUser.setEnabled(true);
        newUser.setMustChangePassword(true);
        newUser.setPassword(bCryptPasswordEncoder.encode("Passer@123"));
        newUser.setImageUrl(p.getImageUrl());

        // Attribuer le rôle et la catégorie
        Role role = roleRepository.findRoleByName("Consultant");
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        newUser.setRoles(roles);

        Categorie categorie = categorieRepository.findByLibelle("Niveau 2");
        newUser.setCategorie(categorie);

        PartieInteresse partieInteresse = partieInteresseRepository.findById(p.getPartieInteresse_id())
                .orElseThrow(() -> new EntityNotFoundException("PartieInteresse introuvable avec l'ID " + p.getPartieInteresse_id()));
        newUser.setPartieInteresse(partieInteresse);
        if (p.getProjectId() != null) {
            Project project = projectRepository.findById(p.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            // Synchronise les deux côtés de la relation
            newUser.getProjects().add(project);
            project.getUsers().add(newUser);
        } else {
            newUser.setProjects(new ArrayList<>()); // Aucun projet associé
        }

        // Sauvegarde de l'utilisateur
        User savedUser = userRepository.save(newUser);

        // Affecte le rôle au projet via UserProjectRole (voir saveUser() ci-dessus pour le pourquoi).
        if (p.getProjectId() != null) {
            userProjectRoleService.assign(savedUser.getId(), p.getProjectId(), role.getId());
        }

        auditRepository.save(new AuditEntry(
                "CREATION_UTILISATEUR",
                String.format("Création du consultant %s", savedUser.getEmail()),
                currentUser(),
                LocalDateTime.now()
        ));

        // Envoi de l'email avec le mot de passe
        try {
            /*
            MailRequestDto mailRequest = new MailRequestDto();
            mailRequest.setToEmail(savedUser.getEmail());
            mailRequest.setSubject("Votre compte a été créé");
            mailRequest.setMessage("Bonjour " + savedUser.getFirstname() + ",\n\n"
                    + "Votre compte a été créé avec succès.\n"
                    + "Voici vos informations de connexion :\n"
                    + "Email: " + savedUser.getEmail() + "\n"
                    + "Mot de passe: " + "Passer@123" + "\n\n"
                    + "Nous vous recommandons de changer votre mot de passe après votre première connexion.\n\n"
                    + "Cordialement,\nL'équipe de support");
            mailRequest.setTemplate("email-template"); // Remplacez par le nom de votre template

             */
            MailRequestDto mailRequest = getMailRequestDto(savedUser);

            emailService.sendMail(mailRequest);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de création de compte", e);
            // Vous pouvez choisir de ne pas lever d'exception ici pour ne pas interrompre le processus
        }

        return savedUser;


        // Sauvegarder l'utilisateur
      //  return  userRepository.save(newUser);
    }



    @Override
    public Optional<User> findById(Long id) {
        try {
            Optional<User> user = userRepository.findById((long) Math.toIntExact(id));
            return user;

        } catch (Exception e) {
            // Gérer l'exception et lancer une nouvelle exception personnalisée
            throw new UsernameNotFoundException("Cet utilisateur n'est pas trouvé", e);
        }
    }

    @Override
    public User updateMo(MoRequest p,Long id) {

        User existingUser = userRepository.findById((long)(id))
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + p.getId()));

        // Check if the email is being changed and if the new email already exists
        // Vérification de l'email
        userRepository.findByEmail(p.getEmail()).ifPresent(user -> {
            if (!Objects.equals(user.getId(), existingUser.getId())) {
                throw new EmailAlreadyExistsException("Email déjà existant!");
            }

        });

        existingUser.setEmail(p.getEmail());
        existingUser.setLastname(p.getLastname());
        existingUser.setFirstname(p.getFirstname());
        existingUser.setContact(p.getContact());
      //  existingUser.setDate_of_birth(p.getDate_of_birth());
        existingUser.setLocality(p.getLocality());
        //existingUser.setPlace_of_birth(p.getPlace_of_birth()); // Assuming this should be 'getPlaceOfBirth'
        //existingUser.setEnabled(false);
       // existingUser.setPassword(bCryptPasswordEncoder.encode("Passer@123"));
        existingUser.setImageUrl(p.getImageUrl());
        User savedUser = userRepository.save(existingUser);
        auditRepository.save(new AuditEntry(
                "MODIFICATION_UTILISATEUR",
                String.format("Modification du maître d'ouvrage %s (id: %d)", savedUser.getEmail(), savedUser.getId()),
                currentUser(),
                LocalDateTime.now()
        ));
        return savedUser;

    }

    @Override
    public UserDTO getUser(Long id) {
        return null;
    }

    @Override
    public List<User> getAllUsers() {
        return  userRepository.findAll();
    }



    @Override
    public List<User> getUsersByRoleName(String roleName) {
        return new ArrayList<>(userRepository.findUsersByRoleName(roleName));
    }

    @Override
    public Page<User> getUsersByProjectId(Long projectId, Pageable pageable) {
        return userRepository.findByProjects_Id(projectId, pageable);
    }
    @Override
    public void deleteUser(User p) {
    }

    @Override
    @Transactional
    public void deleteUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));

        // Remove the user's associations in user_project
        userRepository.deleteUserAssociations(id);
/*
        // Remove the user's associations with roles and projects
        if (user.getImage() != null) {
            Image image = user.getImage();
            user.setImage(null);
            imageRepository.delete(image);
        }
        user.getRoles().clear();
        user.getProjects().clear();
        userRepository.save(user);

 */

        auditRepository.save(new AuditEntry(
                "SUPPRESSION_UTILISATEUR",
                String.format("Suppression de l'utilisateur %s (id: %d)", user.getEmail(), id),
                currentUser(),
                LocalDateTime.now()
        ));

        // Delete the user
        userRepository.deleteById((id));
    }

    @Override
    /*
    public UserDTO convertEntityToDto(User p) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return modelMapper.map(p, UserDTO.class);
    }

     */

    public UserDTO convertEntityToDto(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId((long) user.getId());
        userDTO.setLastname(user.getLastname());
        userDTO.setFirstname(user.getFirstname());
        userDTO.setEmail(user.getEmail());
        userDTO.setContact(user.getContact());
        userDTO.setLocality(user.getLocality());
        userDTO.setEnabled(user.getEnabled());
        userDTO.setImageUrl(user.getImageUrl());
      //  userDTO.setImage(user.getImage());
        userDTO.setCategorie(user.getCategorie());

        // Convert Fonction entity to DtoFonction
        DtoFonction dtoFonction = convertFonctionEntityToDto(user.getFonction());
        userDTO.setFonction(dtoFonction);

        // Convert roles from entity to DTO
        List<RoleDTO> roles = user.getRoles().stream()
                .map(this::convertRoleEntityToDto)
                .collect(Collectors.toList());
        userDTO.setRole(roles);

        // Convert projects from entity to DTO
        Set<ProjectDTO> projects = user.getProjects().stream()
                .map(this::convertProjectEntityToDto)
                .collect(Collectors.toSet());
        userDTO.setProjects(projects);
        userDTO.setMustChangePassword(user.getMustChangePassword());
        return userDTO;
    }


    @Override
    public User convertDtoToEntity(UserRequest userRequest) {
        User user = new User();
        user = modelMapper.map(userRequest, User.class);
        return user;
    }


    @Override
    public void saveUserVerificationToken(User theUser, String token) {
        var verificationToken = new VerificationToken(token, theUser);
        tokenRepository.save(verificationToken);
    }

    @Override
    public String validateToken(String theToken) {
        VerificationToken token = tokenRepository.findByToken(theToken);
        if(token == null){
            return "Invalid verification token";
        }
        User user = token.getUser();
        Calendar calendar = Calendar.getInstance();
        if ((token.getExpirationTime().getTime() - calendar.getTime().getTime()) <= 0){
            tokenRepository.delete(token);
            return "Token already expired";
        }
        user.setEnabled(true);
        userRepository.save(user);
        return "valid";
    }

    @Override
    public void createPasswordResetTokenForUser(User user, String passwordResetToken) {
        passwordResetTokenService.createPasswordResetTokenForUser(user, passwordResetToken);
    }

    @Override
    public String validatePasswordResetToken(String token) {
        return passwordResetTokenService.validatePasswordResetToken(token);
    }

    @Override
    public void deletePasswordResetToken(String token) {
        passwordResetTokenService.deletePasswordResetToken(token);
    }


    @Override
    public User findUserByToken(String token) {
        return passwordResetTokenService.findUserByPasswordToken(token).get();
    }

    @Override
    public void changePassword(User theUser, String newPassword) {
        theUser.setPassword(bCryptPasswordEncoder.encode(newPassword));
        userRepository.save(theUser);
        auditRepository.save(new AuditEntry(
                "REINITIALISATION_MOT_DE_PASSE",
                "Réinitialisation du mot de passe via lien de récupération",
                theUser.getEmail(),
                LocalDateTime.now()
        ));
    }

    @Override
    public boolean oldPasswordIsValid(User user, String oldPassword){
        return bCryptPasswordEncoder.matches(oldPassword, user.getPassword());
    }

    @Override
    public User toggleEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur non trouvé avec l'id " + id));
        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        User savedUser = userRepository.save(user);
        auditRepository.save(new AuditEntry(
                Boolean.TRUE.equals(savedUser.getEnabled()) ? "ACTIVATION_UTILISATEUR" : "DESACTIVATION_UTILISATEUR",
                String.format("Utilisateur %s %s", savedUser.getEmail(),
                        Boolean.TRUE.equals(savedUser.getEnabled()) ? "activé" : "désactivé"),
                currentUser(),
                LocalDateTime.now()
        ));
        return savedUser;
    }





    @Override
    public User updateConsultant(Long id, UserRequest p) {
        // Find the user by ID
        User existingUser = userRepository.findById((long)(id))
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + p.getId()));

        // Check if the email is being changed and if the new email already exists
        // Vérification de l'email
        userRepository.findByEmail(p.getEmail()).ifPresent(user -> {
            if (!Objects.equals(user.getId(), existingUser.getId())) {
                throw new EmailAlreadyExistsException("Email déjà existant!");
            }

        });

        // Vérification du numéro de contact
        userRepository.findByContact(p.getContact()).ifPresent(user -> {

            if (!Objects.equals(user.getId(), existingUser.getId())) {
                throw new EmailAlreadyExistsException("Email déjà existant!");
            }

        });

        // Update the user's information
        existingUser.setEmail(p.getEmail());
        existingUser.setLastname(p.getLastname());
        existingUser.setFirstname(p.getFirstname());
        existingUser.setContact(p.getContact());
      //  existingUser.setLocality(p.getLocality());
        existingUser.setImageUrl(p.getImageUrl());

        // Save the updated user
        User savedUser = userRepository.save(existingUser);
        auditRepository.save(new AuditEntry(
                "MODIFICATION_UTILISATEUR",
                String.format("Modification du consultant %s (id: %d)", savedUser.getEmail(), savedUser.getId()),
                currentUser(),
                LocalDateTime.now()
        ));
        return savedUser;
    }





    public RoleDTO convertRoleEntityToDto(Role role) {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setId((long) role.getId());
        roleDTO.setName(role.getName());
        return roleDTO;
    }

    public ProjectDTO convertProjectEntityToDto(Project project) {
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(String.valueOf(project.getId()));
        projectDTO.setLibelle(project.getLibelle());
        projectDTO.setDescription(project.getDescription());
        // Add any other necessary fields to be mapped
        return projectDTO;
    }

    public DtoFonction convertFonctionEntityToDto(Fonction fonction) {
        DtoFonction dtoFonction = new DtoFonction();

        if (fonction != null) {
            dtoFonction.setId(fonction.getId());
            dtoFonction.setLibelle(fonction.getLibelle()); 
        }

        return dtoFonction;
    }


    public User updateUser(Long userId, UserRequest p) {
        // Retrieve the user by ID
        Optional<User> optionalUser = (userRepository.findById(userId));

        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get();  // Get the User object safely after the check

        // Check if the new email is already in use, but skip if the email is the same as the current one
        if (!user.getEmail().equals(p.getEmail())) {
            // Look for a user with the new email
            Optional<User> optionalNewEmailUser = userRepository.findByEmail(p.getEmail());
            if (optionalNewEmailUser.isPresent()) {
                // If email exists, throw an exception
                throw new EmailAlreadyExistsException("Email déjà existant!");
            }
            // Set new email if no conflicts
            user.setEmail(p.getEmail());
        }

        // Update other fields without the need for 'orElseThrow' since we're directly modifying the current user
        user.setLastname(p.getLastname());
        user.setFirstname(p.getFirstname());
        user.setContact(p.getContact());
      //  user.setLocality(p.getLocality());
        user.setImageUrl(p.getImageUrl());

        // Role update (check if the role exists)
        Role role = roleRepository.findById((p.getRole_id())).orElse(null);
        if (role == null) {
            throw new RuntimeException("Role not found");
        }
        List<Role> roles = new ArrayList<>();
        roles.add(role);
        user.setRoles(roles);

        // Fonction update (check if the fonction exists)
        Fonction fonction = fonctionRepository.findById(p.getFonction_id()).orElse(null);
        if (fonction == null) {
            throw new RuntimeException("Fonction not found");
        }
        user.setFonction(fonction);

        // Categorie update (check if the categorie exists)
        Categorie categorie = categorieRepository.findById(p.getCategorie_id()).orElse(null);
        if (categorie == null) {
            throw new RuntimeException("Categorie not found");
        }
        user.setCategorie(categorie);

        // Save the updated user
        User savedUser = userRepository.save(user);

        // Synchronise le nouveau rôle vers UserProjectRole pour chaque projet de l'utilisateur :
        // resolveGrantedCodes() lit UserProjectRole (pas User.roles), donc sans cette synchro le
        // menu/les guards resteraient sur l'ancien rôle après un changement ici.
        for (Project project : savedUser.getProjects()) {
            userProjectRoleService.assign(savedUser.getId(), project.getId(), role.getId());
        }

        auditRepository.save(new AuditEntry(
                "MODIFICATION_UTILISATEUR",
                String.format("Modification de l'utilisateur %s (id: %d, rôle: %s)",
                        savedUser.getEmail(), savedUser.getId(), role.getName()),
                currentUser(),
                LocalDateTime.now()
        ));
        return savedUser;
    }

    public Page<User> getUsersByRoleNameAndProjectId(
            String roleName,
            Long projectId,
            Pageable pageable
    ) {
        //Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return userRepository.findUsersByRoleNameAndProjectId(roleName, projectId, pageable);
    }


    public boolean hasPermission(String email, List<String> requiredRoles) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        // Vérifier si l'utilisateur est activé
        if (!user.getEnabled()) {
            return false;
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .anyMatch(roleName -> requiredRoles.contains(roleName));
    }
}
