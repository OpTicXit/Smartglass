package com.smartglass.config;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Busca si el admin ya existe para no duplicarlo cada vez que arranca el server
        Optional<Usuario> admin = usuarioRepository.findByEmail("admin@smartglass.com");

        if (admin.isEmpty()) {
            Usuario nuevoAdmin = new Usuario();
            nuevoAdmin.setNombre("Administrador del Sistema");
            nuevoAdmin.setEmail("admin@smartglass.com");
            // Se encripta la contraseña antes de guardarla
            nuevoAdmin.setPassword(passwordEncoder.encode("Admin123!")); 
            nuevoAdmin.setRol("ADMIN");
            
            usuarioRepository.save(nuevoAdmin);
            System.out.println("✅ Usuario Administrador creado automáticamente en MySQL.");
        }
    }
}