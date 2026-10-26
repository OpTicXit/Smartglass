package com.smartglass.service;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.repository.mongo.VidrioSostenibleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Capa de persistencia/orquestacion para las cotizaciones de vidrio
 * sostenible. Traduce el DTO de entrada a la entidad, delega el
 * calculo matematico a AnalisisService y guarda el resultado.
 *
 * - Constantes de estado COTIZACION / APROBADO / RECHAZADA.
 * - enum ResultadoCambio y cambiarEstado(id, estado): maquina de
 *   estados COTIZACION -> APROBADO | RECHAZADA ; RECHAZADA -> APROBADO.
 * - obtenerTodas(): todas las cotizaciones, mas reciente primero.
 * - actualizarEstado(...) @Deprecated: ahora delega en cambiarEstado.
 */
@Service
public class VidrioSostenibleService {

    /** Estados de la maquina de cotizaciones (fuente unica de verdad). */
    public static final String COTIZACION = "COTIZACION";
    public static final String APROBADO   = "APROBADO";
    public static final String RECHAZADA  = "RECHAZADA";

    /** Resultado de una transicion de estado pedida por controllers/panel. */
    public enum ResultadoCambio { OK, NO_ENCONTRADA, TRANSICION_INVALIDA }

    /**
     * T2: geometrias que ofrece el configurador (mismos codigos que glass-preview.js).
     * Solo presentacion: todas se cotizan por caja envolvente salvo "Personalizada" (F45).
     * Un valor desconocido se guarda como "Rectangular" en vez de texto libre.
     */
    public static final List<String> FORMAS = List.of(
            "Rectangular", "EsquinasRedondeadas", "Circular", "Capsula", "Arco", "Triangular",
            "TrianguloRectangulo", "TrapecioRectangulo", "Fronton", "Rombo", "Hexagonal", "Octogonal",
            "Irregular", "Personalizada");

    /** T5: una forma que el plan no incluye se cotiza como Rectangular (mismo criterio que un valor desconocido). */
    String formaSegunPlan(String forma) {
        if ("Personalizada".equals(forma) && !plan.activa(Funcion.CORTE_PERSONALIZADO)) return "Rectangular";
        if (!FORMAS_CLASICAS.contains(forma) && !plan.activa(Funcion.FORMAS_AVANZADAS)) return "Rectangular";
        return forma;
    }

    static String normalizarForma(String forma) {
        if (forma == null) return "Rectangular";
        String f = forma.trim();
        return FORMAS.stream().filter(x -> x.equalsIgnoreCase(f)).findFirst().orElse("Rectangular");
    }

    private final VidrioSostenibleRepository vidrioSostenibleRepository;
    private final AnalisisService analisisService;
    private final PlanService plan;

    /** T5: las 5 geometrias de siempre; las demas son de FORMAS_AVANZADAS ("Personalizada" va aparte). */
    static final List<String> FORMAS_CLASICAS = List.of("Rectangular", "Circular", "Triangular", "Irregular", "Personalizada");

