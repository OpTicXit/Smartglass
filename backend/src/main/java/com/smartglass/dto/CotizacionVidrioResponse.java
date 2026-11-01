package com.smartglass.dto;

import com.smartglass.model.mongo.VidrioSostenible;

public class CotizacionVidrioResponse {

    private String id;
    private String tipoVidrio;
    private double areaTotal;
    private double pesoEstimado;
    private double huellaCarbono;
    private double energiaRequerida;
    private double precioEstimado;
    private String precioEstimadoFormateado;
    private Double resistenciaEstimada;
    private String recomendacion;
    private String estado;

    public static CotizacionVidrioResponse from(VidrioSostenible vidrio) {
        CotizacionVidrioResponse response = new CotizacionVidrioResponse();
        response.id = vidrio.getId();
        response.tipoVidrio = vidrio.getTipoVidrio();
        response.areaTotal = vidrio.getAreaTotal();
        response.pesoEstimado = vidrio.getPesoEstimado();
        response.huellaCarbono = vidrio.getHuellaCarbono();
        response.energiaRequerida = vidrio.getEnergiaRequerida();
        response.precioEstimado = vidrio.getPrecioEstimado();
        response.precioEstimadoFormateado = vidrio.getPrecioEstimadoFormateado();
        response.resistenciaEstimada = vidrio.getResistenciaEstimada();
        response.recomendacion = vidrio.getRecomendacion();
        response.estado = vidrio.getEstado();
        return response;
    }

    public String getId() { return id; }
    public String getTipoVidrio() { return tipoVidrio; }
    public double getAreaTotal() { return areaTotal; }
    public double getPesoEstimado() { return pesoEstimado; }
    public double getHuellaCarbono() { return huellaCarbono; }
    public double getEnergiaRequerida() { return energiaRequerida; }
    public double getPrecioEstimado() { return precioEstimado; }
    public String getPrecioEstimadoFormateado() { return precioEstimadoFormateado; }
    public Double getResistenciaEstimada() { return resistenciaEstimada; }
    public String getRecomendacion() { return recomendacion; }
    public String getEstado() { return estado; }
}
