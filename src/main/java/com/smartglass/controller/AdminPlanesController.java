package com.smartglass.controller;

import com.smartglass.service.Funcion;
import com.smartglass.service.PlanService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * U3 · Planes SaaS dentro del panel de administracion (antes T5: consola /superadmin aparte).
 *   GET  /admin/planes                 plan actual, vista previa, funciones, historial y estudio de mercado
 *   POST /admin/planes/plan            plan=BASICO|AVANZADO
 *   POST /admin/planes/funcion/{clave} modo=PLAN|ON|OFF
 *
 * Decision del equipo (U3): cualquier admin edita el plan; ya no hay lista SUPERADMIN_USUARIOS.
 * SecurityConfig protege /admin/** con ROLE_ADMIN. /superadmin queda como redireccion.
 * La comparacion publica (sin precios) es /planes (HomeController).
 */
@Controller
public class AdminPlanesController {

    private final PlanService plan;
    private final MessageSource messageSource;

    public AdminPlanesController(PlanService plan, MessageSource messageSource) {
        this.plan = plan;
        this.messageSource = messageSource;
    }

    private String texto(String clave, Object... args) {
        return messageSource.getMessage(clave, args, LocaleContextHolder.getLocale());
    }

    /** Fila de la tabla de funciones (getters clasicos para SpEL). */
    public static class FilaFuncion {
        private final Funcion funcion;
        private final boolean activa, enBasico, enAvanzado;
        private final String modo;

        FilaFuncion(Funcion funcion, boolean activa, Boolean ajuste) {
            this.funcion = funcion;
            this.activa = activa;
            this.enBasico = funcion.incluidaEn(PlanService.BASICO);
            this.enAvanzado = funcion.incluidaEn(PlanService.AVANZADO);
            this.modo = ajuste == null ? "PLAN" : (ajuste ? "ON" : "OFF");
        }
        public String getClave() { return funcion.name(); }
        public String getIcono() { return funcion.getIcono(); }
        public boolean isActiva() { return activa; }
        public boolean isEnBasico() { return enBasico; }
        public boolean isEnAvanzado() { return enAvanzado; }
        /** PLAN (sigue al plan), ON u OFF (ajuste manual del admin). */
        public String getModo() { return modo; }
    }

    /** Filas de la tabla (tambien las usa la pagina publica para la lista por plan). */
    static List<FilaFuncion> filas(PlanService plan) {
        return Arrays.stream(Funcion.values())
                .map(f -> new FilaFuncion(f, plan.activa(f), plan.ajuste(f)))
                .toList();
    }

    @GetMapping({"/superadmin", "/superadmin/**"})
    public String consolaAntigua() {
        return "redirect:/admin/planes";
    }

    @GetMapping("/admin/planes")
    public String planes(Model model) {
        List<FilaFuncion> filas = filas(plan);
        model.addAttribute("planActual", plan.planActual());
        model.addAttribute("funciones", filas);
        model.addAttribute("historial", plan.historial());
        model.addAttribute("activas", filas.stream().filter(FilaFuncion::isActiva).count());
        return "admin/planes";
    }

    @PostMapping("/admin/planes/plan")
    public String cambiarPlan(@RequestParam String plan, Authentication auth, RedirectAttributes ra) {
        this.plan.cambiarPlan(plan, auth.getName());
        ra.addFlashAttribute("mensaje", texto("superadmin.plan_guardado",
                texto("superadmin.plan." + this.plan.planActual())));
        return "redirect:/admin/planes";
    }

    @PostMapping("/admin/planes/funcion/{clave}")
    public String ajustarFuncion(@PathVariable String clave, @RequestParam String modo,
                                 Authentication auth, RedirectAttributes ra) {
        Funcion f;
        try {
            f = Funcion.valueOf(clave);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        plan.ajustarFuncion(f, modo, auth.getName());
        ra.addFlashAttribute("mensaje", texto("superadmin.funcion_guardada", texto("funcion." + f.name() + ".titulo")));
        return "redirect:/admin/planes#funciones";
    }
}
