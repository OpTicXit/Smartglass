package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * T5 · Plan SaaS de esta instalacion (un solo documento, id "plataforma").
 * - plan: "BASICO" o "AVANZADO".
 * - ajustes: excepciones por funcion sobre lo que trae el plan (true = activa, false = apagada).
 * - historial: quien cambio que y cuando (los ultimos 50 cambios).
 * Lo edita solo el superadmin (SuperadminController); no es una configuracion del admin de la vidrieria.
 */
@Document(collection = "configuracion_plataforma")
public class ConfiguracionPlataforma {

    public static final String ID = "plataforma";

    @Id
    private String id = ID;
    private String plan = "AVANZADO";
    private Map<String, Boolean> ajustes = new LinkedHashMap<>();
    private List<Cambio> historial = new ArrayList<>();

    /** Un cambio del plan o de una funcion. */
    public static class Cambio {
        private LocalDateTime fecha;
        private String usuario;
        private String descripcion;

        public Cambio() { }
        public Cambio(LocalDateTime fecha, String usuario, String descripcion) {
            this.fecha = fecha; this.usuario = usuario; this.descripcion = descripcion;
        }
        public LocalDateTime getFecha() { return fecha; }
        public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public Map<String, Boolean> getAjustes() { return ajustes; }
    public void setAjustes(Map<String, Boolean> ajustes) { this.ajustes = ajustes != null ? ajustes : new LinkedHashMap<>(); }
    public List<Cambio> getHistorial() { return historial; }
    public void setHistorial(List<Cambio> historial) { this.historial = historial != null ? historial : new ArrayList<>(); }
}
