package com.smartglass.config;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Resuelve el Usuario de la sesion actual (anti-spoofing: nunca del cuerpo de
 * la peticion). Mismo criterio que VistaGlobalAttributesAdvice: OAuth2 por
 * correo; login local o JWT por username y, si parece correo, por email.
 * Lo usan los endpoints de perfil (/perfil/accesibilidad, /perfil/idioma) y el
 * interceptor de idioma.
 */
@Component
public class UsuarioSesion {

    private final UsuarioRepository usuarioRepository;

    public UsuarioSesion(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public static boolean autenticado(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    public Optional<Usuario> buscar(Authentication auth) {
        if (!autenticado(auth)) return Optional.empty();
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            String email = oauth.getPrincipal().getAttribute("email");
            if (email != null) {
                Optional<Usuario> porCorreo = usuarioRepository.findByEmail(email);
                if (porCorreo.isPresent()) return porCorreo;
            }
        }
        String login = auth.getName();
        if (login == null || login.isBlank()) return Optional.empty();
        Optional<Usuario> porUsername = usuarioRepository.findByUsername(login);
        if (porUsername.isPresent() || !login.contains("@")) return porUsername;
        return usuarioRepository.findByEmail(login);
    }

    public Usuario guardar(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }
}
