package com.smartglass.service;

import com.smartglass.model.mysql.Usuario;
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

    public CustomOAuth2UserService(UserService userService) {
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