package com.itma.gestionProjet.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class ProjectAccessInterceptor implements HandlerInterceptor {

    private static final Set<String> ADMIN_ROLES = Set.of("Super Admin", "Admin");

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String projectIdParam = request.getParameter("projectId");
        if (projectIdParam == null) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        Long projectId;
        try {
            projectId = Long.parseLong(projectIdParam);
        } catch (NumberFormatException e) {
            return true;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserRegistrationDetails)) {
            return true;
        }

        UserRegistrationDetails userDetails = (UserRegistrationDetails) principal;

        boolean isAdmin = userDetails.getRoleNames().stream()
                .anyMatch(ADMIN_ROLES::contains);
        if (isAdmin) {
            return true;
        }

        boolean hasAccess = userDetails.getProjectIds().contains(projectId);

        if (!hasAccess) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Accès refusé : vous n'êtes pas membre du projet " + projectId);
            return false;
        }

        return true;
    }
}