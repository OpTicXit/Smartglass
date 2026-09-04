package com.smartglass.config;

import com.smartglass.security.JwtAuthenticationFilter;
import com.smartglass.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Configuracion de seguridad unificada de SmartGlass.
 *
 * Fusiona lo que antes eran SecurityConfig y PasswordEncoderConfig.
 * Se elimina toda dependencia de Redis (CookieSerializer, sesiones
 * distribuidas, etc.) ya que la autenticacion basada en JWT es
 * stateless y no requiere almacenar sesion en el servidor.
 *
 * NOTA / SUPUESTO: se conserva el flujo de oauth2Login y formLogin
 * porque el proyecto ya usa CustomOAuth2UserService y un
 * CustomSuccessHandler. Si el objetivo final es una API 100% JWT
 * (sin login por formulario ni OAuth2 con sesion), se puede eliminar
 * ese bloque y dejar solo los endpoints /auth/** + el filtro JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomSuccessHandler customSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomSuccessHandler customSuccessHandler,
                           CustomOAuth2UserService customOAuth2UserService,
                           JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customSuccessHandler = customSuccessHandler;
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/usuario/**", "/carrito/**", "/checkout/**").authenticated()
                .requestMatchers("/error/**", "/css/**", "/js/**", "/img/**").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customSuccessHandler)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .successHandler(customSuccessHandler)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .failureHandler((request, response, exception) -> {
                    System.err.println("=== 🚨 ERROR INTERNO EN OAUTH2 🚨 ===");
                    System.err.println("Motivo: " + exception.getMessage());
                    exception.printStackTrace();
                    response.sendRedirect("/login?error");
                })
            )
            .logout(logout -> logout
                .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout"))
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            )
            // Las rutas /auth/** (login/registro por JWT) no necesitan sesion HTTP.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}