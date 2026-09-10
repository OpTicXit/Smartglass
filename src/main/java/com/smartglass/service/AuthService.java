package com.smartglass.service;

import com.smartglass.dto.LoginRequest;
import com.smartglass.dto.RegisterRequest;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.security.JwtPorvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Estructura base de autenticacion de SmartGlass.
 *
 * Encapsula el registro de usuarios y el login, que delega en el
 * AuthenticationManager de Spring Security y devuelve un JWT firmado
 * por JwtProvider. El encriptado de la contraseña ya NO ocurre aqui:
 * UserService.register(...) lo hace internamente con BCrypt, asi que
 * este service solo le pasa la contraseña en texto plano recibida
 * del DTO.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtPorvider jwtProvider;

    public AuthService(UserService userService,
                        AuthenticationManager authenticationManager,
                        JwtPorvider jwtProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
    }

    /**
     * Registra un nuevo usuario. Lanza IllegalStateException si el
     * email ya esta en uso, o si UserService.register(...) devuelve
     * null (username o email ya existen -- ver su implementacion).
     */
    public Usuario register(RegisterRequest request) {
        Optional<Usuario> existente = userService.findByEmail(request.getEmail());
        if (existente.isPresent()) {
            throw new IllegalStateException("Ya existe una cuenta con ese email.");
        }

        // Firma real de UserService.register: (nombre, email, telefono, tipoUsuario, username, password)
        Usuario usuario = userService.register(
                request.getNombre(),
                request.getEmail(),
                request.getTelefono(),
                "ROLE_USER",
                request.getUsername(),
                request.getPassword() // texto plano: UserService ya lo encripta con BCrypt
        );

        if (usuario == null) {
            throw new IllegalStateException("El username o el email ya estan en uso.");
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