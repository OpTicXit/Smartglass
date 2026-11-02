package com.smartglass.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Informe tecnico del panel de administracion.
 *   GET /admin/informe-tecnico        pagina con el globo de emisiones y las graficas del modelo
 *   GET /admin/informe-tecnico/datos  JSON generado por "mineria de datos/exportar_informe.py"
 *
 * Produccion no tiene Python: el JSON se versiona y se reexporta al reentrenar el modelo.
 * SecurityConfig protege /admin/** con ROLE_ADMIN.
 */
@Controller
public class AdminInformeController {

    private static final Resource DATOS = new ClassPathResource("informe/informe-tecnico.json");

    @GetMapping("/admin/informe-tecnico")
    public String informe() {
        return "admin/informe-tecnico";
    }

    @GetMapping("/admin/informe-tecnico/datos")
    public ResponseEntity<Resource> datos() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(DATOS);
    }
}
