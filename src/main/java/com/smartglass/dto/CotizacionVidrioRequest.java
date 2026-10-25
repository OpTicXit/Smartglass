package com.smartglass.dto;

import java.util.List;

public class CotizacionVidrioRequest {

    private Long usuarioId;
    private String tipoVidrio;
    private double ancho;
    private double alto;
    private double espesor;
    private String forma;
    private String tratamiento;
    private String color;
    private int cantidadCapas = 1; // F27: una pieza simple tiene 1 capa
    private boolean requiereCorteEspecial;
    private String notasAdicionales;

    // Campos del formulario real (vidrio-personalizado-form.html) que
    // el AnalisisService todavia no usa en el calculo -- se capturan
    // igual para no perderlos silenciosamente al hacer el bind.
    private String usoPrincipal;
    private String acabadoBordes;
    private int perforaciones;

    private boolean usarVidrioReciclado;
    private int porcentajeReciclado;
    private boolean energiaRenovable;
    private boolean compensarCarbono;
    private boolean urgente;

    // T3: compromisos sostenibles sin costo (no entran en AnalisisService ni en el precio)
    private boolean retomaVidrio;
    private boolean empaqueReutilizable;
    private boolean entregaConsolidada;
    private boolean usarRetazos;

    // F45: vertices del corte personalizado (solo se usan si forma = "Personalizada").
    // La validacion vive en AnalisisService (cae a bbox si son invalidos), no en anotaciones del DTO.
    private List<PuntoCorte> puntosPersonalizados;

    public CotizacionVidrioRequest() {
    }

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

    public boolean isUsarVidrioReciclado() { return usarVidrioReciclado; }
    public void setUsarVidrioReciclado(boolean usarVidrioReciclado) { this.usarVidrioReciclado = usarVidrioReciclado; }

    public int getPorcentajeReciclado() { return porcentajeReciclado; }
    public void setPorcentajeReciclado(int porcentajeReciclado) { this.porcentajeReciclado = porcentajeReciclado; }

    public boolean isEnergiaRenovable() { return energiaRenovable; }
    public void setEnergiaRenovable(boolean energiaRenovable) { this.energiaRenovable = energiaRenovable; }

    public boolean isCompensarCarbono() { return compensarCarbono; }
    public void setCompensarCarbono(boolean compensarCarbono) { this.compensarCarbono = compensarCarbono; }

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

    public List<PuntoCorte> getPuntosPersonalizados() { return puntosPersonalizados; }
    public void setPuntosPersonalizados(List<PuntoCorte> puntosPersonalizados) { this.puntosPersonalizados = puntosPersonalizados; }

    /** Entrada del formulario HTML: campo oculto "puntosTexto" con "x,y;x,y;..." (formato de PuntoCorte). */
    public void setPuntosTexto(String puntosTexto) { this.puntosPersonalizados = PuntoCorte.desdeTexto(puntosTexto); }
}