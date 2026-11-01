package com.smartglass.controller;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.VidrioSostenibleService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Panel del administrador para aprobar o rechazar las cotizaciones de
 * TODOS los clientes.
 *   GET  /admin/cotizaciones[?estado=COTIZACION|APROBADO|RECHAZADA]
 *   POST /admin/cotizaciones/{id}/aprobar
 *   POST /admin/cotizaciones/{id}/rechazar
 * Sin edicion, sin carrito y sin pagos: solo cambia el estado.
 * "/admin/**" exige ROLE_ADMIN en SecurityConfig; los POST repiten la
 * comprobacion con PermisosAdvice.esAdmin(auth) (defensa en profundidad).
 * Maquina de estados: la valida VidrioSostenibleService.cambiarEstado.
 * CSRF desactivado en el proyecto: los forms no llevan token.
 *
 * T2: los avisos salen de messages*.properties (idioma activo) y, con
 * volver=detalle, aprobar o rechazar regresa al detalle de la obra.
 *
 * CORREGIDO: FilaCotizacion ahora expone ancho / alto / espesor. La
 * plantilla admin-cotizaciones.html los lee (${f.ancho}, th:attr
 * data-ancho...) y sin esos getters fallaba con 500 (SpEL).
 */
@Controller
@RequestMapping("/admin/cotizaciones")
public class AdminCotizacionesController {

    private static final List<String> ESTADOS = List.of("COTIZACION", "APROBADO", "RECHAZADA");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VidrioSostenibleService vidrioSostenibleService;
    private final UsuarioRepository usuarioRepository;
    private final MessageSource messageSource;

    public AdminCotizacionesController(VidrioSostenibleService vidrioSostenibleService,
                                       UsuarioRepository usuarioRepository,
                                       MessageSource messageSource) {
        this.vidrioSostenibleService = vidrioSostenibleService;
        this.usuarioRepository = usuarioRepository;
        this.messageSource = messageSource;
    }

    /** Texto en el idioma activo (D2: los numeros llegan ya como texto). */
    private String texto(String clave, Object... args) {
        return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
    }

    // ------------------------------------------------------------------ lista
    @GetMapping
    public String listar(@RequestParam(required = false) String estado, Model model) {
        String filtro = normalizarFiltro(estado);
        List<VidrioSostenible> todas = vidrioSostenibleService.obtenerTodas();

        // usuarioId -> username (la cotizacion solo guarda el id del cliente)
        Map<Long, String> clientes = usuarioRepository.findAll().stream()
                .filter(u -> u.getId() != null && u.getUsername() != null)
                .collect(Collectors.toMap(Usuario::getId, Usuario::getUsername, (a, b) -> a));

        Map<String, Long> conteos = new LinkedHashMap<>();
        conteos.put("TODAS", (long) todas.size());
        for (String e : ESTADOS) {
            conteos.put(e, todas.stream().filter(v -> e.equals(estadoDe(v))).count());
        }

        List<FilaCotizacion> filas = todas.stream()
                .filter(v -> "TODAS".equals(filtro) || filtro.equals(estadoDe(v)))
                .map(v -> aFila(v, clientes))
                .toList();

        // T3: compromisos de los clientes (ventaja operativa: retazos, rutas, empaques y calcin recuperable)
        Map<String, Object> compromisos = new LinkedHashMap<>();
        compromisos.put("retoma", todas.stream().filter(VidrioSostenible::isRetomaVidrio).count());
        compromisos.put("kgRetoma", fmt1(todas.stream().filter(VidrioSostenible::isRetomaVidrio).mapToDouble(VidrioSostenible::getPesoEstimado).sum()));
        compromisos.put("empaque", todas.stream().filter(VidrioSostenible::isEmpaqueReutilizable).count());
        compromisos.put("consolidada", todas.stream().filter(VidrioSostenible::isEntregaConsolidada).count());
        compromisos.put("retazos", todas.stream().filter(VidrioSostenible::isUsarRetazos).count());
        compromisos.put("oro", todas.stream().filter(v -> "ORO".equals(v.getCompromisoNivel())).count());
        compromisos.put("plata", todas.stream().filter(v -> "PLATA".equals(v.getCompromisoNivel())).count());
        compromisos.put("bronce", todas.stream().filter(v -> "BRONCE".equals(v.getCompromisoNivel())).count());
        model.addAttribute("compromisos", compromisos);
        model.addAttribute("filas", filas);
        model.addAttribute("conteos", conteos);
        model.addAttribute("filtro", filtro);
        return "admin-cotizaciones";
    }

