package com.smartglass.model.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.smartglass.dto.PuntoCorte;

import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.List;
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

    // --- Umbrales de intensidad de carbono (kg CO2e por m2) ---
    // Compartidos con el configurador del frontend: <=12 Favorable,
    // <=20 Moderado, >20 Alto. Si cambian aqui, cambian tambien alla.
    public static final double UMBRAL_FAVORABLE = 12.0;
    public static final double UMBRAL_MODERADO = 20.0;

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
    private int cantidadCapas = 1; // F27
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
    private double cantidadOtros;
    private double energiaRequerida;
    private boolean usarVidrioReciclado;
    private int porcentajeReciclado;
    private boolean energiaRenovable;
    private boolean compensarCarbono;
    private boolean urgente;
    // T3: compromisos sostenibles sin costo (no cambian CO2 ni precio). Las obras anteriores los leen como false.
    private boolean retomaVidrio;
    private boolean empaqueReutilizable;
    private boolean entregaConsolidada;
    private boolean usarRetazos;
    private double precioEstimado;
    private Double resistenciaEstimada;
    private String recomendacion;
    private LocalDateTime fechaCreacion;
    private String estado;

    // F45: corte personalizado (nullable; solo existe si forma = "Personalizada" y el poligono fue valido)
    private List<PuntoCorte> puntosPersonalizados;
    private Double areaPoligonal;   // m2, calculada por AnalisisService y guardada para no recalcular en cada vista

    public VidrioSostenible() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = "COTIZACION";
    }

    // --- Metodos de apoyo para la vista ---

    public String getPrecioEstimadoFormateado() {
        NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        return formato.format(this.precioEstimado);
    }

    /**
     * Intensidad de carbono en kg CO2e por m2 (huella / area).
     * No depende del tamano de la pieza: el area se cancela y queda
     * funcion de espesor, tipo y opciones verdes.
     */
    public double getIntensidadCarbono() {
        if (this.areaTotal <= 0) return 0.0;
        return Math.round((this.huellaCarbono / this.areaTotal) * 100.0) / 100.0;
    }

    /**
     * Nivel de impacto segun los umbrales compartidos con el
     * frontend: FAVORABLE, MODERADO o ALTO. Permite que Thymeleaf
     * pinte chips y anillos sin duplicar la logica en JavaScript:
     * th:text="${cot.nivelImpacto}" / th:if="${cot.nivelImpacto == 'ALTO'}".
     */
    public String getNivelImpacto() {
        double intensidad = getIntensidadCarbono();
        if (intensidad <= UMBRAL_FAVORABLE) return "FAVORABLE";
        if (intensidad <= UMBRAL_MODERADO) return "MODERADO";
        return "ALTO";
    }

    /**
     * Kg de vidrio reciclado (calcin) de la pieza: 15% del peso cuando se
     * usa material reciclado. Getter transitorio (no se persiste). Con la
     * materia virgen y "otros oxidos", la suma de la anatomia = pesoEstimado.
     */
    public double getVidrioRecicladoKg() {
        if (!usarVidrioReciclado || pesoEstimado <= 0) return 0.0;
        return Math.round(pesoEstimado * 0.15 * 100.0) / 100.0;
    }

    /** F45: vertices como "x,y;x,y" para el atributo data-puntos del detalle (no se persiste; no depende del locale). */
    public String getPuntosTexto() {
        return PuntoCorte.aTexto(puntosPersonalizados);
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
    public double getCantidadOtros() { return cantidadOtros; }
    public void setCantidadOtros(double v) { this.cantidadOtros = v; }
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

    public boolean isRetomaVidrio() { return retomaVidrio; }
    public void setRetomaVidrio(boolean retomaVidrio) { this.retomaVidrio = retomaVidrio; }

    public boolean isEmpaqueReutilizable() { return empaqueReutilizable; }
    public void setEmpaqueReutilizable(boolean empaqueReutilizable) { this.empaqueReutilizable = empaqueReutilizable; }

    public boolean isEntregaConsolidada() { return entregaConsolidada; }
    public void setEntregaConsolidada(boolean entregaConsolidada) { this.entregaConsolidada = entregaConsolidada; }

    public boolean isUsarRetazos() { return usarRetazos; }
    public void setUsarRetazos(boolean usarRetazos) { this.usarRetazos = usarRetazos; }

    /** T3: puntos del sello "Compromiso verde" (0-100). Derivado, no se guarda. */
    public int getCompromisoPuntos() {
        return com.smartglass.service.CompromisoVerde.puntos(usarVidrioReciclado, energiaRenovable, compensarCarbono,
                retomaVidrio, empaqueReutilizable, entregaConsolidada, usarRetazos);
    }

    /** T3: "ORO", "PLATA", "BRONCE" o "" (sin sello). */
    public String getCompromisoNivel() {
        return com.smartglass.service.CompromisoVerde.nivel(getCompromisoPuntos());
    }
    public Double getResistenciaEstimada() { return resistenciaEstimada; }
    public void setResistenciaEstimada(Double resistenciaEstimada) { this.resistenciaEstimada = resistenciaEstimada; }
    public String getRecomendacion() { return recomendacion; }
    public void setRecomendacion(String recomendacion) { this.recomendacion = recomendacion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public List<PuntoCorte> getPuntosPersonalizados() { return puntosPersonalizados; }
    public void setPuntosPersonalizados(List<PuntoCorte> puntosPersonalizados) { this.puntosPersonalizados = puntosPersonalizados; }
    public Double getAreaPoligonal() { return areaPoligonal; }
    public void setAreaPoligonal(Double areaPoligonal) { this.areaPoligonal = areaPoligonal; }
}