    public VidrioSostenibleService(VidrioSostenibleRepository vidrioSostenibleRepository,
                                   AnalisisService analisisService, PlanService plan) {
        this.plan = plan;
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
        vidrio.setForma(formaSegunPlan(normalizarForma(request.getForma())));
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
        // T3: compromisos sin costo; la entrega consolidada no es compatible con la urgente
        boolean compromisos = plan.activa(Funcion.COMPROMISOS_VERDES);   // T5: plan Avanzado
        vidrio.setRetomaVidrio(compromisos && request.isRetomaVidrio());
        vidrio.setEmpaqueReutilizable(compromisos && request.isEmpaqueReutilizable());
        vidrio.setEntregaConsolidada(compromisos && request.isEntregaConsolidada() && !request.isUrgente());
        vidrio.setUsarRetazos(compromisos && request.isUsarRetazos());
        // F45: los vertices deben estar puestos ANTES del analisis (AnalisisService decide si son validos)
        vidrio.setPuntosPersonalizados("Personalizada".equals(vidrio.getForma()) ? request.getPuntosPersonalizados() : null);

        ResultadoAnalisisVidrio resultado = analisisService.calcularCotizacionVidrio(vidrio);
        vidrio.setAreaTotal(resultado.getAreaTotal());
        vidrio.setPesoEstimado(resultado.getPesoEstimado());
        vidrio.setHuellaCarbono(resultado.getHuellaCarbono());
        vidrio.setHuellaEtapas(resultado.getHuellaEtapas());          // U4: desglose y version del metodo
        vidrio.setMetodoHuella(AnalisisService.METODO_HUELLA);
        vidrio.setEnergiaRequerida(resultado.getEnergiaRequerida());
        vidrio.setCantidadSilice(resultado.getCantidadSilice());
        vidrio.setCantidadSosa(resultado.getCantidadSosa());
        vidrio.setCantidadCaliza(resultado.getCantidadCaliza());
        vidrio.setCantidadAlumina(resultado.getCantidadAlumina());
        vidrio.setCantidadOtros(resultado.getCantidadOtros());
        vidrio.setPrecioEstimado(resultado.getPrecioEstimado());
        vidrio.setResistenciaEstimada(resultado.getResistenciaEstimada());
        vidrio.setRecomendacion(resultado.getRecomendacion());
        // F45: solo se guarda el poligono si AnalisisService lo uso; si era basura (o no es "Personalizada")
        // se descarta y la pieza queda cotizada por caja envolvente, igual que las formas fijas.
        if (resultado.isCorteAplicado()) {
            vidrio.setAreaPoligonal(resultado.getAreaTotal());
        } else {
            vidrio.setPuntosPersonalizados(null);
            vidrio.setAreaPoligonal(null);
        }
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

    public long contarPorEstado(String estado) {
        return vidrioSostenibleRepository.countByEstado(estado);
    }

    /** Todas las cotizaciones de todos los usuarios, mas reciente primero (panel admin). */
    public List<VidrioSostenible> obtenerTodas() {
        return vidrioSostenibleRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaCreacion"));
    }

    /**
     * Maquina de estados: COTIZACION -> APROBADO | RECHAZADA ;
     * RECHAZADA -> APROBADO (re-aprobacion, solo panel).
     * APROBADO es terminal. No toca carrito ni pagos.
     */
    public ResultadoCambio cambiarEstado(String id, String nuevoEstado) {
        Optional<VidrioSostenible> opt = vidrioSostenibleRepository.findById(id);
        if (opt.isEmpty()) return ResultadoCambio.NO_ENCONTRADA;
        VidrioSostenible v = opt.get();
        String actual = v.getEstado() == null ? "" : v.getEstado().toUpperCase();
        String objetivo = nuevoEstado == null ? "" : nuevoEstado.toUpperCase();
        boolean permitido =
                (COTIZACION.equals(actual) && (APROBADO.equals(objetivo) || RECHAZADA.equals(objetivo)))
             || (RECHAZADA.equals(actual) && APROBADO.equals(objetivo));
        if (!permitido) return ResultadoCambio.TRANSICION_INVALIDA;
        v.setEstado(objetivo);
        vidrioSostenibleRepository.save(v);
        return ResultadoCambio.OK;
    }

    /**
     * @deprecated usar {@link #cambiarEstado(String, String)}. Ahora delega en
     * la maquina de estados: una transicion ilegal o un id inexistente
     * devuelven Optional.empty().
     */
    @Deprecated
    public Optional<VidrioSostenible> actualizarEstado(String id, String estado) {
        return cambiarEstado(id, estado) == ResultadoCambio.OK
                ? vidrioSostenibleRepository.findById(id)
                : Optional.empty();
    }

    public void eliminarPorId(String id) {
        vidrioSostenibleRepository.deleteById(id);
    }

    /**
     * T1 (D4): al autenticarse, las obras que la MISMA sesion cotizo como anonima
     * pasan a ser del usuario. Solo las que siguen sin dueno (usuarioId null): una
     * obra con dueno nunca cambia de manos. No toca estado ni calculo.
     */
    public int reclamarCotizacionesAnonimas(Collection<String> ids, Long usuarioId) {
        if (ids == null || ids.isEmpty() || usuarioId == null) return 0;
        int reclamadas = 0;
        for (VidrioSostenible v : vidrioSostenibleRepository.findAllById(ids)) {
            if (v.getUsuarioId() != null) continue;
            v.setUsuarioId(usuarioId);
            vidrioSostenibleRepository.save(v);
            reclamadas++;
        }
        return reclamadas;
    }
}