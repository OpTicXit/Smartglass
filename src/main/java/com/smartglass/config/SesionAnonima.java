package com.smartglass.config;

import com.smartglass.service.CarritoService;
import com.smartglass.service.VidrioSostenibleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * T1 (D4) · login solo al pagar. Lo que un visitante hace sin cuenta vive en SU
 * sesion HTTP (nunca en otra, nunca por un id que llegue del formulario):
 * - el carrito anonimo (CarritoService.SESION_CARRITO_ANONIMO);
 * - las obras que cotizo (COTIZACIONES): ids guardados con usuarioId null.
 *
 * Al autenticarse (formLogin o Google, via CustomSuccessHandler) transferir()
 * suma el carrito al del usuario y le asigna esas obras. Spring Security cambia
 * el id de sesion al autenticar (proteccion de fijacion) pero conserva sus
 * atributos, asi que la transferencia ocurre en la misma sesion del visitante.
 */
@Component
public class SesionAnonima {

    /** Atributo de sesion: ArrayList<String> con los ids de obras cotizadas sin cuenta. */
    public static final String COTIZACIONES = "cotizacionesAnonimas";
    /** Tope por sesion: evita que la sesion crezca sin limite. */
    private static final int MAX_COTIZACIONES = 20;

    private static final Logger log = LoggerFactory.getLogger(SesionAnonima.class);

    private final CarritoService carritoService;
    private final VidrioSostenibleService vidrioSostenibleService;
    private final UsuarioSesion usuarioSesion;

    public SesionAnonima(CarritoService carritoService,
                         VidrioSostenibleService vidrioSostenibleService,
                         UsuarioSesion usuarioSesion) {
        this.carritoService = carritoService;
        this.vidrioSostenibleService = vidrioSostenibleService;
        this.usuarioSesion = usuarioSesion;
    }

    @SuppressWarnings("unchecked")
    private static List<String> cotizaciones(HttpSession sesion) {
        Object valor = sesion == null ? null : sesion.getAttribute(COTIZACIONES);
        return valor instanceof List<?> l ? (List<String>) l : null;
    }

    /** Recuerda en la sesion una obra cotizada sin cuenta. */
    public static void registrarCotizacion(HttpSession sesion, String id) {
        if (sesion == null || id == null) return;
        List<String> ids = cotizaciones(sesion);
        ids = ids == null ? new ArrayList<>() : new ArrayList<>(ids);
        ids.remove(id);
        ids.add(id);
        while (ids.size() > MAX_COTIZACIONES) ids.remove(0);
        sesion.setAttribute(COTIZACIONES, ids);
    }

    /** true si ESTA sesion cotizo la obra como anonima. */
    public static boolean esCotizacionDeLaSesion(HttpSession sesion, String id) {
        List<String> ids = cotizaciones(sesion);
        return ids != null && id != null && ids.contains(id);
    }

    /**
     * Pasa el carrito y las obras anonimas de la sesion al usuario recien autenticado.
     * Nunca rompe el login: si algo falla se registra y el usuario entra igual.
     */
    public void transferir(HttpServletRequest request, Authentication auth) {
        HttpSession sesion = request.getSession(false);
        if (sesion == null || !quedaAlgo(sesion)) return;
        try {
            usuarioSesion.buscar(auth).ifPresent(usuario -> transferir(sesion, usuario.getId()));
        } catch (RuntimeException e) {
            log.warn("No se pudo transferir la sesion anonima al usuario", e);
        }
    }

    /**
     * Respaldo: VistaGlobalAttributesAdvice lo llama en cada vista con sesion. Si el
     * login no paso por CustomSuccessHandler (o la transferencia fallo), lo anonimo
     * que quede en la sesion se transfiere en la siguiente pagina. Sin restos, no hace nada.
     */
    public void transferirSiQueda(HttpSession sesion, Long usuarioId) {
        if (sesion == null || usuarioId == null || !quedaAlgo(sesion)) return;
        try {
            transferir(sesion, usuarioId);
        } catch (RuntimeException e) {
            log.warn("No se pudo transferir la sesion anonima al usuario {}", usuarioId, e);
        }
    }

    private static boolean quedaAlgo(HttpSession sesion) {
        return sesion.getAttribute(COTIZACIONES) != null
                || sesion.getAttribute(CarritoService.SESION_CARRITO_ANONIMO) != null;
    }

    private void transferir(HttpSession sesion, Long usuarioId) {
        int unidades = carritoService.fusionarCarritoAnonimo(sesion, usuarioId);
        int obras = 0;
        List<String> ids = cotizaciones(sesion);
        if (ids != null) {
            obras = vidrioSostenibleService.reclamarCotizacionesAnonimas(ids, usuarioId);
            sesion.removeAttribute(COTIZACIONES);
        }
        log.info("Sesion anonima -> usuario {}: {} unidad(es) de carrito, {} obra(s)", usuarioId, unidades, obras);
    }
}
