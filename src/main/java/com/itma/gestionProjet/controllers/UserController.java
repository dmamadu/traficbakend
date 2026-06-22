package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.Password.PasswordRequest;
import com.itma.gestionProjet.Password.PasswordRequestUtil;
import com.itma.gestionProjet.Password.PasswordResetUtil;
import com.itma.gestionProjet.dtos.*;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.entities.VerificationToken;
import com.itma.gestionProjet.events.RegistrationCompleteEvent;
import com.itma.gestionProjet.events.listenner.RegistrationCompleteEventListener;
import com.itma.gestionProjet.exceptions.EmailAlreadyExistsException;
import com.itma.gestionProjet.repositories.UserRepository;
import com.itma.gestionProjet.repositories.VerificationTokenRepository;
import com.itma.gestionProjet.requests.ChangePasswordRequest;
import com.itma.gestionProjet.requests.ConsultantRequest;
import com.itma.gestionProjet.requests.MoRequest;
import com.itma.gestionProjet.requests.UserRequest;
import com.itma.gestionProjet.security.JWTGenerator;
import com.itma.gestionProjet.services.imp.UserService;
import com.itma.gestionProjet.utils.EmailService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;



import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JWTGenerator jwtGenerator;
    @Autowired
    private AuthenticationManager authenticationManager;
    private  final ApplicationEventPublisher publisher;
    @Autowired
    private  RegistrationCompleteEventListener eventListener;

    @Autowired
    PasswordEncoder bCryptPasswordEncoder;


    @Autowired
    private  EmailService emailService;

    @Autowired
    private  HttpServletRequest servletRequest;
    private  final  VerificationTokenRepository tokenRepository;

    @Value("${app.front.url}")
    private String urlFront;

    public UserController(ApplicationEventPublisher publisher, VerificationTokenRepository tokenRepository) {
        this.publisher = publisher;
        this.tokenRepository = tokenRepository;
    }

    @RequestMapping(path = "/all", method = RequestMethod.GET)

    public ApiResponse<List<UserDTO>> getUsers() {
        List<User> users = userService.getAllUsers();
        return new ApiResponse<>(HttpStatus.OK.value(), "Liste des utilisateurs récupérée avec succès", users);
    }



    @RequestMapping("projects")
    @GetMapping
    public ResponseEntity<AApiResponse<User>> getUsers(
            @RequestParam Long projectId, // Plus de required = false
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max) {

        Pageable pageable = PageRequest.of(offset, max);
        Page<User> userPage = userService.getUsersByProjectId(projectId, pageable);

        AApiResponse<User> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Users filtered by project ID " + projectId);
        response.setData(userPage.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(userPage.getTotalElements());

        return ResponseEntity.ok(response);
    }



    @RequestMapping(path = "/createUser", method = RequestMethod.POST)
    public  ApiResponse<User> createUser(@Valid @RequestBody UserRequest userRequest, final HttpServletRequest request) {
        User user = userService.saveUser(userRequest);
        publisher.publishEvent(new RegistrationCompleteEvent(user, applicationUrl(request)));
        return   new ApiResponse<>(HttpStatus.OK.value(), "User cree avec succés",user);
    }

    @RequestMapping(path = "/updateUser/{userId}", method = RequestMethod.PUT)
    public ApiResponse<User> updateUser(@PathVariable Long userId, @RequestBody UserRequest userRequest, final HttpServletRequest request) {
        try {
            // Call the service layer to update the user
            User user = userService.updateUser(userId, userRequest);

            // Return the success response with updated user details
            return new ApiResponse<>(HttpStatus.OK.value(), "User mis à jour avec succès", user);
        } catch (EmailAlreadyExistsException e) {
            // Handle case where the email already exists
            return new ApiResponse<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null);
        } catch (Exception e) {
            // Handle any other unexpected exceptions
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Une erreur est survenue", null);
        }
    }



    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById((id));
        User user1 = user.get();
        UserDTO userDTO = userService.convertEntityToDto(user1);
        if (user.isPresent()) {
            return new ApiResponse<>(200,"utilisateur",    userDTO);
        } else {
            throw  new UsernameNotFoundException("utilisateur not found");
        }
    }
    @PostMapping("login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO loginDto){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (authentication.isAuthenticated()){
            String token = jwtGenerator.generateToken(authentication);
            Optional<User> user=userRepository.findByEmail(loginDto.getEmail());
                User user1 = user.get();
                UserDTO userDTO = userService.convertEntityToDto(user1);
                return new ResponseEntity<>(new AuthResponseDTO(token, Optional.ofNullable(userDTO)), HttpStatus.OK);
        }
        else {
            throw  new UsernameNotFoundException("Invalid credentials");
        }

    }

    @PostMapping("/reset")
    public ApiResponse resetPasswordRequest(@RequestBody PasswordRequest passwordRequest,
                                            final HttpServletRequest servletRequest)
            throws MessagingException, UnsupportedEncodingException {
        Optional<User> user = userService.findUserByEmail(passwordRequest.getEmail());
        if (user.isPresent()) {
            String passwordResetToken = UUID.randomUUID().toString();
            String passwordResetUrl = passwordResetEmailLink(user.get(), urlFront+"/#/auth/login-2?token="+passwordResetToken, passwordResetToken);
            userService.createPasswordResetTokenForUser(user.get(), passwordResetToken);
            return new ApiResponse(HttpStatus.OK.value(), "Un mail vous est envoyé pour réinitialiser votre mot de passe", null);
        } else {
            return new ApiResponse(HttpStatus.NOT_FOUND.value(), "C'est addresse mail n'existe pas dans notre base de données", null);
        }
    }

    private String passwordResetEmailLink(User user, String url, String passwordToken) throws MessagingException, UnsupportedEncodingException {
        eventListener.sendPasswordResetVerificationEmail(url,user);
        return url;
    }

    @PostMapping("/reset-password")
    public ApiResponse resetPassword(@RequestBody PasswordResetUtil passwordRequestUtil){
        String token=passwordRequestUtil.getToken();
        String tokenVerificationResult = userService.validatePasswordResetToken(token);
     /*
        if (!tokenVerificationResult.equalsIgnoreCase("valid")) {
            return   new ApiResponse(400,"Invalid token",null);
        }
        */
        if (!tokenVerificationResult.equalsIgnoreCase("valid")) {
            return new ApiResponse(400, "Invalid password reset token", null);
        }
        Optional<User> theUser = Optional.ofNullable(userService.findUserByToken(token));
        if (theUser.isPresent()) {
            userService.changePassword(theUser.get(), passwordRequestUtil.getNewPassword());
            userService.deletePasswordResetToken(token);
            return new ApiResponse(200, "Password has been reset successfully", null);
        }
        return new ApiResponse(404, "User not found for this token", null);
    }


    @PostMapping("/change-password")
    public ApiResponse changePassword(@RequestBody ChangePasswordRequest changePasswordRequest) {
        // Vérifier que les champs requis sont présents
        if (changePasswordRequest.getEmail() == null || changePasswordRequest.getOldPassword() == null
                || changePasswordRequest.getNewPassword() == null) {
            return new ApiResponse(400, "Tous les champs (email, ancien mot de passe, nouveau mot de passe) sont requis", null);
        }

        // Trouver l'utilisateur par email
        Optional<User> userOptional = userRepository.findByEmail(changePasswordRequest.getEmail());
        if (!userOptional.isPresent()) {
            return new ApiResponse(404, "Aucun utilisateur trouvé avec cet email", null);
        }

        User user = userOptional.get();

        // Vérifier l'ancien mot de passe
        if (!bCryptPasswordEncoder.matches(changePasswordRequest.getOldPassword(), user.getPassword())) {
            return new ApiResponse(400, "L'ancien mot de passe est incorrect", null);
        }
        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (changePasswordRequest.getOldPassword().equals(changePasswordRequest.getNewPassword())) {
            return new ApiResponse(400, "Le nouveau mot de passe doit être différent de l'ancien", null);
        }
        user.setPassword(bCryptPasswordEncoder.encode(changePasswordRequest.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        return new ApiResponse(200, "Mot de passe changé avec succès", null);
    }

    @GetMapping("/verifyEmail")
    public String verifyEmail(@RequestParam("token") String token){
        VerificationToken theToken = tokenRepository.findByToken(token);
        if (theToken.getUser().getEnabled()){
            return "This account has already been verified, please, login.";
        }
        String verificationResult = userService.validateToken(token);
        if (verificationResult.equalsIgnoreCase("valid")){
            return "Compte activeted successfully. Now you can login to your account";
        }
        return "Invalid verification token";
    }


    /*
    @PostMapping("/change-password")
    public String changePassword(@RequestBody PasswordRequestUtil requestUtil){
        User user = userService.findUserByEmail(requestUtil.getEmail()).get();
        if (!userService.oldPasswordIsValid(user, requestUtil.getOldPassword())){
            return "Incorrect old password";
        }
        userService.changePassword(user, requestUtil.getNewPassword());
        return "Password changed successfully";
    }

     */
    public String applicationUrl(HttpServletRequest httpServlet) {
        return "http://"+httpServlet.getServerName()+":"+httpServlet.getServerPort()+httpServlet.getContextPath();

    }


//creation des maitres d'ouvrages
    @RequestMapping(path = "/createMaitreOuvrage", method = RequestMethod.POST)
    public  ApiResponse<User> createMO(@RequestBody MoRequest userRequest, final HttpServletRequest request) {
      //  ProjectDTO projectDTO = projectService.saveProject(projectRequest);
        User user = userService.saveMo(userRequest);

       //publisher.publishEvent(new RegistrationCompleteEvent(user, applicationUrl(request)));
        return  new ApiResponse<>(HttpStatus.OK.value(), "Maitre d'ouvrtage  crée avec succés",user);
    }

    @RequestMapping(path = "/createConsultant", method = RequestMethod.POST)
    public  ApiResponse<User> createConsultant(@RequestBody UserRequest userRequest, final HttpServletRequest request) {
        //  ProjectDTO projectDTO = projectService.saveProject(projectRequest);
        User user = userService.saveConsultant(userRequest);

        //publisher.publishEvent(new RegistrationCompleteEvent(user, applicationUrl(request)));
        return  new ApiResponse<>(HttpStatus.OK.value(), "Consultant  crée avec succés",user);
    }

//liste des maitres d'ouvrages
    @RequestMapping(path = "/by_role", method = RequestMethod.GET)
    public ApiResponse<List<User>> getMaitresOuvrages(@RequestParam String roleName) {
        List<User> users = userService.getUsersByRoleName(roleName);
        return new ApiResponse<>(HttpStatus.OK.value(), "Liste des utilisateurs récupérée avec succès", users);
    }



    @RequestMapping("by_role/projects")
    @GetMapping
    public ResponseEntity<AApiResponse<User>> getMaitresOuvragesByProject(
            @RequestParam Long projectId,
            @RequestParam String roleName,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max) {

        Pageable pageable = PageRequest.of(offset, max);
        Page<User> userPage = userService.getUsersByRoleNameAndProjectId(roleName,projectId, pageable);

        AApiResponse<User> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setMessage("Users filtered by project ID " + projectId);
        response.setData(userPage.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(userPage.getTotalElements());

        return ResponseEntity.ok(response);
    }



    @DeleteMapping("/deleteMaitreOuvrage/{id}")
    public ApiResponse<?> deleteUser(@PathVariable Long id) throws Exception {
        try {
            userService.deleteUserById(id);
            return new  ApiResponse<>(HttpStatus.OK.value(),"User deleted successfully.",null);
        } catch (Exception e) {
             throw  new Exception( "An error occurred while deleting the user."+e);
        }
    }

    @PutMapping("/updateMaitreOuvrage/{id}")
    public ApiResponse<User> updateMo(@RequestBody MoRequest moRequest,@PathVariable Long id) throws Exception {
        try{
            User updatedUser = userService.updateMo(moRequest, id);
            return new ApiResponse<>(HttpStatus.OK.value(), "User updated successfully", updatedUser);
        }catch (Exception e){
            throw new Exception("An error ocuured while deleting the user "+e);
        }
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<User> toggleEnabled(@PathVariable Long id) {
        User user = userService.toggleEnabled(id);
        String statut = Boolean.TRUE.equals(user.getEnabled()) ? "activé" : "désactivé";
        return new ApiResponse<>(HttpStatus.OK.value(), "Utilisateur " + statut + " avec succès", user);
    }

    @PutMapping("/updateConsultant/{id}")
    public  ApiResponse<User> updateConsultant(@RequestBody UserRequest userRequest,@PathVariable Long id) {
        try {
            User user = userService.updateConsultant(id, userRequest);
            return new ApiResponse<>(HttpStatus.OK.value(), "Consultant mis à jour avec succès", user);
        } catch (Exception e) {
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur interne du serveur", null);
        }
    }
}
