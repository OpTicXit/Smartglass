package com.smartglass.security;

import com.smartglass.model.Usuario;
import com.smartglass.service.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Implementacion de UserDetailsService que busca al usuario por su
 * username (o email, segun convenga) para que Spring Security pueda
 * autenticarlo con el AuthenticationManager.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = userService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con username: " + username));

        String rol = (usuario.getTipoUsuario() != null) ? usuario.getTipoUsuario() : "ROLE_USER";
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(rol));

        return org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .authorities(authorities)
                .build();
    }
}
