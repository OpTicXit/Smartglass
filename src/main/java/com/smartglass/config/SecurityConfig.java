package com.smartglass.config;

import com.smartglass.security.JwtAuthenticationFilter;
import com.smartglass.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuracion de seguridad de SmartGlass.
 *
 * Correcciones aplicadas:
 * - Rutas con /** correctamente.
 * - Sin espacios en los matchers.
 * - /admin/** protegido con hasRole("ADMIN").
 * - CORS habilitado para React.
 * - Se mantiene formLogin + oauth2Login para no romper la app MVC actual.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomSuccessHandler customSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${smartglass.cors.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    public SecurityConfig(
            CustomSuccessHandler customSuccessHandler,
            CustomOAuth2UserService customOAuth2UserService,
            @Lazy JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth

                // Auth API publica
                .requestMatchers("/auth/**", "/api/auth/**").permitAll()

                // Publicas
                .requestMatchers(
                    "/login",
                    "/login/**",
                    "/registro",
                    "/registro/**",
                    "/oauth2/**",
                    "/error/**",
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/webjars/**",
                    "/favicon.ico"
                ).permitAll()

                // Admin
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                // Acciones autenticadas
                .requestMatchers(HttpMethod.POST, "/detalle/*/resenas").authenticated()
                .requestMatchers(
                    "/usuario",
                    "/usuario/**",
                    "/carrito",
                    "/carrito/**",
                    "/checkout",
                    "/checkout/**",
                    "/vidrio-personalizado",
                    "/vidrio-personalizado/**",
                    "/api/usuario/**",
                    "/api/carrito/**",
                    "/api/checkout/**"
                ).authenticated()

                // El resto publico para no romper storefront
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
                    System.err.println("=== ERROR INTERNO EN OAUTH2 ===");
                    System.err.println("Motivo: " + exception.getMessage());
                    exception.printStackTrace();
                    response.sendRedirect("/login?error");
                })
            )
            .logout(logout -> logout
                .logoutRequestMatcher(
                    PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/logout")
                )
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/error/403")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS para React.
     *
     * Frontend React local tipico:
     * http://localhost:5173
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        Set<String> origins = new LinkedHashSet<>();
        origins.add(allowedOrigin);
        origins.add("http://127.0.0.1:5173");

        configuration.setAllowedOrigins(new ArrayList<>(origins));

        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With"
        ));

        configuration.setExposedHeaders(List.of(
            "Authorization"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}