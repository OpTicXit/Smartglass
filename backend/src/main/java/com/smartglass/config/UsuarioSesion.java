package com.smartglass.config;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * Resuelve el Usuario de la sesion actual (anti-spoofing: nunca del cuerpo de
 * la peticion). Mismo criterio que VistaGlobalAttributesAdvice: OAuth2 por
 * correo; login local o JWT por username y, si parece correo, por email.
 * Lo usan los endpoints de perfil (/perfil/accesibilidad, /perfil/idioma) y el
 * interceptor de idioma.
 *
 * U2: el resultado se recuerda durante la peticion (atributo de request): el interceptor de idioma,
 * VistaGlobalAttributesAdvice y las notificaciones lo pedian por separado, una consulta cada uno.
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

    private static final String MEMO = UsuarioSesion.class.getName() + ".memo";

    private record Memo(String clave, Optional<Usuario> usuario) {}

    private static String clave(Authentication auth) {
        return auth.getClass().getName() + ':' + auth.getName();
    }

    public Optional<Usuario> buscar(Authentication auth) {
        if (!autenticado(auth)) return Optional.empty();
        RequestAttributes req = RequestContextHolder.getRequestAttributes();
        if (req != null && req.getAttribute(MEMO, RequestAttributes.SCOPE_REQUEST) instanceof Memo m && m.clave().equals(clave(auth))) {
            return m.usuario();
        }
        Optional<Usuario> usuario = consultar(auth);
        if (req != null) req.setAttribute(MEMO, new Memo(clave(auth), usuario), RequestAttributes.SCOPE_REQUEST);
        return usuario;
    }

    private Optional<Usuario> consultar(Authentication auth) {
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
        return limpiar(usuarioRepository.save(usuario));   // la proxima busqueda de esta peticion ve lo guardado
    }

    private static Usuario limpiar(Usuario guardado) {
        RequestAttributes req = RequestContextHolder.getRequestAttributes();
        if (req != null) req.removeAttribute(MEMO, RequestAttributes.SCOPE_REQUEST);
        return guardado;
    }
}
