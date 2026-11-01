package com.smartglass.service;

import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Login con Google: VINCULA, no duplica (F43/A4) y deja resuelto F30.
 * F30: el nombre del principal (auth.getName()) es el atributo "mysql_username",
 * es decir el username real de MySQL. Por eso findByUsername(auth.getName())
 * funciona para cuentas locales y de Google en REST, CarritoService y
 * CheckoutController, sin tocar esos archivos.
 * (El snippet de la tarea proponia username = email; NO se aplica: los usuarios
 * de Google ya existentes tienen un username aleatorio y cambiar el esquema los
 * dejaria huerfanos.)
 * Reglas:
 * - Se exige correo verificado por Google (email_verified) y el "sub" de Google.
 * - Sin cuenta con ese correo: se crea y se guarda provider/providerId.
 * - Cuenta local existente sin vinculo: se vincula (una sola cuenta por correo)
 *   y, por F44, se INVALIDA la contrasena local: quien registro un correo ajeno
 *   pierde ese acceso; el dueno real entra con Google o recupera contrasena.
 * - Cuenta ya vinculada a OTRO "sub" de Google: se rechaza el acceso.
 *
 * CICLO DE DEPENDENCIAS (fix del arranque): el bean passwordEncoder vive dentro
 * de SecurityConfig, y SecurityConfig necesita este servicio en su constructor.
 * Con @Lazy en UserService Y en PasswordEncoder, Spring inyecta proxies y el
 * circulo se rompe; el encoder real se resuelve en el primer uso, cuando
 * SecurityConfig ya esta completamente construido. NO crear un segundo
 * @Bean PasswordEncoder en otra clase: daria NoUniqueBeanDefinitionException.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String PROVEEDOR = "google";

    private final UserService userService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(@Lazy UserService userService,
                                   UsuarioRepository usuarioRepository,
                                   @Lazy PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = texto(attributes.get("email"));
        String sub = texto(attributes.get("sub"));
        String nombre = texto(attributes.get("name"));

        if (email == null || email.isBlank() || sub == null || sub.isBlank()) {
            throw rechazo("datos_incompletos", "Google no entregó el correo o el identificador de la cuenta.");
        }
        email = email.trim().toLowerCase(Locale.ROOT);

        if (!correoVerificado(attributes.get("email_verified"))) {
            throw rechazo("email_no_verificado", "El correo de Google no está verificado.");
        }

        Usuario usuario = userService.findByEmail(email).orElse(null);

        if (usuario == null) {
            // Cuenta nueva: username derivado del correo + sufijo aleatorio unico.
            String baseUsername = email.split("@")[0];
            String randomUsername = (baseUsername.length() > 10 ? baseUsername.substring(0, 10) : baseUsername)
                    + "_" + UUID.randomUUID().toString().substring(0, 5);
            String nombreSeguro = (nombre == null || nombre.isBlank()) ? baseUsername : nombre;
            usuario = userService.register(nombreSeguro, email, null, "ROLE_USER",
                    randomUsername, UUID.randomUUID().toString());
            if (usuario == null) {
                throw rechazo("registro_fallido", "No se pudo crear el usuario en la base de datos.");
            }
            usuario.setProvider(PROVEEDOR);
            usuario.setProviderId(sub);
            usuario = usuarioRepository.save(usuario);

        } else if (usuario.getProviderId() == null) {
            // F44: primera vinculacion de una cuenta local (correo no verificado en el
            // registro). Google si verifica el correo, asi que el dueno real del correo
            // es quien esta entrando: se vincula y se invalida la contrasena local
            // (sustituida por una aleatoria codificada) para cerrar el takeover.
            usuario.setProvider(PROVEEDOR);
            usuario.setProviderId(sub);
            usuario.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario = usuarioRepository.save(usuario);

        } else if (!sub.equals(usuario.getProviderId())) {
            throw rechazo("cuenta_ya_vinculada", "Este correo ya está vinculado a otra cuenta de Google.");
        }

        String rol = (usuario.getTipoUsuario() != null) ? usuario.getTipoUsuario() : "ROLE_USER";
        GrantedAuthority authority = new SimpleGrantedAuthority(rol);

        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("mysql_username", usuario.getUsername());

        return new DefaultOAuth2User(
                Collections.singletonList(authority),
                customAttributes,
                "mysql_username"
        );
    }

    private static String texto(Object valor) {
        return valor instanceof String s ? s : null;
    }

    /** Google envia un booleano; se acepta tambien "true" como texto. */
    private static boolean correoVerificado(Object valor) {
        return Boolean.TRUE.equals(valor) || "true".equalsIgnoreCase(String.valueOf(valor));
    }

    private static OAuth2AuthenticationException rechazo(String codigo, String descripcion) {
        return new OAuth2AuthenticationException(new OAuth2Error(codigo), descripcion);
    }
}