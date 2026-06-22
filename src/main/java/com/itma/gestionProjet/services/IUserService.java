package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.UserDTO;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.requests.MoRequest;
import com.itma.gestionProjet.requests.UserRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    Optional<User> findUserByEmail(String email);
    User saveUser(UserRequest p);

    User saveMo(MoRequest p);

    User saveConsultant(UserRequest p);

    Page<User> getUsersByProjectId(Long projectId, Pageable pageable);


    Optional<User> findById(Long id);
    User updateMo(MoRequest p,Long id);
    UserDTO getUser(Long id);
    List<User> getAllUsers();


List<User> getUsersByRoleName(String username);

    void saveUserVerificationToken(User theUser, String verificationToken);
    void deleteUser(User p);
    void deleteUserById(Long id);


    UserDTO convertEntityToDto(User p);
   User convertDtoToEntity(UserRequest p);

    String validateToken(String theToken);

    void createPasswordResetTokenForUser(User user, String passwordResetToken);

    String validatePasswordResetToken(String token);

    void deletePasswordResetToken(String token);

    User findUserByToken(String token);

    void changePassword(User user, String newPassword);

    boolean oldPasswordIsValid(User user, String oldPassword);

    User toggleEnabled(Long id);





   // UserDTO saveConsultant(ConsultantRequest p);
    User updateConsultant(Long id,UserRequest p);
}
