package com.smartglass.controller;

import com.smartglass.dto.CotizacionVidrioRequest;
import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Usuario;
import com.smartglass.service.VidrioSostenibleService;
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
 * El parametro @ModelAttribute("usuario") Usuario usuario en cada
 * metodo no es una consulta nueva: reutiliza el mismo atributo que
 * VistaGlobalAttributesAdvice ya coloco en el Model para esta misma
 * peticion, asi no repetimos la logica de resolver el usuario actual.
 */
@Controller
@RequestMapping("/vidrio-personalizado")
public class VidrioPersonalizadoWebController {

    private final VidrioSostenibleService vidrioSostenibleService;

    public VidrioPersonalizadoWebController(VidrioSostenibleService vidrioSostenibleService) {
        this.vidrioSostenibleService = vidrioSostenibleService;
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

    @PostMapping("/aprobar/{id}")
    public String aprobar(@PathVariable String id, RedirectAttributes ra) {
        vidrioSostenibleService.actualizarEstado(id, "APROBADO");
        ra.addFlashAttribute("mensaje", "Cotización aprobada.");
        return "redirect:/vidrio-personalizado/mis-cotizaciones";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable String id) {
        vidrioSostenibleService.eliminarPorId(id);
        return "redirect:/vidrio-personalizado/mis-cotizaciones";
    }
}