package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Cotizacion/requerimiento tecnico de un vidrio sostenible,
 * almacenado en MongoDB. Renombrado desde "VidrioPersonalizado" a
 * "VidrioSostenible". Incluye tanto las medidas fisicas del vidrio
 * como los datos de huella de CO2 y materiales usados en su
 * fabricacion.
 */
@Document(collection = "vidrios_sostenibles")
public class VidrioSostenible {

    @Id
    private String id;

    // Referencia al usuario relacional (MySQL); se guarda como Long
    // plano porque las relaciones entre Mongo y MySQL no se modelan
    // con @DBRef sino a nivel de aplicacion.
    private Long usuarioId;

    private String tipoVidrio;

    // --- Medidas ---
    private double ancho;
    private double alto;
    private double espesor;
    private String forma;
    private String tratamiento;
    private String color;
    private int cantidadCapas;
    private boolean requiereCorteEspecial;
    private double areaTotal;
    private double pesoEstimado;

    // Campos del formulario real que AnalisisService todavia no usa
    // en el calculo -- se guardan para no perderlos, y quedan listos
    // para cuando se decida incorporarlos a la formula.
    private String usoPrincipal;
    private String acabadoBordes;
    private int perforaciones;

    private String notasAdicionales;

    // --- Huella de CO2 y materiales ---
    private double huellaCarbono;
    private double cantidadSilice;
    private double cantidadSosa;
    private double cantidadCaliza;
    private double cantidadAlumina;
    private double energiaRequerida;
    private boolean usarVidrioReciclado;
    private int porcentajeReciclado;
    private boolean energiaRenovable;
    private boolean compensarCarbono;
    private boolean urgente;

    private double precioEstimado;
    private Double resistenciaEstimada;
    private String recomendacion;

    private LocalDateTime fechaCreacion;
    private String estado;

    public VidrioSostenible() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "COTIZACION";
    }

    // --- Metodos de apoyo para la vista ---
    public String getPrecioEstimadoFormateado() {
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        return formato.format(this.precioEstimado);
    }

    // --- Getters y Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTipoVidrio() { return tipoVidrio; }
    public void setTipoVidrio(String tipoVidrio) { this.tipoVidrio = tipoVidrio; }

    public double getAncho() { return ancho; }
    public void setAncho(double ancho) { this.ancho = ancho; }

    public double getAlto() { return alto; }
    public void setAlto(double alto) { this.alto = alto; }

    public double getEspesor() { return espesor; }
    public void setEspesor(double espesor) { this.espesor = espesor; }

    public String getForma() { return forma; }
    public void setForma(String forma) { this.forma = forma; }

    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String tratamiento) { this.tratamiento = tratamiento; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getCantidadCapas() { return cantidadCapas; }
    public void setCantidadCapas(int cantidadCapas) { this.cantidadCapas = cantidadCapas; }

    public boolean isRequiereCorteEspecial() { return requiereCorteEspecial; }
    public void setRequiereCorteEspecial(boolean requiereCorteEspecial) { this.requiereCorteEspecial = requiereCorteEspecial; }

    public String getNotasAdicionales() { return notasAdicionales; }
    public void setNotasAdicionales(String notasAdicionales) { this.notasAdicionales = notasAdicionales; }

    public String getUsoPrincipal() { return usoPrincipal; }
    public void setUsoPrincipal(String usoPrincipal) { this.usoPrincipal = usoPrincipal; }

    public String getAcabadoBordes() { return acabadoBordes; }
    public void setAcabadoBordes(String acabadoBordes) { this.acabadoBordes = acabadoBordes; }

    public int getPerforaciones() { return perforaciones; }
    public void setPerforaciones(int perforaciones) { this.perforaciones = perforaciones; }

    public double getAreaTotal() { return areaTotal; }
    public void setAreaTotal(double areaTotal) { this.areaTotal = areaTotal; }

    public double getPesoEstimado() { return pesoEstimado; }
    public void setPesoEstimado(double pesoEstimado) { this.pesoEstimado = pesoEstimado; }

    public double getHuellaCarbono() { return huellaCarbono; }
    public void setHuellaCarbono(double huellaCarbono) { this.huellaCarbono = huellaCarbono; }

    public double getPrecioEstimado() { return precioEstimado; }
    public void setPrecioEstimado(double precioEstimado) { this.precioEstimado = precioEstimado; }

    public double getCantidadSilice() { return cantidadSilice; }
    public void setCantidadSilice(double v) { this.cantidadSilice = v; }

    public double getCantidadSosa() { return cantidadSosa; }
    public void setCantidadSosa(double v) { this.cantidadSosa = v; }

    public double getCantidadCaliza() { return cantidadCaliza; }
    public void setCantidadCaliza(double v) { this.cantidadCaliza = v; }

    public double getCantidadAlumina() { return cantidadAlumina; }
    public void setCantidadAlumina(double v) { this.cantidadAlumina = v; }

    public double getEnergiaRequerida() { return energiaRequerida; }
    public void setEnergiaRequerida(double v) { this.energiaRequerida = v; }

    public boolean isUsarVidrioReciclado() { return usarVidrioReciclado; }
    public void setUsarVidrioReciclado(boolean v) { this.usarVidrioReciclado = v; }

    public int getPorcentajeReciclado() { return porcentajeReciclado; }
    public void setPorcentajeReciclado(int v) { this.porcentajeReciclado = v; }

    public boolean isEnergiaRenovable() { return energiaRenovable; }
    public void setEnergiaRenovable(boolean v) { this.energiaRenovable = v; }

    public boolean isCompensarCarbono() { return compensarCarbono; }
    public void setCompensarCarbono(boolean v) { this.compensarCarbono = v; }

    public boolean isUrgente() { return urgente; }
    public void setUrgente(boolean urgente) { this.urgente = urgente; }

    public Double getResistenciaEstimada() { return resistenciaEstimada; }
    public void setResistenciaEstimada(Double resistenciaEstimada) { this.resistenciaEstimada = resistenciaEstimada; }

    public String getRecomendacion() { return recomendacion; }
    public void setRecomendacion(String recomendacion) { this.recomendacion = recomendacion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}