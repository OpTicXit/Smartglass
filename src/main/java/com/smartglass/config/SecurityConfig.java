package com.smartglass.config;

import com.smartglass.security.JwtAuthenticationFilter;
import com.smartglass.security.RateLimitFilter;
import com.smartglass.service.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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
 * - /admin/** y /api/admin/** con hasRole("ADMIN").
 * - API REST de vidrios: GET y POST autenticados; PATCH/DELETE solo ROLE_ADMIN.
 *   Las lecturas GET ademas se filtran por dueno/admin dentro del controller
 *   (F5): las DOS capas son necesarias, una sin la otra deja hueco.
 * - NO hay regla para POST /vidrio-personalizado/aprobar/**: el guard del
 *   controller da un flash amigable; una regla aqui mostraria 403.
 * - CORS habilitado para React; formLogin + oauth2Login se mantienen.
 * F41: RateLimitFilter (429 + Retry-After) tras la autenticacion; ver su javadoc.
 * CAMBIO: las rutas /api/** sin sesion responden 401 (antes redirigian a
 * /login con 302, porque el entry point por defecto es el del formLogin).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomSuccessHandler customSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${smartglass.cors.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    // F41: limites por minuto del RateLimitFilter (configurables en application.properties)
    @Value("${smartglass.ratelimit.anonimo-por-minuto:30}")
    private int rlAnonimo;
    @Value("${smartglass.ratelimit.usuario-por-minuto:120}")
    private int rlUsuario;
    @Value("${smartglass.ratelimit.admin-por-minuto:300}")
    private int rlAdmin;
    @Value("${smartglass.ratelimit.sensible-por-minuto:10}")
    private int rlSensible;
    @Value("${smartglass.ratelimit.confiar-forwarded-for:false}")
    private boolean rlConfiarForwardedFor;

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
        // F41: NO es un bean (Spring Boot lo registraria tambien como filtro de servlet, antes de autenticar)
        RateLimitFilter rateLimitFilter = new RateLimitFilter(
                rlAnonimo, rlUsuario, rlAdmin, rlSensible, rlConfiarForwardedFor);
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
                // API de vidrios (REST)
                .requestMatchers(HttpMethod.GET, "/api/vidrios/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/vidrios/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/vidrios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/vidrios/**").hasRole("ADMIN")
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
                // API REST sin sesion -> 401 (no redirect al login HTML)
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // F41: despues de la autenticacion (JWT/sesion) para distinguir usuario, admin y anonimo
            .addFilterAfter(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS para React (http://localhost:5173 por defecto). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        Set<String> origins = new LinkedHashSet<>();
        origins.add(allowedOrigin);
        origins.add("http://127.0.0.1:5173");
        configuration.setAllowedOrigins(new ArrayList<>(origins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}