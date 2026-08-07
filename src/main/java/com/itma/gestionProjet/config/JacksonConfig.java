package com.itma.gestionProjet.config;

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Empêche Jackson de tenter de sérialiser les proxys Hibernate (ByteBuddy) qui apparaissent quand
// une relation LAZY est renvoyée telle quelle par un controller (ex. User.projects dans
// UserController.createConsultant) : sans ce module, Jackson introspecte les getters synthétiques
// du proxy (hibernateLazyInitializer) et casse la réponse en plein flux avec une
// InvalidDefinitionException sur org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor.
@Configuration
public class JacksonConfig {

    @Bean
    public Hibernate6Module hibernate6Module() {
        return new Hibernate6Module();
    }
}
