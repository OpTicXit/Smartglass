package com.smartglass.config;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * H01: la contrasena del admin inicial ya no esta en el codigo. Se lee de la
 * variable de entorno ADMIN_PASSWORD (propiedad smartglass.admin.password) y solo
 * se usa la primera vez, cuando el admin todavia no existe. Si falta, no se crea
 * el admin y el arranque sigue (aviso en consola).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String passwordAdmin;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                           @Value("${smartglass.admin.password:}") String passwordAdmin) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordAdmin = passwordAdmin;
    }

    @Override
    public void run(String... args) throws Exception {
        // Busca si el admin ya existe para no duplicarlo cada vez que arranca el server
        Optional<Usuario> admin = usuarioRepository.findByEmail("admin@smartglass.com");

        if (admin.isEmpty() && (passwordAdmin == null || passwordAdmin.isBlank())) {
            System.out.println("⚠ No se creó el administrador inicial: define la variable de entorno ADMIN_PASSWORD.");
        } else if (admin.isEmpty()) {
            Usuario nuevoAdmin = new Usuario();
            nuevoAdmin.setNombre("Administrador del Sistema");
            nuevoAdmin.setEmail("admin@smartglass.com");
            // AJUSTE: username es nullable=false + unique en la entidad;
            // sin esta linea el INSERT fallaba en tiempo de ejecucion.
            nuevoAdmin.setUsername("admin");
            // Se encripta la contraseña antes de guardarla
            nuevoAdmin.setPassword(passwordEncoder.encode(passwordAdmin));
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