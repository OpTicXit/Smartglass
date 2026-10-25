package com.smartglass.controller;

import com.smartglass.config.SesionAnonima;
import com.smartglass.config.UsuarioSesion;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.CarritoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Atributos globales para vistas MVC/Thymeleaf.
 *
 * Correcciones:
 * - Usa Authentication en lugar de Principal.
 * - Expone autenticado, usuarioLogueado, usuario, esAdmin, isAdmin.
 * - Incluye AdminController.
 * - Busca usuario de forma mas robusta.
 */
@ControllerAdvice(assignableTypes = {
    HomeController.class,
    TiendaController.class,
    CarritoController.class,
    CheckoutController.class,
    UsuarioDashboardController.class,
    VidrioPersonalizadoWebController.class,
    AdminController.class,
    AdminCotizacionesController.class
})
public class VistaGlobalAttributesAdvice {

    private final CarritoService carritoService;
    private final UsuarioSesion usuarioSesion;
    private final SesionAnonima sesionAnonima;

    public VistaGlobalAttributesAdvice(
            CarritoService carritoService,
            UsuarioSesion usuarioSesion,
            SesionAnonima sesionAnonima
    ) {
        this.carritoService = carritoService;
        this.usuarioSesion = usuarioSesion;
        this.sesionAnonima = sesionAnonima;
    }

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, Model model, HttpServletRequest request) {

        boolean autenticado = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        Usuario usuario = null;
        boolean esAdmin = false;

        if (autenticado) {
            usuario = resolverUsuarioActual(authentication);

            esAdmin = authentication.getAuthorities().stream()
                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        }

        // Variable nueva y clara para saber si hay sesion.
        model.addAttribute("autenticado", autenticado);

        // Compatibilidad con templates existentes.
        // Se deja true solo si el usuario fue encontrado en BD.
        model.addAttribute("usuarioLogueado", usuario != null);

        model.addAttribute("usuario", usuario);

        // Variables claras para UI.
        model.addAttribute("esAdmin", esAdmin);

        // Compatibilidad por si algun template usa isAdmin.
        model.addAttribute("isAdmin", esAdmin);

        // T1 (D4): respaldo de la transferencia del login (antes de contar el carrito)
        if (usuario != null) sesionAnonima.transferirSiQueda(request.getSession(false), usuario.getId());

        model.addAttribute("cantidadCarrito", carritoService.getCantidadTotal());
    }

    /** U2: mismo criterio de siempre (OAuth2 por correo; local/JWT por username o correo), compartido
     *  con el interceptor de idioma en una sola consulta por peticion. */
    private Usuario resolverUsuarioActual(Authentication authentication) {
        return usuarioSesion.buscar(authentication).orElse(null);
    }
}