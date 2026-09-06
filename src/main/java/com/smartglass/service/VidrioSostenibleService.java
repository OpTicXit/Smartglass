package com.smartglass.service;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Capa de persistencia/orquestacion para las cotizaciones de vidrio
 * sostenible. Traduce el DTO de entrada a la entidad, delega TODO el
 * calculo matematico a AnalisisService, y guarda el resultado.
 * VidrioPersonalizadoController solo habla con esta clase; nunca
 * toca el repositorio ni AnalisisService directamente.
 */
@Service
public class VidrioSostenibleService {

    private final VidrioSostenibleRepository vidrioSostenibleRepository;
    private final AnalisisService analisisService;

    public VidrioSostenibleService(VidrioSostenibleRepository vidrioSostenibleRepository,
                                    AnalisisService analisisService) {
        this.vidrioSostenibleRepository = vidrioSostenibleRepository;
        this.analisisService = analisisService;
    }

    public VidrioSostenible generarCotizacion(CotizacionVidrioRequest request) {
        VidrioSostenible vidrio = new VidrioSostenible();
        vidrio.setUsuarioId(request.getUsuarioId());
        vidrio.setTipoVidrio(request.getTipoVidrio());
        vidrio.setAncho(request.getAncho());
        vidrio.setAlto(request.getAlto());
        vidrio.setEspesor(request.getEspesor());
        vidrio.setForma(request.getForma());
        vidrio.setTratamiento(request.getTratamiento());
        vidrio.setColor(request.getColor());
        vidrio.setCantidadCapas(request.getCantidadCapas());
        vidrio.setRequiereCorteEspecial(request.isRequiereCorteEspecial());
        vidrio.setNotasAdicionales(request.getNotasAdicionales());
        vidrio.setUsoPrincipal(request.getUsoPrincipal());
        vidrio.setAcabadoBordes(request.getAcabadoBordes());
        vidrio.setPerforaciones(request.getPerforaciones());
        vidrio.setUsarVidrioReciclado(request.isUsarVidrioReciclado());
        vidrio.setPorcentajeReciclado(request.getPorcentajeReciclado());
        vidrio.setEnergiaRenovable(request.isEnergiaRenovable());
        vidrio.setCompensarCarbono(request.isCompensarCarbono());
        vidrio.setUrgente(request.isUrgente());

        ResultadoAnalisisVidrio resultado = analisisService.calcularCotizacionVidrio(vidrio);

        vidrio.setAreaTotal(resultado.getAreaTotal());
        vidrio.setPesoEstimado(resultado.getPesoEstimado());
        vidrio.setHuellaCarbono(resultado.getHuellaCarbono());
        vidrio.setEnergiaRequerida(resultado.getEnergiaRequerida());
        vidrio.setCantidadSilice(resultado.getCantidadSilice());
        vidrio.setCantidadSosa(resultado.getCantidadSosa());
        vidrio.setCantidadCaliza(resultado.getCantidadCaliza());
        vidrio.setCantidadAlumina(resultado.getCantidadAlumina());
        vidrio.setPrecioEstimado(resultado.getPrecioEstimado());
        vidrio.setResistenciaEstimada(resultado.getResistenciaEstimada());
        vidrio.setRecomendacion(resultado.getRecomendacion());

        return vidrioSostenibleRepository.save(vidrio);
    }

    public Optional<VidrioSostenible> obtenerPorId(String id) {
        return vidrioSostenibleRepository.findById(id);
    }

    public List<VidrioSostenible> obtenerPorUsuario(Long usuarioId) {
        return vidrioSostenibleRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    public List<VidrioSostenible> obtenerPorEstado(String estado) {
        return vidrioSostenibleRepository.findByEstado(estado);
    }

    public Optional<VidrioSostenible> actualizarEstado(String id, String estado) {
        return vidrioSostenibleRepository.findById(id).map(vidrio -> {
            vidrio.setEstado(estado);
            return vidrioSostenibleRepository.save(vidrio);
        });
    }

    public void eliminarPorId(String id) {
        vidrioSostenibleRepository.deleteById(id);
    }
}