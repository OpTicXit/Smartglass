package com.smartglass.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
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
 */
@Configuration
public class UploadsWebConfig implements WebMvcConfigurer {

    private final String carpeta;

    public UploadsWebConfig(@Value("${smartglass.uploads.dir:uploads}") String carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String fotos = java.nio.file.Paths.get(carpeta, "img").toAbsolutePath().toUri().toString();
        if (!fotos.endsWith("/")) fotos += "/";   // toUri() solo pone la barra si la carpeta ya existe
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/", fotos);
    }
}
