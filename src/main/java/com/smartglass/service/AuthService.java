package com.smartglass.service;

import com.smartglass.dto.LoginRequest;
import com.smartglass.dto.RegisterRequest;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.security.JwtProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Estructura base de autenticacion de SmartGlass.
 *
 * Encapsula el registro de usuarios (con password encriptado via
 * BCrypt) y el login, que delega en el AuthenticationManager de
 * Spring Security y devuelve un JWT firmado por JwtProvider.
 *
 * NOTA / TODO: ajustar los campos de RegisterRequest/LoginRequest y
 * la firma de UserService.register(...) a los que ya existen en tu
 * proyecto (ver CustomOAuth2UserService, que llama a
 * userService.register(nombre, email, password, rol, username, token)).
 */
@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    public AuthService(UserService userService,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtProvider jwtProvider) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    /**
     * Registra un nuevo usuario con la contrasena encriptada.
     * Lanza IllegalStateException si el email ya esta en uso.
     */
    public Usuario register(RegisterRequest request) {
        Optional<Usuario> existente = userService.findByEmail(request.getEmail());
        if (existente.isPresent()) {
            throw new IllegalStateException("Ya existe una cuenta con ese email.");
        }

        String passwordEncriptada = passwordEncoder.encode(request.getPassword());

        Usuario usuario = userService.register(
                request.getNombre(),
                request.getEmail(),
                passwordEncriptada,
                "ROLE_USER",
                request.getUsername(),
                null
        );

        if (usuario == null) {
            throw new IllegalStateException("No se pudo crear el usuario en la base de datos.");
        }

        return usuario;
    }

    /**
     * Autentica al usuario con username/password y, si es correcto,
     * devuelve un JWT listo para usar en el header Authorization.
     */
    public String login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        return jwtProvider.generateToken(authentication.getName());
    }
}
