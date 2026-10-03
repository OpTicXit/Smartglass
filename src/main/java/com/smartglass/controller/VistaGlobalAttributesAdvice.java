package com.smartglass.controller;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.CarritoService;
import com.smartglass.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

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
    AdminController.class
})
public class VistaGlobalAttributesAdvice {

    private final CarritoService carritoService;
    private final UsuarioRepository usuarioRepository;
    private final UserService userService;

    public VistaGlobalAttributesAdvice(
            CarritoService carritoService,
            UsuarioRepository usuarioRepository,
            UserService userService
    ) {
        this.carritoService = carritoService;
        this.usuarioRepository = usuarioRepository;
        this.userService = userService;
    }

    @ModelAttribute
    public void addGlobalAttributes(Authentication authentication, Model model) {

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

        model.addAttribute("cantidadCarrito", carritoService.getCantidadTotal());
    }

    private Usuario resolverUsuarioActual(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        // OAuth2 / Google
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauth2User = oauthToken.getPrincipal();

            String email = oauth2User.getAttribute("email");

            if (email != null) {
                Optional<Usuario> usuarioByEmail = usuarioRepository.findByEmail(email);

                if (usuarioByEmail.isPresent()) {
                    return usuarioByEmail.get();
                }
            }

            // Fallback si el name del OAuth2 coincide con username/email local.
            String oauthName = authentication.getName();

            if (oauthName != null) {
                return buscarUsuarioPorUsernameOEmail(oauthName).orElse(null);
            }

            return null;
        }

        // Login normal o JWT
        String login = authentication.getName();

        return buscarUsuarioPorUsernameOEmail(login).orElse(null);
    }

    /**
     * Busca primero por username usando el mismo servicio que usa UserDetailsServiceImpl.
     * Si no lo encuentra y el valor parece email, intenta buscar por email.
     */
    private Optional<Usuario> buscarUsuarioPorUsernameOEmail(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }

        Optional<Usuario> usuarioPorUsername = userService.findByUsername(login);

        if (usuarioPorUsername.isPresent()) {
            return usuarioPorUsername;
        }

        if (login.contains("@")) {
            return usuarioRepository.findByEmail(login);
        }

        return Optional.empty();
    }
}