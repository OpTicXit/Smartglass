package com.smartglass.config;

import org.apache.catalina.Container;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.ErrorReportValve;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Los errores que Tomcat corta antes de llegar a Spring (una URL mal formada: 400) mostraban
 * la pagina generica "HTTP Status 400 - Bad Request". Al arrancar, cada ErrorReportValve de Tomcat
 * recibe como pagina por defecto (errorCode.0) la version estatica con la marca: paginas/error-tomcat.html.
 * Tomcat solo lee archivos del disco, por eso se copia a un temporal.
 */
@Component
public class PaginaErrorTomcat implements ApplicationListener<WebServerInitializedEvent> {

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        if (!(event.getWebServer() instanceof TomcatWebServer tomcat)) return;
        try (InputStream html = new ClassPathResource("paginas/error-tomcat.html").getInputStream()) {
            Path archivo = Files.createTempFile("smartglass-error-", ".html");
            Files.copy(html, archivo, StandardCopyOption.REPLACE_EXISTING);
            archivo.toFile().deleteOnExit();
            String ruta = archivo.toAbsolutePath().toString();
            Container motor = tomcat.getTomcat().getEngine();
            asignar(motor, ruta);
            for (Container host : motor.findChildren()) asignar(host, ruta);
        } catch (IOException e) {
            // sin la pagina propia queda la de Tomcat: no es motivo para no arrancar
        }
    }

    private static void asignar(Container contenedor, String ruta) {
        for (Valve valvula : contenedor.getPipeline().getValves()) {
            if (valvula instanceof ErrorReportValve informe) informe.setProperty("errorCode.0", ruta);
        }
    }
}
