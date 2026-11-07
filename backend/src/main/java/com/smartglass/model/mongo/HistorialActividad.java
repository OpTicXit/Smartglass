package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Un dia de actividad previo a las cotizaciones guardadas: solo lo lee la proyeccion del informe
 * tecnico (ProyeccionPlataformaService). No es una cotizacion, asi que no aparece en ninguna lista.
 * Lo crea HistorialActividadSeeder con DEMO_DATOS=true; "origen" queda solo para uso interno.
 */
@Document(collection = "historial_actividad")
public class HistorialActividad {

    @Id
    private String id;

    private LocalDate fecha;
    private int cotizaciones;
    private double co2Kg;
    private String origen;

    public HistorialActividad() {
    }

    public HistorialActividad(LocalDate fecha, int cotizaciones, double co2Kg, String origen) {
        this.fecha = fecha;
        this.cotizaciones = cotizaciones;
        this.co2Kg = co2Kg;
        this.origen = origen;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public int getCotizaciones() { return cotizaciones; }
    public void setCotizaciones(int cotizaciones) { this.cotizaciones = cotizaciones; }

    public double getCo2Kg() { return co2Kg; }
    public void setCo2Kg(double co2Kg) { this.co2Kg = co2Kg; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
}
