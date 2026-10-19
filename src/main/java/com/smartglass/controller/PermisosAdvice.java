package com.smartglass.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Permisos que las plantillas necesitan saber.
 *
 * "puedeAprobar" es true solo para ROLE_ADMIN. Las vistas (mis-cotizaciones y el
 * detalle de la obra) lo usan para mostrar u ocultar el boton "Aprobar".
 * IMPORTANTE: ocultar el boton NO protege nada; el POST de aprobacion se valida
 * en el servidor con {@link #esAdmin(Authentication)} (ver backend-parches.md).
 *
 * Se calcula desde las autoridades de Spring Security, asi que no depende del
 * nombre del campo de rol en la entidad Usuario.
 */
@ControllerAdvice
public class PermisosAdvice {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    /** true si el usuario autenticado tiene ROLE_ADMIN. */
    public static boolean esAdmin(Authentication auth) {
        return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> ROL_ADMIN.equals(a.getAuthority()));
    }

    @ModelAttribute("puedeAprobar")
    public boolean puedeAprobar(Authentication auth) {
        return esAdmin(auth);
    }
}
