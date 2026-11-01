package com.smartglass.controller;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * T1 accesibilidad: preferencia por usuario para sincronizar entre dispositivos.
 *
 * - El navegador aplica primero lo que tiene en localStorage (a11y-init.js,
 *   sin parpadeo); este endpoint solo sincroniza.
 * - Anti-spoofing: el usuario sale SIEMPRE de la sesion (Authentication), nunca
 *   del cuerpo. El cuerpo solo trae el valor de la preferencia.
 * - /perfil/** cae en anyRequest().permitAll() de SecurityConfig (que no se
 *   toca): por eso cada metodo responde 401 si no hay sesion.
 * - Valores validos: null (normal), "daltotonico", "fotosensible" o
 *   "daltotonico,fotosensible" (orden canonico). Cualquier otro -> 400.
 */
@RestController
@RequestMapping("/perfil/accesibilidad")
public class PerfilAccesibilidadController {

    private static final Set<String> MODOS = Set.of("daltotonico", "fotosensible");

    private final UsuarioRepository usuarioRepository;

    public PerfilAccesibilidadController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> obtener(Authentication authentication) {
        Optional<Usuario> usuario = usuarioDeSesion(authentication);
        if (usuario.isEmpty()) {
            return ResponseEntity.status(401).body(respuesta(null, "Inicia sesión para sincronizar la preferencia."));
        }
        return ResponseEntity.ok(respuesta(usuario.get().getPreferenciaAccesibilidad(), null));
    }

    @PatchMapping
    public ResponseEntity<Map<String, Object>> guardar(Authentication authentication,
                                                       @RequestBody(required = false) Map<String, Object> cuerpo) {
        Optional<Usuario> usuario = usuarioDeSesion(authentication);
        if (usuario.isEmpty()) {
            return ResponseEntity.status(401).body(respuesta(null, "Inicia sesión para sincronizar la preferencia."));
        }

        Object bruto = cuerpo == null ? null : cuerpo.get("preferencia");
        Optional<String> normalizada = normalizar(bruto);
        if (normalizada == null) {
            return ResponseEntity.badRequest().body(respuesta(null, "Preferencia de accesibilidad no válida."));
        }

        Usuario u = usuario.get();
        u.setPreferenciaAccesibilidad(normalizada.orElse(null));
        usuarioRepository.save(u);
        return ResponseEntity.ok(respuesta(u.getPreferenciaAccesibilidad(), null));
    }

    /**
     * Optional.empty() = modo normal (se guarda null); null = valor invalido.
     * Acepta "", "normal" o null como normal y ordena los modos para que
     * "fotosensible,daltotonico" y "daltotonico,fotosensible" sean lo mismo.
     */
    static Optional<String> normalizar(Object bruto) {
        if (bruto == null) return Optional.empty();
        if (!(bruto instanceof String texto)) return null;
        String limpio = texto.trim().toLowerCase();
        if (limpio.isEmpty() || "normal".equals(limpio)) return Optional.empty();

        Set<String> modos = new TreeSet<>();
        for (String parte : limpio.split(",")) {
            String modo = parte.trim();
            if (!MODOS.contains(modo)) return null;
            modos.add(modo);
        }
        return Optional.of(String.join(",", modos));
    }

    private static Map<String, Object> respuesta(String preferencia, String error) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("preferencia", preferencia);
        if (error != null) r.put("error", error);
        return r;
    }

    /** Mismo criterio que VistaGlobalAttributesAdvice: OAuth2 por correo; local/JWT por username o correo. */
    private Optional<Usuario> usuarioDeSesion(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        if (authentication instanceof OAuth2AuthenticationToken oauth) {
            String email = oauth.getPrincipal().getAttribute("email");
            if (email != null) {
                Optional<Usuario> porCorreo = usuarioRepository.findByEmail(email);
                if (porCorreo.isPresent()) return porCorreo;
            }
        }
        String login = authentication.getName();
        if (login == null || login.isBlank()) return Optional.empty();
        Optional<Usuario> porUsername = usuarioRepository.findByUsername(login);
        if (porUsername.isPresent() || !login.contains("@")) return porUsername;
        return usuarioRepository.findByEmail(login);
    }
}
