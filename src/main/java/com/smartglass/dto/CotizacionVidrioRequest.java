package com.smartglass.dto;

public class CotizacionVidrioRequest {

    private Long usuarioId;
    private String tipoVidrio;
    private double ancho;
    private double alto;
    private double espesor;
    private String forma;
    private String tratamiento;
    private String color;
    private int cantidadCapas;
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
}