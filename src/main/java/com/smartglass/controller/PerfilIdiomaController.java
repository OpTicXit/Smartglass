package com.smartglass.controller;

import com.smartglass.config.I18nConfig;
import com.smartglass.config.UsuarioSesion;
import com.smartglass.model.mysql.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * T0: guarda el idioma de la interfaz. Siempre fija la cookie (sirve tambien a
 * visitantes); con sesion, ademas lo guarda en usuarios.preferencia_idioma del
 * usuario de la SESION (anti-spoofing: nunca un id del cuerpo).
 * /perfil/** cae en permitAll de SecurityConfig (sin cambios).
 */
@RestController
@RequestMapping("/perfil/idioma")
public class PerfilIdiomaController {

    private final UsuarioSesion usuarioSesion;
    private final LocaleResolver localeResolver;

    public PerfilIdiomaController(UsuarioSesion usuarioSesion, LocaleResolver localeResolver) {
        this.usuarioSesion = usuarioSesion;
        this.localeResolver = localeResolver;
    }

    @PatchMapping
    public ResponseEntity<Map<String, Object>> guardar(Authentication authentication,
                                                       @RequestBody(required = false) Map<String, Object> cuerpo,
                                                       HttpServletRequest req, HttpServletResponse res) {
        Object valor = cuerpo == null ? null : cuerpo.get("idioma");
        Optional<Locale> locale = valor instanceof String s ? I18nConfig.soportado(s) : Optional.empty();
        Map<String, Object> r = new LinkedHashMap<>();
        if (locale.isEmpty()) {
            r.put("error", "Idioma no soportado.");
            return ResponseEntity.badRequest().body(r);
        }
        String etiqueta = locale.get().toLanguageTag();
        localeResolver.setLocale(req, res, locale.get());

        Optional<Usuario> usuario = usuarioSesion.buscar(authentication);
        usuario.ifPresent(u -> {
            u.setPreferenciaIdioma(etiqueta);
            usuarioSesion.guardar(u);
        });
        r.put("idioma", etiqueta);
        r.put("guardadoEnCuenta", usuario.isPresent());
        return ResponseEntity.ok(r);
    }
}
