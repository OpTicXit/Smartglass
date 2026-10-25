package com.smartglass.controller;

import com.smartglass.service.Funcion;
import com.smartglass.service.PlanService;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * T5 · Consola SaaS del dueno de la plataforma (aparte del panel /admin de la vidrieria).
 *   GET  /superadmin                 plan actual, funciones, historial y estudio de mercado
 *   POST /superadmin/plan            plan=BASICO|AVANZADO
 *   POST /superadmin/funcion/{clave} modo=PLAN|ON|OFF
 *
 * Quien es superadmin NO se guarda en la base: lo define la variable SUPERADMIN_USUARIOS
 * (nombres de usuario separados por comas). Asi el admin de la vidrieria, que puede cambiar
 * roles desde /admin/usuarios, no puede darse este acceso. Sin la variable, nadie entra.
 * Para cualquier otro usuario la consola responde 404 (no se anuncia que existe).
 */
@Controller
@RequestMapping("/superadmin")
public class SuperadminController {

    private final PlanService plan;
    private final MessageSource messageSource;
    private final Set<String> superadmins;

    public SuperadminController(PlanService plan, MessageSource messageSource,
                                @Value("${smartglass.superadmin.usuarios:}") String usuarios) {
        this.plan = plan;
        this.messageSource = messageSource;
        this.superadmins = Arrays.stream(usuarios.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    private String exigirSuperadmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !superadmins.contains(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return auth.getName();
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
        /** PLAN (sigue al plan), ON u OFF (ajuste del superadmin). */
        public String getModo() { return modo; }
    }

    @GetMapping
    public String consola(Authentication auth, Model model) {
        model.addAttribute("superadmin", exigirSuperadmin(auth));
        List<FilaFuncion> filas = Arrays.stream(Funcion.values())
                .map(f -> new FilaFuncion(f, plan.activa(f), plan.ajuste(f)))
                .toList();
        model.addAttribute("planActual", plan.planActual());
        model.addAttribute("funciones", filas);
        model.addAttribute("historial", plan.historial());
        model.addAttribute("activas", filas.stream().filter(FilaFuncion::isActiva).count());
        return "superadmin";
    }

    @PostMapping("/plan")
    public String cambiarPlan(@RequestParam String plan, Authentication auth, RedirectAttributes ra) {
        String usuario = exigirSuperadmin(auth);
        this.plan.cambiarPlan(plan, usuario);
        ra.addFlashAttribute("mensaje", texto("superadmin.plan_guardado",
                texto("superadmin.plan." + this.plan.planActual())));
        return "redirect:/superadmin";
    }

    @PostMapping("/funcion/{clave}")
    public String ajustarFuncion(@PathVariable String clave, @RequestParam String modo,
                                 Authentication auth, RedirectAttributes ra) {
        String usuario = exigirSuperadmin(auth);
        Funcion f;
        try {
            f = Funcion.valueOf(clave);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        plan.ajustarFuncion(f, modo, usuario);
        ra.addFlashAttribute("mensaje", texto("superadmin.funcion_guardada", texto("funcion." + f.name() + ".titulo")));
        return "redirect:/superadmin#funciones";
    }
}
