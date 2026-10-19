package com.smartglass.config;

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
 */
@Configuration
public class UploadsWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/", "file:uploads/img/");
    }
}
