package com.smartglass.security;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Implementacion de UserDetailsService.
 *
 * Correccion importante:
 * Spring Security espera ROLE_ADMIN cuando usamos hasRole("ADMIN").
 * Por eso normalizamos el tipo de usuario antes de crear la autoridad.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Usuario usuario = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con username: " + username));

        String authority = normalizarRol(usuario.getTipoUsuario());

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(authority)
        );

        return org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(authorities)
                .build();
    }

    /**
     * Convierte cualquier formato de rol guardado en base de datos
     * a una autoridad valida para Spring Security.
     *
     * Ejemplos:
     * Administrador -> ROLE_ADMIN
     * ADMIN -> ROLE_ADMIN
     * admin -> ROLE_ADMIN
     * ROLE_ADMIN -> ROLE_ADMIN
     * Usuario -> ROLE_USER
     */
    private String normalizarRol(String tipoUsuario) {
        if (tipoUsuario == null || tipoUsuario.isBlank()) {
            return "ROLE_USER";
        }

        String rol = tipoUsuario.trim().toUpperCase();

        return switch (rol) {
            case "ADMIN", "ADMINISTRADOR", "ROLE_ADMIN" -> "ROLE_ADMIN";
            case "USER", "USUARIO", "CLIENTE", "ROLE_USER" -> "ROLE_USER";
            default -> rol.startsWith("ROLE_") ? rol : "ROLE_" + rol;
        };
    }
}