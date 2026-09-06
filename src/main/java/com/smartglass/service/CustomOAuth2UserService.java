package com.smartglass.service;

import com.smartglass.model.mysql.Usuario;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    /**
     * AJUSTE (ciclo de dependencias): UserService depende del bean
     * PasswordEncoder, que esta definido dentro de SecurityConfig.
     * SecurityConfig a su vez depende de este CustomOAuth2UserService.
     * Sin @Lazy, Spring intenta resolver los tres en orden estricto y
     * entra en un ciclo (userService -> securityConfig ->
     * customOAuth2UserService -> userService) que no puede arrancar.
     *
     * @Lazy le dice a Spring que inyecte aqui un proxy de UserService
     * en vez del bean real: el proxy se resuelve recien la primera
     * vez que se usa (dentro de loadUser, ya con el contexto
     * totalmente inicializado), no durante la construccion de este
     * componente. Con ese unico punto "diferido" alcanza para romper
     * todo el ciclo -- no hizo falta tocar SecurityConfig.
     */
    public CustomOAuth2UserService(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String nombre = (String) attributes.get("name");

        Usuario usuario = userService.findByEmail(email).orElse(null);

        if (usuario == null) {
            String baseUsername = email.split("@")[0];
            String randomUsername = (baseUsername.length() > 10 ? baseUsername.substring(0, 10) : baseUsername)
                + "_" + UUID.randomUUID().toString().substring(0, 5);

            usuario = userService.register(nombre, email, null, "ROLE_USER", randomUsername, UUID.randomUUID().toString());

            if (usuario == null) {
                throw new OAuth2AuthenticationException("Error: No se pudo crear el usuario en la base de datos.");
            }
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
}