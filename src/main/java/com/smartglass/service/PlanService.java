package com.smartglass.service;

import com.smartglass.model.mongo.ConfiguracionPlataforma;
import com.smartglass.repository.mongo.ConfiguracionPlataformaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * T5 · Plan SaaS de la instalacion y sus funciones (feature flags).
 *
 * - Bean "plan": las plantillas preguntan con ${@plan.activa('CORTE_PERSONALIZADO')} (funciona en
 *   cualquier vista, tambien en las de error, sin depender de un ControllerAdvice).
 * - Sin documento guardado el plan es AVANZADO: la instalacion actual no pierde nada.
 * - Lectura en memoria (se recarga al cambiar o cada 60 s, por si otra instancia lo cambio).
 * - Cada funcion: lo que diga el plan, salvo un ajuste explicito del superadmin.
 */
@Service("plan")
public class PlanService {

    public static final String BASICO = "BASICO";
    public static final String AVANZADO = "AVANZADO";
    private static final long VIGENCIA_MS = 60_000;
    private static final int HISTORIAL_MAX = 50;

    private final ConfiguracionPlataformaRepository repositorio;
    private volatile ConfiguracionPlataforma cache;
    private volatile long leidoEn;

    public PlanService(ConfiguracionPlataformaRepository repositorio) {
        this.repositorio = repositorio;
    }

    private ConfiguracionPlataforma config() {
        long ahora = System.currentTimeMillis();
        ConfiguracionPlataforma c = cache;
        if (c != null && ahora - leidoEn < VIGENCIA_MS) return c;
        try {
            c = repositorio.findById(ConfiguracionPlataforma.ID).orElseGet(ConfiguracionPlataforma::new);
        } catch (RuntimeException e) {
            // sin base: se mantiene lo ultimo conocido o, si no hay, el plan completo
            c = cache != null ? cache : new ConfiguracionPlataforma();
        }
        cache = c;
        leidoEn = ahora;
        return c;
    }

    public String planActual() {
        String p = config().getPlan();
        return BASICO.equals(p) ? BASICO : AVANZADO;
    }

    /** null = sigue al plan; true/false = ajuste explicito del superadmin. */
    public Boolean ajuste(Funcion f) {
        return config().getAjustes().get(f.name());
    }

    public boolean activa(Funcion f) {
        Boolean a = ajuste(f);
        return a != null ? a : f.incluidaEn(planActual());
    }

    /** Para las plantillas: nombre de la funcion; una desconocida cuenta como activa (no rompe vistas). */
    public boolean activa(String nombre) {
        try {
            return activa(Funcion.valueOf(nombre.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return true;
        }
    }

    public List<ConfiguracionPlataforma.Cambio> historial() {
        List<ConfiguracionPlataforma.Cambio> h = new ArrayList<>(config().getHistorial());
        java.util.Collections.reverse(h);   // lo mas reciente primero
        return h;
    }

    public synchronized void cambiarPlan(String plan, String usuario) {
        String nuevo = BASICO.equalsIgnoreCase(plan) ? BASICO : AVANZADO;
        ConfiguracionPlataforma c = leerFresco();
        if (nuevo.equals(c.getPlan()) && repositorio.existsById(ConfiguracionPlataforma.ID)) return;
        c.setPlan(nuevo);
        registrar(c, usuario, "plan=" + nuevo);
    }

    /** modo: "PLAN" (quita el ajuste), "ON" o "OFF". */
    public synchronized void ajustarFuncion(Funcion f, String modo, String usuario) {
        ConfiguracionPlataforma c = leerFresco();
        String m = modo == null ? "PLAN" : modo.trim().toUpperCase(Locale.ROOT);
        switch (m) {
            case "ON" -> c.getAjustes().put(f.name(), true);
            case "OFF" -> c.getAjustes().put(f.name(), false);
            default -> c.getAjustes().remove(f.name());
        }
        registrar(c, usuario, f.name() + "=" + (m.equals("ON") || m.equals("OFF") ? m : "PLAN"));
    }

    private ConfiguracionPlataforma leerFresco() {
        return repositorio.findById(ConfiguracionPlataforma.ID).orElseGet(ConfiguracionPlataforma::new);
    }

    private void registrar(ConfiguracionPlataforma c, String usuario, String descripcion) {
        c.getHistorial().add(new ConfiguracionPlataforma.Cambio(LocalDateTime.now(), usuario, descripcion));
        while (c.getHistorial().size() > HISTORIAL_MAX) c.getHistorial().remove(0);
        cache = repositorio.save(c);
        leidoEn = System.currentTimeMillis();
    }
}
