package com.smartglass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.time.Duration;

/**
 * U2 · Estaticos con un manejador por carpeta (css, js, vendor; /img lo sirve UploadsWebConfig).
 *
 * Antes servia Spring Boot con el patron "/**" (spring.web.resources.add-mappings=false lo apaga).
 * Con el versionado de prod, el filtro que reescribe las URLs (@{...}) consultaba ese "/**" con cada
 * enlace de la pagina: /detalle/..., /carrito/agregar... no son archivos, se buscaban en el JAR sin
 * encontrarse y esa busqueda no se recuerda. El catalogo (27 tarjetas) pasaba de ~20 a ~90 ms.
 * Con patrones concretos solo se resuelven las rutas que de verdad son archivos.
 *
 * Mismo comportamiento que antes: en desarrollo sin cache ni versionado; en prod (application-prod)
 * cache de 7 dias y version por contenido (theme-<hash>.css, tambien las fuentes dentro del CSS).
 */
@Configuration
public class EstaticosWebConfig implements WebMvcConfigurer {

    static final String[] CARPETAS = { "css", "js", "vendor" };

    private final Duration cache;
    private final boolean versionado;

    public EstaticosWebConfig(@Value("${spring.web.resources.cache.period:0s}") Duration cache,
                              @Value("${spring.web.resources.chain.strategy.content.enabled:false}") boolean versionado) {
        this.cache = cache;
        this.versionado = versionado;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        for (String carpeta : CARPETAS) {
            var registro = registry.addResourceHandler("/" + carpeta + "/**")
                    .addResourceLocations("classpath:/static/" + carpeta + "/");
            if (!cache.isZero()) registro.setCacheControl(CacheControl.maxAge(cache).cachePublic());
            if (versionado) {
                registro.resourceChain(true)
                        .addResolver(new VersionResourceResolver().addContentVersionStrategy("/**"));
            }
        }
    }
}
