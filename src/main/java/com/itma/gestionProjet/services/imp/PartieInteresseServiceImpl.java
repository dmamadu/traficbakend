package com.itma.gestionProjet.services.imp;


import com.itma.gestionProjet.dtos.ContactDTO;
import com.itma.gestionProjet.dtos.DatabasePapAgricoleResponseDTO;
import com.itma.gestionProjet.dtos.PartieInteresseDTO;
import com.itma.gestionProjet.dtos.PartieInteresseResponseDTO;
import com.itma.gestionProjet.entities.*;
import com.itma.gestionProjet.exceptions.*;
import com.itma.gestionProjet.repositories.*;
import com.itma.gestionProjet.services.PartieInteresseService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PartieInteresseServiceImpl implements PartieInteresseService {

    @Autowired
    private PartieInteresseRepository repository;

    @Autowired
    PasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CategorieRepository categorieRepository;



    @Autowired
    private CategoriePartieInteresseRepository categoriePartieInteresseRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public List<PartieInteresse> findAll() {
        return repository.findAll();
    }

    @Override
    public Optional<PartieInteresse> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional
    public PartieInteresse save(PartieInteresseResponseDTO partieInteresse) {
        try {
            // Vérification existence par libellé
            repository.findByLibelle(partieInteresse.getLibelle())
                    .ifPresent(pip -> {
                        String errorMsg = String.format("Partie intéressée existe déjà - ID: %d, Libellé: %s",
                                pip.getId(), pip.getLibelle());
                        throw new PartieInteresseAlreadyExistsException(errorMsg);
                    });

            // Vérification email principal
            repository.findByCourrielPrincipal(partieInteresse.getCourrielPrincipal())
                    .ifPresent(pip -> {
                        String errorMsg = String.format("Email déjà utilisé - ID existant: %d, Email: %s",
                                pip.getId(), pip.getCourrielPrincipal());
                        throw new PartieInteresseAlreadyExistsException(errorMsg);
                    });

            // Vérification projet existe
            Long projectId = (long) partieInteresse.getProject_id();
            Project defaultProject = projectRepository.findById(projectId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            String.format("Projet ID %d non trouvé", projectId)));

            // Création entité
            PartieInteresse pip = createPartieInteresse(partieInteresse, defaultProject);

            // Gestion contacts
            List<User> users = createContacts(partieInteresse.getContacts(), pip);
            pip.setContacts(users);

            return repository.save(pip);

        } catch (PartieInteresseAlreadyExistsException | EntityNotFoundException e) {
            // On log les erreurs métier connues
           // logger.error("Erreur métier lors de la création: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // On log la stack trace complète pour les autres erreurs
            String errorMsg = "Erreur technique lors de la création";
           // logger.error("{} - Cause: {}", errorMsg, e.getMessage(), e);
            // On inclut la cause originale dans l'exception
            throw new PartieInteresseCreationException(
                    String.format("%s: %s", errorMsg, e.getMessage()),
                    e);
        }
    }

    private PartieInteresse createPartieInteresse(PartieInteresseResponseDTO dto, Project project) {
        PartieInteresse pip = new PartieInteresse();
        pip.setLibelle(dto.getLibelle());
        pip.setAdresse(dto.getAdresse());
        pip.setLocalisation(dto.getLocalisation());
        pip.setCourrielPrincipal(dto.getCourrielPrincipal());
        pip.setStatut(dto.getStatut());
//        pip.setNormes(dto.getNormes());
        pip.setCategorie(dto.getCategorie());
        pip.setProject(project);
        return pip;
    }

    private List<User> createContacts(List<ContactDTO> contacts, PartieInteresse pip) {
        return contacts.stream().map(contact -> {
            try {
                validateContactUniqueness(contact);
                return createUser(contact, pip);
            } catch (Exception e) {
                String errorMsg = String.format("Erreur création contact %s %s: %s",
                        contact.getPrenomContactPrincipal(),
                        contact.getNomContactPrincipal(),
                        e.getMessage());
                throw new PartieInteresseCreationException(errorMsg, e);
            }
        }).toList();
    }

    private void validateContactUniqueness(ContactDTO contact) {
        // Vérification email unique
        userRepository.findByEmail(contact.getEmailContactPrincipal())
                .ifPresent(user -> {
                    throw new EmailAlreadyExistsException("Email déjà utilisé: " + contact.getEmailContactPrincipal());
                });

        // Vérification téléphone unique
        userRepository.findByContact(contact.getTelephoneContactPrincipal())
                .ifPresent(user -> {
                    throw new ContactMobileAlreadyExistsException("Numéro de téléphone déjà utilisé: " + contact.getTelephoneContactPrincipal());
                });
    }

    private User createUser(ContactDTO contact, PartieInteresse pip) {
        User newUser = new User();
        newUser.setEmail(contact.getEmailContactPrincipal());
        newUser.setLastname(contact.getNomContactPrincipal());
        newUser.setFirstname(contact.getPrenomContactPrincipal());
        newUser.setContact(contact.getTelephoneContactPrincipal());
        newUser.setLocality(contact.getAdresseContactPrincipal());
        newUser.setSexe(contact.getSexeContactPrincipal());
        newUser.setEnabled(true);
        newUser.setPassword(bCryptPasswordEncoder.encode("P@sser123"));

        // Récupération rôle et catégorie avec vérification

        Role role = roleRepository.findRoleByName("Representant Principal");
        if (role == null) {
            throw new EntityNotFoundException("Rôle 'Representant Principal' non trouvé");
        }

        Categorie categorie = categorieRepository.findByLibelle("Niveau 2");
        if (categorie == null) {
            throw new EntityNotFoundException("Catégorie 'Niveau 2' non trouvée");
        }

        newUser.setRoles(List.of(role));
        newUser.setCategorie(categorie);
        newUser.setPartieInteresse(pip);

        return newUser;
    }



    /*
    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
*/

    @Override
    public Page<PartieInteresse> getPartieInteresses(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Page<PartieInteresse> getPartieInteressesByProjectId(Long projectId, Pageable pageable) {
        return repository.findByProjectId(projectId, pageable);
    }


    @Override
    public Page<PartieInteresse> getByCategorieLibelle(String categorie, Pageable pageable) {
        return repository.findByCategorie(categorie, pageable);
    }


    @Override
    public PartieInteresse update(Long id, PartieInteresseResponseDTO partieInteresseDTO) {
        Optional<PartieInteresse> optionalPip = repository.findById(id);
        if (!optionalPip.isPresent()) {
            throw new PartieInteresseNotFoundException("Partie intéressée avec ID " + id + " n'existe pas.");
        }
        PartieInteresse pip = optionalPip.get();

        // Mise à jour des informations de PartieInteresse (informations générales)
        pip.setLibelle(partieInteresseDTO.getLibelle());
        pip.setAdresse(partieInteresseDTO.getAdresse());
        pip.setLocalisation(partieInteresseDTO.getLocalisation());
        pip.setCourrielPrincipal(partieInteresseDTO.getCourrielPrincipal());
//        pip.setNormes(partieInteresseDTO.getNormes());
        pip.setStatut(partieInteresseDTO.getStatut());
        pip.setCategorie(partieInteresseDTO.getCategorie());


        // Gestion des contacts
        List<User> contacts = new ArrayList<>(); // Liste des contacts (représentants)

        // Si des contacts existent dans le DTO, les traiter
        if (partieInteresseDTO.getContacts() != null && !partieInteresseDTO.getContacts().isEmpty()) {
            for (ContactDTO contactDTO : partieInteresseDTO.getContacts()) {
                // Rechercher si un contact existe déjà (par exemple via l'email ou un autre identifiant unique)
                Optional<User> existingContactOpt = userRepository.findByEmail(contactDTO.getEmailContactPrincipal());
                User user;
                if (existingContactOpt.isPresent()) {
                    user = existingContactOpt.get();
                    user.setEmail(contactDTO.getEmailContactPrincipal());
                    user.setLastname(contactDTO.getNomContactPrincipal());
                    user.setFirstname(contactDTO.getPrenomContactPrincipal());
                    user.setContact(contactDTO.getTelephoneContactPrincipal());
                    user.setLocality(contactDTO.getAdresseContactPrincipal());
                    user.setSexe(contactDTO.getSexeContactPrincipal());
                    user.setEnabled(true);  // Activer le contact
                    user.setPassword(bCryptPasswordEncoder.encode("P@sser123")); // Nouveau mot de passe par défaut

                    // Mise à jour des rôles
                    Role role = roleRepository.findRoleByName("Representant principal");
                    List<Role> roles = new ArrayList<>();
                    roles.add(role);
                    user.setRoles(roles);
                    userRepository.save(user);
                } else {
                    // Si le contact n'existe pas, créer un nouveau contact
                    user = new User();
                    user.setEmail(contactDTO.getEmailContactPrincipal());
                    user.setLastname(contactDTO.getNomContactPrincipal());
                    user.setFirstname(contactDTO.getPrenomContactPrincipal());
                    user.setContact(contactDTO.getTelephoneContactPrincipal());
                    user.setLocality(contactDTO.getAdresseContactPrincipal());
                    user.setSexe(contactDTO.getSexeContactPrincipal());
                    user.setEnabled(true);
                    Role role = roleRepository.findRoleByName("Representant principal");
                    List<Role> roles = new ArrayList<>();
                    roles.add(role);
                    user.setRoles(roles);
                    userRepository.save(user);
                }
                contacts.add(user);
            }
        }
        pip.setContacts(contacts);
        return repository.save(pip);
    }




    @Override
    public void deleteById(Long id) {
        Optional<PartieInteresse> optionalPip = repository.findById(id);
        if (optionalPip.isPresent()) {
            PartieInteresse pip = optionalPip.get();
          //  User user = pip.getUser();

            repository.delete(pip); // Supprime la partie intéressée
        } else {
            throw new PartieInteresseNotFoundException("Partie intéressée non trouvée avec l'ID : " + id);
        }
    }



    public List<PartieInteresseResponseDTO> getAllPartieInteresseWithContacts() {
        // Récupérer toutes les instances de PartieInteresse avec leurs contacts
        List<PartieInteresse> partieInteresseList = repository.findAllWithContacts();

        // Mapper chaque PartieInteresse en DTO
        return partieInteresseList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Mapper PartieInteresse vers PartieInteresseResponseDTO

    private PartieInteresseResponseDTO convertToDTO(PartieInteresse partieInteresse) {
        PartieInteresseResponseDTO responseDTO = new PartieInteresseResponseDTO();
        responseDTO.setId(partieInteresse.getId());
        responseDTO.setAdresse(partieInteresse.getAdresse());
       responseDTO.setCourrielPrincipal(partieInteresse.getCourrielPrincipal());
        responseDTO.setLibelle(partieInteresse.getLibelle());
        responseDTO.setCategorie(partieInteresse.getCategorie());
        responseDTO.setLocalisation(partieInteresse.getLocalisation());
//        responseDTO.setNormes(partieInteresse.getNormes());
        responseDTO.setStatut(partieInteresse.getStatut());
        responseDTO.setProject_id(partieInteresse.getProject().getId());

        // Mapper les utilisateurs (contacts) vers ContactDTO
        List<ContactDTO> contactDTOs = partieInteresse.getContacts().stream()
                .map(this::toContactDTO)  // Utilisation de la méthode de mappage interne
                .collect(Collectors.toList());
        responseDTO.setContacts(contactDTOs);

        return responseDTO;
    }


    private ContactDTO toContactDTO(User user) {
        if (user == null) {
            return null;
        }

        ContactDTO contactDTO = new ContactDTO();
      //  contactDTO.setId(user.getId());
        contactDTO.setNomContactPrincipal(user.getFirstname());
        contactDTO.setPrenomContactPrincipal(user.getLastname());
        contactDTO.setEmailContactPrincipal(user.getEmail());
        //contactDTO.setLocality(user.getLocality());
        //contactDTO.setContact(user.getContact());
        //contactDTO.setImageUrl(user.getImageUrl());
        contactDTO.setSexeContactPrincipal(user.getSexe());
        // Autres propriétés du User si nécessaire

        return contactDTO;
    }

}

