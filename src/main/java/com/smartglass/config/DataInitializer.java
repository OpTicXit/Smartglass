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
            // AJUSTE: username es nullable=false + unique en la entidad;
            // sin esta linea el INSERT fallaba en tiempo de ejecucion.
            nuevoAdmin.setUsername("admin");
            // Se encripta la contraseña antes de guardarla
            nuevoAdmin.setPassword(passwordEncoder.encode("Admin123!"));
            // AJUSTE: el setter real es setTipoUsuario (no setRol), y el valor
            // debe llevar el prefijo "ROLE_" para que .hasRole("ADMIN") de
            // SecurityConfig lo reconozca (UserDetailsServiceImpl usa este
            // valor tal cual como nombre de autoridad, sin agregar el prefijo).
            nuevoAdmin.setTipoUsuario("ROLE_ADMIN");

            usuarioRepository.save(nuevoAdmin);
            System.out.println("✅ Usuario Administrador creado automáticamente en MySQL.");
        }
    }
}