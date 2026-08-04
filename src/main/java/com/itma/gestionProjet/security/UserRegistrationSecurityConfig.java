package com.itma.gestionProjet.security;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class UserRegistrationSecurityConfig {

@Autowired
    private JwtAuthEntryPoint authEntryPoint;

    private static final String[] SECURED_URLs = {"/yna/**"};

    private static final String[] UN_SECURED_URLs = {
            "/image/**",
            "projects/**",
            "/geolocation/**",
            "/error"
    };

    // Seuls les échanges d'authentification en dehors de toute session doivent rester publics.
    // Tout le reste de /users/** (création, modification, suppression, listing...) exige un JWT valide.
    private static final String[] PUBLIC_USER_URLs = {
            "/users/login",
            "/users/reset",
            "/users/reset-password",
            "/users/verifyEmail"
    };

    // Formulaire de plainte public (page "accueil" du frontend, sans authentification) — seule la
    // soumission (POST) est publique ; la consultation (GET, écran de gestion interne) reste protégée.
    private static final String[] PUBLIC_POST_URLs = {
            "/complaints"
    };
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

   /*
    @Bean

    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint(authEntryPoint)
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .requestMatchers("/users/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
*/

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthEntryPoint authEntryPoint) throws Exception {
        return http.csrf().disable()
                .cors(cors -> cors.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration cors = new CorsConfiguration();
                        cors.setAllowedOrigins(Arrays.asList(
                                "http://localhost:4200",
                                "https://testsolutiondigital.com",
                                "https://www.testsolutiondigital.com",
                                "http://localhost:8080",
                                "https://invodis.com",
                                "https://www.invodis.com",
                                "http://invodis.com",
                                "http://www.invodis.com",
                                "https://invodis.fereya.dev"
                        ));
                        cors.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                        cors.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
                        cors.setExposedHeaders(Collections.singletonList("Authorization"));
                        return cors;
                    }
                }))
                .authorizeHttpRequests()
                .requestMatchers(UN_SECURED_URLs).permitAll()
                .requestMatchers(PUBLIC_USER_URLs).permitAll()
                .requestMatchers(HttpMethod.POST, PUBLIC_POST_URLs).permitAll().and()
                .authorizeHttpRequests().requestMatchers(SECURED_URLs)
                .hasAuthority("Super Admin").anyRequest()
                .authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                 .authenticationEntryPoint(authEntryPoint)
                .and()
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public  JWTAuthenticationFilter jwtAuthenticationFilter() {
        return new JWTAuthenticationFilter();
    }

}
