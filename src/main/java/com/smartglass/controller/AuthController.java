package com.smartglass.controller;

import com.smartglass.dto.ErrorResponse;
import com.smartglass.dto.LoginRequest;
import com.smartglass.dto.RegistroRequest;
import com.smartglass.dto.TokenResponse;
import com.smartglass.dto.UsuarioResponse;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Capa API REST de autenticacion de SmartGlass. Delega toda la logica de
 * negocio en AuthService; este controller solo traduce HTTP <-> DTOs.
 *
 * F43: @Valid activa las anotaciones del DTO (correo, telefono, contrasena,
 * coincidencia de contrasenas). Sin el, Bean Validation nunca se ejecutaba.
 * El honeypot se descarta en silencio: se responde 201 sin crear nada, para
 * no revelar al bot que fue detectado.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequest request,
                                       BindingResult bindingResult) {
        if (request.esSpam()) {
            // Honeypot: parece exitoso, no crea nada.
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        if (bindingResult.hasErrors()) {
            String mensaje = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(new ErrorResponse(mensaje));
        }
        try {
            Usuario usuario = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request);
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Usuario o contraseña incorrectos."));
        }
    }
}