    // --------------------------------------------------------------- acciones
    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable String id,
                          @RequestParam(required = false) String estado,
                          @RequestParam(required = false) String volver,
                          Authentication auth, RedirectAttributes ra) {
        return cambiar(id, VidrioSostenibleService.APROBADO, estado, volver, auth, ra);
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable String id,
                           @RequestParam(required = false) String estado,
                           @RequestParam(required = false) String volver,
                           Authentication auth, RedirectAttributes ra) {
        return cambiar(id, VidrioSostenibleService.RECHAZADA, estado, volver, auth, ra);
    }

    private String cambiar(String id, String nuevoEstado, String filtro, String volver,
                           Authentication auth, RedirectAttributes ra) {
        if (!PermisosAdvice.esAdmin(auth)) {
            ra.addFlashAttribute("mensaje", texto("cotflash.solo_admin_gestionar"));
            ra.addFlashAttribute("tipoMensaje", "error");
            return "redirect:/vidrio-personalizado/mis-cotizaciones";
        }
        boolean aprobar = VidrioSostenibleService.APROBADO.equals(nuevoEstado);
        VidrioSostenibleService.ResultadoCambio resultado = vidrioSostenibleService.cambiarEstado(id, nuevoEstado);
        switch (resultado) {
            case OK -> {
                ra.addFlashAttribute("mensaje", texto(aprobar ? "cotflash.aprobada" : "cotflash.rechazada"));
                ra.addFlashAttribute("tipoMensaje", "success");
            }
            case NO_ENCONTRADA -> {
                ra.addFlashAttribute("mensaje", texto("cotflash.no_existe"));
                ra.addFlashAttribute("tipoMensaje", "error");
            }
            case TRANSICION_INVALIDA -> {
                ra.addFlashAttribute("mensaje", texto(aprobar ? "cotflash.no_aprobar" : "cotflash.no_rechazar"));
                ra.addFlashAttribute("tipoMensaje", "error");
            }
        }
        // T2: desde el detalle de la obra se vuelve a ese mismo detalle (si la obra sigue existiendo)
        if ("detalle".equals(volver) && resultado != VidrioSostenibleService.ResultadoCambio.NO_ENCONTRADA) {
            return "redirect:/vidrio-personalizado/cotizacion/" + id;
        }
        String f = normalizarFiltro(filtro);
        return "redirect:/admin/cotizaciones" + ("TODAS".equals(f) ? "" : "?estado=" + f);
    }

    // ------------------------------------------------------------------ vista
    /** Fila lista para la plantilla (getters clasicos, SpEL los lee sin friccion). */
    public static class FilaCotizacion {
        private final String id, fecha, cliente, tipo, forma, medidas, precio, estado, nivel, puntos, sello;
        private final double ancho, alto, espesor, co2, intensidad;
        private final boolean reciclado;

        FilaCotizacion(String id, String fecha, String cliente, String tipo, String forma,
                       double ancho, double alto, double espesor, String medidas,
                       String precio, String estado, String nivel,
                       double co2, double intensidad, boolean reciclado, String puntos, String sello) {
            this.id = id; this.fecha = fecha; this.cliente = cliente; this.tipo = tipo;
            this.forma = forma; this.ancho = ancho; this.alto = alto; this.espesor = espesor;
            this.medidas = medidas; this.precio = precio;
            this.estado = estado; this.nivel = nivel; this.co2 = co2; this.intensidad = intensidad; this.reciclado = reciclado;
            this.puntos = puntos; this.sello = sello;
        }
        public String getId() { return id; }
        public String getFecha() { return fecha; }
        public String getCliente() { return cliente; }
        public String getTipo() { return tipo; }
        public String getForma() { return forma; }
        public double getAncho() { return ancho; }
        public double getAlto() { return alto; }
        public double getEspesor() { return espesor; }
        public String getMedidas() { return medidas; }
        public String getPrecio() { return precio; }
        public String getEstado() { return estado; }
        public String getNivel() { return nivel; }
        /** Huella absoluta de la PIEZA (kg CO2e): dato neutro, sin juicio de color. */
        public double getCo2() { return co2; }
        /** Intensidad del PROCESO (kg CO2e/m2): es lo que determina el nivel. */
        public double getIntensidad() { return intensidad; }
        public boolean isReciclado() { return reciclado; }
        /** F45: vertices del corte personalizado ("x,y;x,y;..."); vacio si es una forma fija. */
        public String getPuntos() { return puntos; }
        /** T3: sello "Compromiso verde" (ORO, PLATA, BRONCE o vacio). */
        public String getSello() { return sello; }
    }

    private FilaCotizacion aFila(VidrioSostenible v, Map<Long, String> clientes) {
        // T1: una obra cotizada sin cuenta no tiene usuarioId hasta que su autor inicia sesion
        String cliente = v.getUsuarioId() == null
                ? texto("admin.cotizaciones.visitante")
                : clientes.getOrDefault(v.getUsuarioId(), texto("admin.cotizaciones.usuario_n", String.valueOf(v.getUsuarioId())));
        // F24: fuente unica de intensidad y nivel = la entidad (umbrales 12/20), la misma
        // que usa el detalle. El nivel sale SIEMPRE de la intensidad, nunca del absoluto.
        double intensidad = v.getIntensidadCarbono();
        String nivel = v.getNivelImpacto();
        return new FilaCotizacion(
                v.getId(),
                v.getFechaCreacion() != null ? v.getFechaCreacion().format(FECHA) : "—",
                cliente,
                v.getTipoVidrio(),
                v.getForma(),
                v.getAncho(),
                v.getAlto(),
                v.getEspesor(),
                fmt(v.getAncho()) + " × " + fmt(v.getAlto()) + " cm · " + fmt(v.getEspesor()) + " mm",
                v.getPrecioEstimadoFormateado(),
                estadoDe(v),
                nivel,
                v.getHuellaCarbono(),
                intensidad,
                v.isUsarVidrioReciclado(),
                v.getPuntosTexto(),
                v.getCompromisoNivel());
    }

    // ------------------------------------------------------------- utilidades
    private static String normalizarFiltro(String estado) {
        if (estado == null) return "TODAS";
        String e = estado.trim().toUpperCase();
        return ESTADOS.contains(e) ? e : "TODAS";
    }

    private static String estadoDe(VidrioSostenible v) {
        return v.getEstado() == null ? "" : v.getEstado().toUpperCase();
    }

    /** Un decimal con coma decimal y punto de miles (D2: formato es-CO en todos los idiomas). */
    private static String fmt1(double d) {
        java.text.DecimalFormatSymbols s = new java.text.DecimalFormatSymbols(Locale.forLanguageTag("es-CO"));
        s.setDecimalSeparator(','); s.setGroupingSeparator('.');
        return new java.text.DecimalFormat("#,##0.0", s).format(d);
    }

    /** Medidas sin ceros sobrantes: 400 en vez de 400.0 */
    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}