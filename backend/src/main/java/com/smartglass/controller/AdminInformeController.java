package com.smartglass.controller;

import com.smartglass.service.ActividadPlataformaService;
import com.smartglass.service.ProyeccionCo2;
import com.smartglass.service.ProyeccionPlataformaService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Informe tecnico del panel de administracion.
 *   GET /admin/informe-tecnico        pagina con el globo de emisiones y las graficas del modelo
 *   GET /admin/informe-tecnico/datos  JSON generado por "mineria de datos/exportar_informe.py"
 *   GET /admin/informe-tecnico/vivo   actividad actual de la plataforma, sumada de lo guardado
 *
 * Produccion no tiene Python: el JSON se versiona y se reexporta al reentrenar el modelo.
 * SecurityConfig protege /admin/** con ROLE_ADMIN.
 */
@Controller
public class AdminInformeController {

    private static final Resource DATOS = new ClassPathResource("informe/informe-tecnico.json");
    private static final Resource SIMULACION = new ClassPathResource("informe/proyeccion-simulacion.json");
    private static final Resource ARBOL = new ClassPathResource("informe/arbol-modelo.json");

    private final ActividadPlataformaService actividad;
    private final ProyeccionPlataformaService proyeccion;

    public AdminInformeController(ActividadPlataformaService actividad, ProyeccionPlataformaService proyeccion) {
        this.actividad = actividad;
        this.proyeccion = proyeccion;
    }

    @GetMapping("/admin/informe-tecnico")
    public String informe() {
        return "admin/informe-tecnico";
    }

    @GetMapping("/admin/informe-tecnico/datos")
    public ResponseEntity<Resource> datos() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(DATOS);
    }

    @GetMapping("/admin/informe-tecnico/vivo")
    @ResponseBody
    public ActividadPlataformaService.Actividad vivo() {
        return actividad.actividad();
    }

    /** "Actividad de la plataforma": cotizaciones guardadas + historial, proyectado en vivo. */
    @GetMapping("/admin/informe-tecnico/proyeccion")
    @ResponseBody
    public ProyeccionCo2.Proyeccion proyeccion() {
        return proyeccion.proyeccion();
    }

    /** Primeras preguntas de un arbol del Random Forest: JSON de "mineria de datos/exportar_arbol.py". */
    @GetMapping("/admin/informe-tecnico/arbol")
    public ResponseEntity<Resource> arbol() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ARBOL);
    }

    /** "Simulacion": JSON de "mineria de datos/exportar_proyeccion.py" (historia sintetica de 5 anios). */
    @GetMapping("/admin/informe-tecnico/proyeccion-simulacion")
    public ResponseEntity<Resource> proyeccionSimulacion() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(SIMULACION);
    }
}
