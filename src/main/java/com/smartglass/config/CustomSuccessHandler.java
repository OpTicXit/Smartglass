package com.smartglass.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * CustomSuccessHandler corregido.
 *
 * Mejoras:
 * - Si hay una peticion guardada, intenta retomarla.
 * - Evita redirigir a /logout o /login despues de autenticar.
 * - Si un usuario sin rol ADMIN intenta volver a una ruta admin guardada,
 *   se manda al dashboard de usuario normal.
 */
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        SavedRequest savedRequest = requestCache.getRequest(request, response);

        String targetUrl = determineTargetUrl(savedRequest, authentication);

        redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    private String determineTargetUrl(SavedRequest savedRequest, Authentication authentication) {
        boolean isAdmin = hasAuthority(authentication, "ROLE_ADMIN");

        if (savedRequest != null) {
            String redirectUrl = savedRequest.getRedirectUrl();
            String path = extractPath(redirectUrl);

            if (path != null && !path.contains("/logout") && !path.contains("/login")) {

                // Si la peticion guardada era admin pero el usuario no es admin,
                // no se le permite volver ahi.
                if (path.contains("/admin") && !isAdmin) {
                    return "/usuario";
                }

                return redirectUrl;
            }
        }

        return isAdmin ? "/admin/dashboard" : "/usuario";
    }

    private String extractPath(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            return URI.create(url).getPath();
        } catch (IllegalArgumentException ex) {
            return url;
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> authority.equals(a));
    }
}