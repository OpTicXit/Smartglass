package com.smartglass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve las fotos de productos subidas desde el panel admin.
 *
 * Los archivos que se crean mientras la app corre no aparecen en classpath:/static,
 * asi que se guardan en la carpeta "uploads/img/" (junto al proyecto) y /img/**
 * busca primero en static/img/ y luego ahi. Las vistas no cambian: siguen usando
 * @{/img/{nombre}}.
 *
 * SecurityConfig ya tiene /img/** como publico.
 * T6: la carpeta base sale de smartglass.uploads.dir (UPLOADS_DIR); en produccion es un volumen persistente.
 * U2: este manejador propio no heredaba spring.web.resources.cache.period, y Spring Security marcaba las
 * imagenes como no-store (se bajaban de nuevo en cada pagina). Ahora usan el mismo periodo que CSS y JS
 * (7 dias en prod; en desarrollo no hay periodo y todo sigue sin cache). Las fotos subidas llevan UUID.
 */
@Configuration
public class UploadsWebConfig implements WebMvcConfigurer {

    private final String carpeta;
    private final java.time.Duration cache;

    public UploadsWebConfig(@Value("${smartglass.uploads.dir:uploads}") String carpeta,
                            @Value("${spring.web.resources.cache.period:0s}") java.time.Duration cache) {
        this.carpeta = carpeta;
        this.cache = cache;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String fotos = java.nio.file.Paths.get(carpeta, "img").toAbsolutePath().toUri().toString();
        if (!fotos.endsWith("/")) fotos += "/";   // toUri() solo pone la barra si la carpeta ya existe
        var imagenes = registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/", fotos);
        if (!cache.isZero()) {
            imagenes.setCacheControl(CacheControl.maxAge(cache).cachePublic());
            imagenes.resourceChain(true);   // recuerda donde esta cada imagen (antes se buscaba en el JAR por cada <img>)
        }
    }
}
