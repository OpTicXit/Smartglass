package com.smartglass.controller;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.CarritoService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/**
 * Centraliza los atributos que TODAS las vistas MVC (Thymeleaf) del
 * storefront necesitan en el header/navbar: si hay sesion, el
 * Usuario logueado (resolviendo tanto login por formulario como
 * OAuth2/Google), y el contador del carrito.
 *
 * Antes esta misma logica estaba duplicada casi identica en el viejo
 * UsuarioController y en CarritoController. Ahora vive en un solo
 * lugar y se aplica solo a los controllers MVC listados en
 * "assignableTypes" -- nunca a los @RestController de la API
 * (AuthController, VidrioPersonalizadoController), que no usan Model
 * ni necesitan esta consulta en cada request.
 */
@ControllerAdvice(assignableTypes = {
        TiendaController.class,
        CarritoController.class,
        CheckoutController.class,
        UsuarioDashboardController.class,
        VidrioPersonalizadoWebController.class
})
public class VistaGlobalAttributesAdvice {

    private final CarritoService carritoService;
    private final UsuarioRepository usuarioRepository;

    public VistaGlobalAttributesAdvice(CarritoService carritoService, UsuarioRepository usuarioRepository) {
        this.carritoService = carritoService;
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute
    public void addGlobalAttributes(Principal principal, Model model) {
        boolean logueado = (principal != null);
        model.addAttribute("usuarioLogueado", logueado);

        if (logueado) {
            model.addAttribute("usuario", resolverUsuarioActual(principal));
        }

        model.addAttribute("cantidadCarrito", carritoService.getCantidadTotal());
    }

    private Usuario resolverUsuarioActual(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauth2User = oauthToken.getPrincipal();
            String email = oauth2User.getAttribute("email");
            return usuarioRepository.findByEmail(email).orElse(null);
        }
        return usuarioRepository.findByUsername(principal.getName()).orElse(null);
    }
}