package com.smartglass.controller;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.CarritoService;
import com.smartglass.service.VidrioSostenibleService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Flujo MVC de cotizaciones de vidrio a medida (mis-cotizaciones.html,
 * vidrio-personalizado-detalle.html, vidrio-personalizado-form.html).
 * Distinto de VidrioPersonalizadoController (la API REST bajo
 * /api/vidrios) -- este es el equivalente en HTML/formularios.
 *
 * CAMBIOS (parches de backend):
 *  - aprobar: exige ROLE_ADMIN (PermisosAdvice.esAdmin); un usuario normal
 *    recibe flash de error y el estado NO cambia.
 *  - nuevo /agregar-al-carrito/{id}: el dueno de una obra APROBADA la anade
 *    a su carrito desde el detalle (parche 3).
 */
@Controller
@RequestMapping("/vidrio-personalizado")
public class VidrioPersonalizadoWebController {

    private final VidrioSostenibleService vidrioSostenibleService;
    private final CarritoService carritoService;

    public VidrioPersonalizadoWebController(VidrioSostenibleService vidrioSostenibleService,
                                            CarritoService carritoService) {
        this.vidrioSostenibleService = vidrioSostenibleService;
        this.carritoService = carritoService;
    }

    @GetMapping("/mis-cotizaciones")
    public String misCotizaciones(@ModelAttribute("usuario") Usuario usuario, Model model) {
        List<VidrioSostenible> cotizaciones = vidrioSostenibleService.obtenerPorUsuario(usuario.getId());
        long pendientes = cotizaciones.stream().filter(c -> "COTIZACION".equals(c.getEstado())).count();
        long aprobadas = cotizaciones.stream().filter(c -> "APROBADO".equals(c.getEstado())).count();
        double huellaTotal = cotizaciones.stream().mapToDouble(VidrioSostenible::getHuellaCarbono).sum();
        model.addAttribute("cotizaciones", cotizaciones);
        model.addAttribute("totalCots", cotizaciones.size());
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aprobadas", aprobadas);
        model.addAttribute("huellaTotal", Math.round(huellaTotal * 100.0) / 100.0);
        return "mis-cotizaciones";
    }

    @GetMapping("/cotizacion/{id}")
    public String verCotizacion(@PathVariable String id, Model model) {
        return vidrioSostenibleService.obtenerPorId(id)
                .map(vidrio -> {
                    model.addAttribute("vidrio", vidrio);
                    return "vidrio-personalizado-detalle";
                })
                .orElse("redirect:/vidrio-personalizado/mis-cotizaciones");
    }

    @GetMapping("/cotizar")
    public String mostrarFormulario() {
        return "vidrio-personalizado-form";
    }

    @PostMapping("/cotizar")
    public String cotizar(@ModelAttribute("usuario") Usuario usuario,
                          CotizacionVidrioRequest request) {
        request.setUsuarioId(usuario.getId());
        VidrioSostenible creado = vidrioSostenibleService.generarCotizacion(request);
        return "redirect:/vidrio-personalizado/cotizacion/" + creado.getId();
    }

    /** PARCHES 1: aprobar es accion de administrador. */
    @PostMapping("/aprobar/{id}")
    public String aprobar(@PathVariable String id, Authentication auth, RedirectAttributes ra) {
        if (!PermisosAdvice.esAdmin(auth)) {
            ra.addFlashAttribute("mensaje", "Solo un administrador puede aprobar cotizaciones.");
            ra.addFlashAttribute("tipoMensaje", "error");
            return "redirect:/vidrio-personalizado/mis-cotizaciones"; // sin tocar el estado
        }
        vidrioSostenibleService.actualizarEstado(id, "APROBADO");
        ra.addFlashAttribute("mensaje", "Cotización aprobada.");
        ra.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/vidrio-personalizado/mis-cotizaciones";
    }

    /** PARCHES 3: el dueno anade su obra APROBADA al carrito. */
    @PostMapping("/agregar-al-carrito/{id}")
    public String agregarAlCarrito(@PathVariable String id,
                                   @ModelAttribute("usuario") Usuario usuario,
                                   RedirectAttributes ra) {
        return vidrioSostenibleService.obtenerPorId(id)
                .filter(vidrio -> usuario.getId().equals(vidrio.getUsuarioId()))
                .filter(vidrio -> "APROBADO".equals(vidrio.getEstado()))
                .map(vidrio -> {
                    carritoService.agregarCotizacionAprobada(vidrio);
                    ra.addFlashAttribute("mensaje", "Obra añadida al carrito.");
                    ra.addFlashAttribute("tipoMensaje", "success");
                    return "redirect:/carrito";
                })
                .orElseGet(() -> {
                    ra.addFlashAttribute("mensaje", "La obra no existe o aún no está aprobada.");
                    ra.addFlashAttribute("tipoMensaje", "error");
                    return "redirect:/vidrio-personalizado/mis-cotizaciones";
                });
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable String id) {
        vidrioSostenibleService.eliminarPorId(id);
        return "redirect:/vidrio-personalizado/mis-cotizaciones";
    }
}