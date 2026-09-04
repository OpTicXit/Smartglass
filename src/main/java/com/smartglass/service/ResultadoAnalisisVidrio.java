package com.smartglass.service;

/**
 * Resultado de un calculo de eficiencia energetica / huella de CO2
 * para una cotizacion de vidrio. Es un objeto de transporte interno
 * entre AnalisisService y VidrioSostenibleService; no se expone
 * directamente por la API (para eso esta CotizacionVidrioResponse).
 */
public class ResultadoAnalisisVidrio {

    private double areaTotal;
    private double pesoEstimado;
    private double huellaCarbono;
    private double energiaRequerida;
    private double cantidadSilice;
    private double cantidadSosa;
    private double cantidadCaliza;
    private double cantidadAlumina;
    private double precioEstimado;
    private double resistenciaEstimada;
    private String recomendacion;

    public double getAreaTotal() { return areaTotal; }
    public void setAreaTotal(double areaTotal) { this.areaTotal = areaTotal; }

    public double getPesoEstimado() { return pesoEstimado; }
    public void setPesoEstimado(double pesoEstimado) { this.pesoEstimado = pesoEstimado; }

    public double getHuellaCarbono() { return huellaCarbono; }
    public void setHuellaCarbono(double huellaCarbono) { this.huellaCarbono = huellaCarbono; }

    public double getEnergiaRequerida() { return energiaRequerida; }
    public void setEnergiaRequerida(double energiaRequerida) { this.energiaRequerida = energiaRequerida; }

    public double getCantidadSilice() { return cantidadSilice; }
    public void setCantidadSilice(double cantidadSilice) { this.cantidadSilice = cantidadSilice; }

    public double getCantidadSosa() { return cantidadSosa; }
    public void setCantidadSosa(double cantidadSosa) { this.cantidadSosa = cantidadSosa; }

    public double getCantidadCaliza() { return cantidadCaliza; }
    public void setCantidadCaliza(double cantidadCaliza) { this.cantidadCaliza = cantidadCaliza; }

    public double getCantidadAlumina() { return cantidadAlumina; }
    public void setCantidadAlumina(double cantidadAlumina) { this.cantidadAlumina = cantidadAlumina; }

    public double getPrecioEstimado() { return precioEstimado; }
    public void setPrecioEstimado(double precioEstimado) { this.precioEstimado = precioEstimado; }

    public double getResistenciaEstimada() { return resistenciaEstimada; }
    public void setResistenciaEstimada(double resistenciaEstimada) { this.resistenciaEstimada = resistenciaEstimada; }

    public String getRecomendacion() { return recomendacion; }
    public void setRecomendacion(String recomendacion) { this.recomendacion = recomendacion; }
}
