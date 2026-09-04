package com.smartglass.service;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    
    public UserService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario register(String nombre, String email, String telefono, 
        String tipoUsuario, String username, String password) {

        if (usuarioRepository.existsByUsername(username) || usuarioRepository.existsByEmail(email)) {
            return null;
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setTelefono(telefono);
        
        usuario.setTipoUsuario(tipoUsuario != null ? tipoUsuario : "ROLE_USER");
        usuario.setUsername(username);
        
        usuario.setPassword(passwordEncoder.encode(password)); 

        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    // --- NUEVO MÉTODO AÑADIDO PARA OAUTH2 ---
    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    // --- NUEVO: requerido por UserDetailsServiceImpl para autenticar con JWT ---
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }
}