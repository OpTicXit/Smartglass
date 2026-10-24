package com.smartglass.controller;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.repository.mysql.UsuarioRepository;
import com.smartglass.service.VidrioSostenibleService;
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

    public AdminCotizacionesController(VidrioSostenibleService vidrioSostenibleService,
                                       UsuarioRepository usuarioRepository) {
        this.vidrioSostenibleService = vidrioSostenibleService;
        this.usuarioRepository = usuarioRepository;
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

        model.addAttribute("filas", filas);
        model.addAttribute("conteos", conteos);
        model.addAttribute("filtro", filtro);
        return "admin-cotizaciones";
    }

    // --------------------------------------------------------------- acciones
    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable String id,
                          @RequestParam(required = false) String estado,
                          Authentication auth, RedirectAttributes ra) {
        return cambiar(id, VidrioSostenibleService.APROBADO, estado, auth, ra);
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable String id,
                           @RequestParam(required = false) String estado,
                           Authentication auth, RedirectAttributes ra) {
        return cambiar(id, VidrioSostenibleService.RECHAZADA, estado, auth, ra);
    }

    private String cambiar(String id, String nuevoEstado, String filtro,
                           Authentication auth, RedirectAttributes ra) {
        if (!PermisosAdvice.esAdmin(auth)) {
            ra.addFlashAttribute("mensaje", "Solo un administrador puede gestionar cotizaciones.");
            ra.addFlashAttribute("tipoMensaje", "error");
            return "redirect:/vidrio-personalizado/mis-cotizaciones";
        }
        boolean aprobar = VidrioSostenibleService.APROBADO.equals(nuevoEstado);
        switch (vidrioSostenibleService.cambiarEstado(id, nuevoEstado)) {
            case OK -> {
                ra.addFlashAttribute("mensaje", aprobar ? "Cotización aprobada." : "Cotización rechazada.");
                ra.addFlashAttribute("tipoMensaje", "success");
            }
            case NO_ENCONTRADA -> {
                ra.addFlashAttribute("mensaje", "La cotización ya no existe.");
                ra.addFlashAttribute("tipoMensaje", "error");
            }
            case TRANSICION_INVALIDA -> {
                ra.addFlashAttribute("mensaje", "Esa cotización ya no se puede "
                        + (aprobar ? "aprobar" : "rechazar") + " desde su estado actual.");
                ra.addFlashAttribute("tipoMensaje", "error");
            }
        }
        String f = normalizarFiltro(filtro);
        return "redirect:/admin/cotizaciones" + ("TODAS".equals(f) ? "" : "?estado=" + f);
    }

    // ------------------------------------------------------------------ vista
    /** Fila lista para la plantilla (getters clasicos, SpEL los lee sin friccion). */
    public static class FilaCotizacion {
        private final String id, fecha, cliente, tipo, forma, medidas, precio, estado, nivel;
        private final double ancho, alto, espesor, co2, intensidad;
        private final boolean reciclado;

        FilaCotizacion(String id, String fecha, String cliente, String tipo, String forma,
                       double ancho, double alto, double espesor, String medidas,
                       String precio, String estado, String nivel,
                       double co2, double intensidad, boolean reciclado) {
            this.id = id; this.fecha = fecha; this.cliente = cliente; this.tipo = tipo;
            this.forma = forma; this.ancho = ancho; this.alto = alto; this.espesor = espesor;
            this.medidas = medidas; this.precio = precio;
            this.estado = estado; this.nivel = nivel; this.co2 = co2; this.intensidad = intensidad; this.reciclado = reciclado;
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
    }

    private FilaCotizacion aFila(VidrioSostenible v, Map<Long, String> clientes) {
        String cliente = clientes.getOrDefault(v.getUsuarioId(), "Usuario #" + v.getUsuarioId());
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
                v.isUsarVidrioReciclado());
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

    /** Medidas sin ceros sobrantes: 400 en vez de 400.0 */
    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}