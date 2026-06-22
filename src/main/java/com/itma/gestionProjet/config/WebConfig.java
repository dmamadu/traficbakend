package com.itma.gestionProjet.config;

import com.itma.gestionProjet.security.ProjectAccessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ProjectAccessInterceptor projectAccessInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(projectAccessInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/users/**",
                        "/projects/**",
                        "/image/**",
                        "/geolocation/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}