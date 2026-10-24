package com.smartglass.controller;

import com.smartglass.service.VidrioSostenibleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Permisos y datos globales para las vistas Thymeleaf.
 * - esAdmin(auth): helper estatico usado por los controllers.
 * - puedeAprobar / esAdmin: banderas para las plantillas.
 * - cotizacionesPendientes: badge del nav del admin. Solo consulta
 *   Mongo en rutas /admin/**, para no gastar una query en cada
 *   request (incluida la API REST).
 */
@ControllerAdvice
public class PermisosAdvice {

    private final VidrioSostenibleService vidrioSostenibleService;

    public PermisosAdvice(VidrioSostenibleService vidrioSostenibleService) {
        this.vidrioSostenibleService = vidrioSostenibleService;
    }

    public static boolean esAdmin(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())
                && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @ModelAttribute("puedeAprobar")
    public boolean puedeAprobar(Authentication auth) {
        return esAdmin(auth);
    }

    @ModelAttribute("esAdmin")
    public boolean esAdminVistas(Authentication auth) {
        return esAdmin(auth);
    }

    @ModelAttribute("cotizacionesPendientes")
    public long cotizacionesPendientes(Authentication auth, HttpServletRequest request) {
        if (!esAdmin(auth)) return 0L;
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/admin")) return 0L;
        return vidrioSostenibleService.contarPorEstado(VidrioSostenibleService.COTIZACION);
    }

    // OPCIONAL: solo si alguna vista lee ${uriActual}; el nav usa data-match, no lo requiere.
    // @ModelAttribute("uriActual") public String uriActual(HttpServletRequest r) { return r.getRequestURI(); }
}