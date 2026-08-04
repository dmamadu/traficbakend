package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.MailRequestDto;
import com.itma.gestionProjet.dtos.PlainteUserDTO;
import com.itma.gestionProjet.entities.PlainteUser;
import com.itma.gestionProjet.services.PlainteUserService;
import com.itma.gestionProjet.utils.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class PlainteUserController {

    private final PlainteUserService plainteUserService;
    private final EmailService emailService;

    @Value("${app.support.mail}")
    private String supportMail;



    // Public par conception : formulaire de plainte du grand public sur la page d'accueil, sans
    // authentification (voir PUBLIC_POST_URLs dans UserRegistrationSecurityConfig).
    @PostMapping
    public ResponseEntity<AApiResponse<PlainteUserDTO>> createComplaint(@RequestBody PlainteUserDTO plainteUserDTO)  {
        PlainteUser plainteUser = plainteUserService.createComplaint(plainteUserDTO);

        // Convertir l'entité en DTO
        PlainteUserDTO dto = new PlainteUserDTO();
        dto.setFirstName(plainteUser.getFirstName());
        dto.setLastName(plainteUser.getLastName());
        dto.setEmail(plainteUser.getEmail());
        dto.setPhone(plainteUser.getPhone());
        dto.setComplaintType(plainteUser.getComplaintType());
        dto.setComplaintDescription(plainteUser.getComplaintDescription());
        dto.setEtat(plainteUser.getEtat());
        dto.setCreatedAt(plainteUser.getCreatedAt());

        AApiResponse<PlainteUserDTO> response = new AApiResponse<>();
        response.setResponseCode(HttpStatus.CREATED.value());
        response.setData(List.of(dto));
        response.setMessage("Complaint created successfully");
        response.setLength(1);
        response.setOffset(0);
        response.setMax(1);

        String messagePlainte = "" +
                "<strong>Nom complet</strong>: "+dto.getFirstName()+' '+dto.getLastName()+"<br/>" +
                "<strong>Email</strong>: "+dto.getEmail()+"<br/>" +
                "<strong>Numéro de téléphone</strong>: "+dto.getPhone()+"<br/>"+
                "<strong>Type de plainte</strong>:"+dto.getComplaintType()+"<br/>" +
                "<strong>Description</strong>: "+dto.getComplaintDescription()+"<br/>";

        /*Information mail admin ------------*/
        MailRequestDto mailRequestDto = new MailRequestDto();
        mailRequestDto.setSubject(dto.getComplaintType());
        mailRequestDto.setToEmail(supportMail);
        mailRequestDto.setMessage(messagePlainte);
        mailRequestDto.setTemplate("reclamation_emails");
        /* ---------------fin info mail admin*/
        /* info replay mail*/
        MailRequestDto request = new MailRequestDto();
        request.setToEmail(dto.getEmail());

        request.setSubject("Accusé de réception de votre plainte du type : "+dto.getComplaintType());
        String message = "Bonjour "+dto.getFirstName()+' '+dto.getLastName()+",<br>" +
                "Nous vous confirmons la réception de votre plainte envoyée le "+new Date() +".<br/>" +
                "Ceci est un message automatique pour vous informer que votre requête a bien été enregistrée. Notre équipe l’examinera et reviendra vers vous dans les plus brefs délais si nécessaire.<br/>" +
                "Veuillez noter qu’il n’est pas nécessaire de répondre à cet email.<br/>" +
                "Merci de votre confiance.<br/>";
        request.setMessage(message);
        request.setTemplate("replay_mail");
        /* fin info replay mail*/
        try{
            emailService.sendMail(mailRequestDto);
            emailService.sendMailReception(request );
        }catch (MessagingException e){
            e.printStackTrace();
        }



        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Pas de notion de projet sur PlainteUser (intake public non trié) : réservé au staff interne
    // tant qu'aucun écran de triage n'existe côté frontend.
    @PreAuthorize("@permissionChecker.has('PLAINTES_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<PlainteUserDTO>> getAllComplaints() {
        List<PlainteUser> complaints = plainteUserService.getAllComplaints();
        List<PlainteUserDTO> plainteUserDTOList = complaints.stream().map(complaint -> {
            PlainteUserDTO dto = new PlainteUserDTO();
            dto.setFirstName(complaint.getFirstName());
            dto.setLastName(complaint.getLastName());
            dto.setEmail(complaint.getEmail());
            dto.setPhone(complaint.getPhone());
            dto.setComplaintType(complaint.getComplaintType());
            dto.setComplaintDescription(complaint.getComplaintDescription());
            dto.setEtat(complaint.getEtat());
            dto.setCreatedAt(complaint.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        AApiResponse<PlainteUserDTO> response = new AApiResponse<>();
        response.setResponseCode(HttpStatus.OK.value());
        response.setData(plainteUserDTOList);
        response.setMessage("Complaints fetched successfully");
        response.setLength(plainteUserDTOList.size());
        response.setOffset(0);
        response.setMax(plainteUserDTOList.size());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
