package com.itma.gestionProjet.events.listenner;

import com.itma.gestionProjet.dtos.MailRequestDto;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.events.RegistrationCompleteEvent;
import com.itma.gestionProjet.services.imp.UserService;
import com.itma.gestionProjet.utils.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class RegistrationCompleteEventListener  implements ApplicationListener<RegistrationCompleteEvent> {

    @Autowired
    private UserService userService;
    @Autowired
    private JavaMailSender mailSender;
    // Pas de champ « utilisateur courant » : ce composant est un singleton partagé entre requêtes,
    // un champ mutable mélangerait les destinataires de deux inscriptions simultanées.
    private final EmailService emailService;

    @Value("${app.front.url}")
    private String urlFront;


    @Override
    public void onApplicationEvent(RegistrationCompleteEvent event) {
        String  verificationToken= UUID.randomUUID().toString();
        User user = event.getUser();
        String url=urlFront+"/#/users/verifyEmail?token="+verificationToken;
        userService.saveUserVerificationToken(user,verificationToken);
        try {
            sendVerificationEmail(url, user);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        log.info("Click the link to verify your registration : {}",url);
    }
    public void sendVerificationEmail(String url, User user) throws MessagingException, UnsupportedEncodingException {
        String subject = "Email Verification";
        String senderName = "User Registration Portal Service";
        String mailContent = "Bonjour " + user.getFirstname() +
                "<br/>" +
                "Nous avons bien reçu une demande de modification de votre mot de passe chez Solution Digitale.<br/>" +
                "Pour réinitialiser votre mot de passe et finaliser cette opération, veuillez cliquer sur le lien ci-dessous pour confirmer votre email :<br/>" +
                "<a class='button' href='" + url + "'>Cliquez ici pour réinitialiser votre mot de passe</a><br/>" +
                "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email.<br/>" +
                "Merci de votre confiance et à bientôt sur Solution Digitale.<br/>";
        MailRequestDto mailRequestDto = new MailRequestDto();
        mailRequestDto.setTemplate("verification_mail");
        mailRequestDto.setToEmail(user.getEmail());
        mailRequestDto.setSubject(subject);
        mailRequestDto.setMessage(mailContent);
        emailService.sendMail(mailRequestDto);
    }

    public void sendPasswordResetVerificationEmail(String url,User user) throws MessagingException, UnsupportedEncodingException {
        String subject = "Password Reset Request Verification";
        String senderName = "User Registration Portal Service";
        String mailContent = "<p>Bonjour " + user.getFirstname() + ",</p>" +
                        "<p><b>Vous avez récemment demandé à réinitialiser votre mot de passe.</b></p>" +
                        "<p>Veuillez suivre le lien ci-dessous pour compléter l'opération :</p>" +
                        "<a class='button' href='" + url + "'>Réinitialiser votre mot de passe</a><br/>" +
                        "<p>Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email.</p>" +
                        "<p>Merci de votre confiance et à bientôt sur notre portail d'enregistrement des utilisateurs.</p>";
        MailRequestDto mailRequestDto = new MailRequestDto();
        mailRequestDto.setTemplate("verification_mail");
        mailRequestDto.setToEmail(user.getEmail());
        mailRequestDto.setSubject(subject);
        mailRequestDto.setMessage(mailContent);
        emailService.sendMail(mailRequestDto);
    }
}
