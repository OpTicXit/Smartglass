package com.smartglass.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * AJUSTE: antes redirigia siempre a una pagina fija segun el rol, lo
 * que rompia el flujo de "visitante intenta agregar al carrito ->
 * Spring Security lo manda a /login -> despues de loguearse pierde
 * la accion". Ahora primero revisa si Spring Security guardo una
 * peticion original (RequestCache, ya activo por defecto en la
 * cadena de filtros) -- si existe, vuelve ahi para retomar justo lo
 * que el usuario estaba haciendo (incluye POSTs como /carrito/agregar,
 * no solo navegacion GET). Si no hay peticion guardada (login normal
 * desde /login), cae al comportamiento anterior por rol.
 */
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException {

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            redirectStrategy.sendRedirect(request, response, savedRequest.getRedirectUrl());
            return;
        }

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_ADMIN")) {
            redirectStrategy.sendRedirect(request, response, "/admin/dashboard");
        } else {
            redirectStrategy.sendRedirect(request, response, "/usuario");
        }
    }
}