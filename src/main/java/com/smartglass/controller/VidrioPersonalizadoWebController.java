package com.smartglass.controller;

import com.smartglass.config.SesionAnonima;
import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.CarritoService;
import com.smartglass.service.VidrioSostenibleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Flujo MVC de cotizaciones de vidrio a medida (mis-cotizaciones.html,
 * vidrio-personalizado-detalle.html, vidrio-personalizado-form.html).
 * Distinto de VidrioPersonalizadoController (API REST bajo /api/vidrios).
 *
 * CAMBIOS (auditoria de fugas):
 * - F9: NINGUN metodo declara @ModelAttribute("usuario") Usuario como
 *   parametro. Con {id} (ObjectId de Mongo) Spring intentaba bindear la
 *   variable de ruta a Usuario.id (Long) -> 400. Ademas, bindear request
 *   params sobre la entidad Usuario es un riesgo de mass-assignment. El
 *   usuario se lee del Model (lo publica VistaGlobalAttributesAdvice).
 * - F8: verCotizacion solo para el dueno o un admin.
 * - F10: aprobar solo admin y solo desde COTIZACION; re-aprobar una
 *   RECHAZADA es exclusivo del panel /admin/cotizaciones.
 * - F7: eliminar: dueno solo COTIZACION/RECHAZADA; admin cualquiera.
 * - agregar-al-carrito: dueno + estado APROBADO.
 * - T1 (D4): cotizar (GET y POST) y ver la obra recien cotizada son anonimos.
 *   La obra anonima se guarda con usuarioId null (el del formulario se ignora)
 *   y solo la ve la sesion que la creo (SesionAnonima); al iniciar sesion pasa a
 *   ser del usuario. Mis cotizaciones, aprobar, eliminar y agregar-al-carrito
 *   siguen exigiendo cuenta.
 * - T2: avisos en el idioma activo (messages*.properties) y, al aprobar desde el
 *   detalle (volver=detalle), se regresa a ese detalle en lugar de a la lista.
 */
@Controller
@RequestMapping("/vidrio-personalizado")
public class VidrioPersonalizadoWebController {

    private static final String REDIRECT_MIS_COTIZACIONES = "redirect:/vidrio-personalizado/mis-cotizaciones";

    private final VidrioSostenibleService vidrioSostenibleService;
    private final CarritoService carritoService;
    private final MessageSource messageSource;

    public VidrioPersonalizadoWebController(VidrioSostenibleService vidrioSostenibleService,
                                            CarritoService carritoService,
                                            MessageSource messageSource) {
        this.vidrioSostenibleService = vidrioSostenibleService;
        this.carritoService = carritoService;
        this.messageSource = messageSource;
    }

    /** Texto en el idioma activo. */
    private String texto(String clave) {
        return messageSource.getMessage(clave, null, LocaleContextHolder.getLocale());
    }

    /** Usuario logueado, tal como lo publica el advice global en el Model (null si no esta). */
    private Usuario usuarioActual(Model model) {
        Object u = model.getAttribute("usuario");
        return (u instanceof Usuario) ? (Usuario) u : null;
    }

    /** Visitante sin cuenta (T1). Con sesion pero sin Usuario en BD NO cuenta como anonimo. */
    private static boolean esVisitante(Model model) {
        return !Boolean.TRUE.equals(model.getAttribute("autenticado"));
    }

    private static boolean esDueno(VidrioSostenible v, Usuario usuario) {
        return v.getUsuarioId() != null && usuario != null && v.getUsuarioId().equals(usuario.getId());
    }

    @GetMapping("/mis-cotizaciones")
    public String misCotizaciones(Model model) {
        Usuario usuario = usuarioActual(model);
        if (usuario == null) return "redirect:/login";
        List<VidrioSostenible> cotizaciones = vidrioSostenibleService.obtenerPorUsuario(usuario.getId());
        long pendientes = cotizaciones.stream().filter(c -> VidrioSostenibleService.COTIZACION.equals(c.getEstado())).count();
        long aprobadas = cotizaciones.stream().filter(c -> VidrioSostenibleService.APROBADO.equals(c.getEstado())).count();
        long rechazadas = cotizaciones.stream().filter(c -> VidrioSostenibleService.RECHAZADA.equals(c.getEstado())).count();
        double huellaTotal = cotizaciones.stream().mapToDouble(VidrioSostenible::getHuellaCarbono).sum();
        model.addAttribute("cotizaciones", cotizaciones);
        model.addAttribute("totalCots", cotizaciones.size());
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aprobadas", aprobadas);
        model.addAttribute("rechazadas", rechazadas);
        model.addAttribute("huellaTotal", Math.round(huellaTotal * 100.0) / 100.0);
        return "mis-cotizaciones";
    }

