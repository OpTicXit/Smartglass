package com.smartglass.service;

import com.smartglass.dto.LoginRequest;
import com.smartglass.dto.RegistroRequest;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.security.JwtProvider;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Estructura base de autenticacion de SmartGlass.
 * Encapsula el registro de usuarios y el login, que delega en el
 * AuthenticationManager de Spring Security y devuelve un JWT firmado
 * por JwtProvider. El encriptado de la contrasena NO ocurre aqui:
 * UserService.register(...) lo hace internamente con BCrypt, asi que
 * este service solo le pasa la contrasena en texto plano del DTO.
 *
 * F43: este service es el UNICO punto que normaliza el telefono a E.164
 * (+573001234567) y guarda el pais (telefonoPais), para que los dos
 * controllers (REST y MVC) no dupliquen esa logica. El honeypot se
 * rechaza aqui como segunda barrera (la primera esta en los controllers,
 * en silencio).
 */
@Service
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UsuarioRepository usuarioRepository;

    public AuthService(UserService userService,
                       AuthenticationManager authenticationManager,
                       JwtProvider jwtProvider,
                       UsuarioRepository usuarioRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra un nuevo usuario. Lanza IllegalStateException si el honeypot
     * viene lleno, si el email ya esta en uso, si el telefono no es valido o
     * si UserService.register(...) devuelve null (username o email duplicados).
     */
    public Usuario register(RegistroRequest request) {
        if (request == null || request.esSpam()) {
            // Honeypot lleno: bot. Mensaje generico, no revela el motivo.
            throw new IllegalStateException("No se pudo completar el registro.");
        }

        Optional<Usuario> existente = userService.findByEmail(request.getEmail());
        if (existente.isPresent()) {
            throw new IllegalStateException("Ya existe una cuenta con ese email.");
        }

        // F43: normaliza a E.164 y obtiene el pais ("CO", "US", "MX"...)
        String[] telefono = normalizarTelefono(request.getTelefono());

        // Firma real de UserService.register: (nombre, email, telefono, tipoUsuario, username, password)
        Usuario usuario = userService.register(
                request.getNombre(),
                request.getEmail(),
                telefono == null ? null : telefono[0],
                "ROLE_USER",
                request.getUsername(),
                request.getPassword() // texto plano: UserService ya lo encripta con BCrypt
        );
        if (usuario == null) {
            throw new IllegalStateException("El username o el email ya estan en uso.");
        }

        if (telefono != null) {
            usuario.setTelefonoPais(telefono[1]);
            usuario = usuarioRepository.save(usuario);
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

    /**
     * F43: convierte cualquier formato valido a E.164 y detecta el pais solo.
     * La region "CO" solo aplica si el numero NO trae "+"; si trae, el pais
     * sale del propio numero. Devuelve { e164, pais } o null si viene vacio.
     */
    private String[] normalizarTelefono(String crudo) {
        if (crudo == null || crudo.isBlank()) return null;
        try {
            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber n = util.parse(crudo.trim(), "CO");
            if (!util.isValidNumber(n)) {
                throw new IllegalStateException("Telefono invalido. Ej.: +57 300 123 4567");
            }
            String e164 = util.format(n, PhoneNumberUtil.PhoneNumberFormat.E164);
            return new String[] { e164, util.getRegionCodeForNumber(n) };
        } catch (NumberParseException e) {
            throw new IllegalStateException("Telefono invalido. Ej.: +57 300 123 4567");
        }
    }
}