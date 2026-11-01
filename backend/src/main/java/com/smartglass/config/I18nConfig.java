package com.smartglass.config;

import com.smartglass.model.mysql.Usuario;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * T0 i18n de TEXTOS: es-CO (por defecto, hogar del contrato), es-ES y en.
 *
 * - Locale por cookie "sg-lang" (un ano). Se cambia con ?lang=es-CO|es-ES|en en
 *   cualquier URL; un valor no soportado se ignora.
 * - Usuario con sesion y sin cookie (p. ej. otro dispositivo): se aplica su
 *   preferencia guardada en usuarios.preferencia_idioma.
 * - Regla D2: SOLO textos. Los numeros del contrato no pasan por el locale: las
 *   vistas usan #numbers.formatDecimal(...,'POINT',...,'COMMA') y SGGlass.fmtNum,
 *   con separadores es-CO explicitos.
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    public static final String COOKIE = "sg-lang";
    public static final String PARAM = "lang";
    public static final Locale POR_DEFECTO = Locale.forLanguageTag("es-CO");
    public static final List<Locale> SOPORTADOS = List.of(
            POR_DEFECTO, Locale.forLanguageTag("es-ES"), Locale.forLanguageTag("en"));

    private final UsuarioSesion usuarioSesion;
    // T5: se resuelve tarde para no acoplar el arranque de la configuracion web a Mongo
    private final org.springframework.beans.factory.ObjectProvider<com.smartglass.service.PlanService> plan;

    public I18nConfig(UsuarioSesion usuarioSesion,
                      org.springframework.beans.factory.ObjectProvider<com.smartglass.service.PlanService> plan) {
        this.usuarioSesion = usuarioSesion;
        this.plan = plan;
    }

    /** T5: sin MULTI_IDIOMA (plan Basico) la tienda queda en es-CO y el selector se oculta. */
    private boolean multiIdioma() {
        com.smartglass.service.PlanService p = plan.getIfAvailable();
        return p == null || p.activa(com.smartglass.service.Funcion.MULTI_IDIOMA);
    }

    /** "es-CO" / "es_es" / "EN" -> Locale soportado; vacio si no se reconoce. */
    public static Optional<Locale> soportado(String etiqueta) {
        if (etiqueta == null || etiqueta.isBlank()) return Optional.empty();
        Locale pedido = Locale.forLanguageTag(etiqueta.trim().replace('_', '-'));
        return SOPORTADOS.stream()
                .filter(l -> l.toLanguageTag().equalsIgnoreCase(pedido.toLanguageTag()))
                .findFirst();
    }

    /**
     * Declarado aqui (y no por spring.messages.*) a proposito: la autoconfiguracion
     * de Spring Boot decide si crea el MessageSource segun exista messages.properties
     * y cachea esa decision en una cache estatica que sobrevive a los reinicios de
     * devtools; con el bean explicito los textos cargan siempre.
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding(StandardCharsets.UTF_8.name());
        ms.setFallbackToSystemLocale(false);   // clave sin traducir -> messages.properties (es-CO)
        return ms;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver(COOKIE);
        resolver.setDefaultLocale(POR_DEFECTO);
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setCookieSameSite("Lax");
        return resolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleResolver resolver = localeResolver();
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                // 0) Plan sin varios idiomas: siempre es-CO (tambien si quedo una cookie de antes)
                if (!multiIdioma()) {
                    if (!POR_DEFECTO.equals(resolver.resolveLocale(req))) resolver.setLocale(req, res, POR_DEFECTO);
                    return true;
                }
                // 1) Cambio explicito por parametro (?lang=en)
                Optional<Locale> pedido = soportado(req.getParameter(PARAM));
                if (pedido.isPresent()) {
                    resolver.setLocale(req, res, pedido.get());
                    return true;
                }
                // 2) Sin cookie y con sesion: la preferencia guardada en la cuenta
                if (!tieneCookie(req)) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (UsuarioSesion.autenticado(auth)) {
                        usuarioSesion.buscar(auth)
                                .map(Usuario::getPreferenciaIdioma)
                                .flatMap(I18nConfig::soportado)
                                .ifPresent(l -> resolver.setLocale(req, res, l));
                    }
                }
                return true;
            }
        }).excludePathPatterns("/css/**", "/js/**", "/img/**", "/vendor/**", "/favicon.ico");
    }

    private static boolean tieneCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return false;
        for (Cookie c : cookies) if (COOKIE.equals(c.getName())) return true;
        return false;
    }
}