    /** F8: solo el dueno o un admin abren el detalle. T1: o la sesion anonima que la cotizo. */
    @GetMapping("/cotizacion/{id}")
    public String verCotizacion(@PathVariable String id, Model model, Authentication auth, HttpSession sesion) {
        Usuario usuario = usuarioActual(model);
        if (usuario == null) {
            if (!esVisitante(model) || !SesionAnonima.esCotizacionDeLaSesion(sesion, id)) return "redirect:/login";
            return vidrioSostenibleService.obtenerPorId(id)
                    .filter(v -> v.getUsuarioId() == null)
                    .map(vidrio -> {
                        model.addAttribute("vidrio", vidrio);
                        model.addAttribute("esDueno", false);
                        model.addAttribute("obraAnonima", true);
                        return "vidrio-personalizado-detalle";
                    })
                    .orElse("redirect:/login");
        }
        boolean admin = PermisosAdvice.esAdmin(auth);
        return vidrioSostenibleService.obtenerPorId(id)
                .filter(v -> admin || esDueno(v, usuario))
                .map(vidrio -> {
                    model.addAttribute("vidrio", vidrio);
                    model.addAttribute("esDueno", esDueno(vidrio, usuario));
                    return "vidrio-personalizado-detalle";
                })
                .orElse(REDIRECT_MIS_COTIZACIONES);
    }

    @GetMapping("/cotizar")
    public String mostrarFormulario() {
        return "vidrio-personalizado-form";
    }

    @PostMapping("/cotizar")
    public String cotizar(CotizacionVidrioRequest request, Model model, HttpSession sesion) {
        Usuario usuario = usuarioActual(model);
        if (usuario == null && !esVisitante(model)) return "redirect:/login";
        // el dueno lo define la sesion, no el formulario: anonimo -> null (T1)
        request.setUsuarioId(usuario != null ? usuario.getId() : null);
        VidrioSostenible creado = vidrioSostenibleService.generarCotizacion(request);
        if (usuario == null) SesionAnonima.registrarCotizacion(sesion, creado.getId());
        return "redirect:/vidrio-personalizado/cotizacion/" + creado.getId();
    }

    /** F10: solo admin y solo COTIZACION -> APROBADO; la re-aprobacion es del panel. */
    @PostMapping("/aprobar/{id}")
    public String aprobar(@PathVariable String id, @RequestParam(required = false) String volver,
                          Authentication auth, RedirectAttributes ra) {
        if (!PermisosAdvice.esAdmin(auth)) {
            ra.addFlashAttribute("mensaje", texto("cotflash.solo_admin_aprobar"));
            ra.addFlashAttribute("tipoMensaje", "error");
            return REDIRECT_MIS_COTIZACIONES;
        }
        VidrioSostenible v = vidrioSostenibleService.obtenerPorId(id).orElse(null);
        if (v == null) {
            ra.addFlashAttribute("mensaje", texto("cotflash.no_existe"));
            ra.addFlashAttribute("tipoMensaje", "error");
        } else if (!VidrioSostenibleService.COTIZACION.equals(v.getEstado())) {
            ra.addFlashAttribute("mensaje", texto("cotflash.ya_gestionada"));
            ra.addFlashAttribute("tipoMensaje", "error");
        } else {
            vidrioSostenibleService.cambiarEstado(id, VidrioSostenibleService.APROBADO);
            ra.addFlashAttribute("mensaje", texto("cotflash.aprobada"));
            ra.addFlashAttribute("tipoMensaje", "success");
        }
        if (v != null && "detalle".equals(volver)) return "redirect:/vidrio-personalizado/cotizacion/" + id;
        return REDIRECT_MIS_COTIZACIONES;
    }

    /** F7: dueno solo COTIZACION o RECHAZADA. Admin: cualquiera. */
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable String id, Model model,
                           Authentication auth, RedirectAttributes ra) {
        Usuario usuario = usuarioActual(model);
        if (usuario == null) return "redirect:/login";
        boolean admin = PermisosAdvice.esAdmin(auth);
        return vidrioSostenibleService.obtenerPorId(id)
                .filter(v -> admin || esDueno(v, usuario))
                .filter(v -> admin || !VidrioSostenibleService.APROBADO.equals(v.getEstado()))
                .map(v -> {
                    vidrioSostenibleService.eliminarPorId(v.getId());
                    ra.addFlashAttribute("mensaje", texto("cotflash.eliminada"));
                    ra.addFlashAttribute("tipoMensaje", "success");
                    return REDIRECT_MIS_COTIZACIONES;
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("mensaje", texto("cotflash.no_eliminar"));
                    ra.addFlashAttribute("tipoMensaje", "error");
                    return REDIRECT_MIS_COTIZACIONES;
                });
    }

    /** El dueno anade su obra APROBADA al carrito (una RECHAZADA o ajena no entra). */
    @PostMapping("/agregar-al-carrito/{id}")
    public String agregarAlCarrito(@PathVariable String id, Model model, RedirectAttributes ra) {
        Usuario usuario = usuarioActual(model);
        if (usuario == null) return "redirect:/login";
        return vidrioSostenibleService.obtenerPorId(id)
                .filter(vidrio -> esDueno(vidrio, usuario))
                .filter(vidrio -> VidrioSostenibleService.APROBADO.equals(vidrio.getEstado()))
                .map(vidrio -> {
                    carritoService.agregarCotizacionAprobada(vidrio);
                    ra.addFlashAttribute("mensaje", texto("cotflash.al_carrito"));
                    ra.addFlashAttribute("tipoMensaje", "success");
                    return "redirect:/carrito";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("mensaje", texto("cotflash.no_carrito"));
                    ra.addFlashAttribute("tipoMensaje", "error");
                    return REDIRECT_MIS_COTIZACIONES;
                });
    }
}