package com.smartglass.service;

import com.smartglass.model.mongo.VidrioSostenible;
import com.smartglass.model.mysql.Pedido;
import com.smartglass.repository.mysql.PedidoRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * T3: dispara notificaciones desde los servicios de negocio SIN modificarlos
 * (PedidoService y la maquina de estados de VidrioSostenibleService quedan igual).
 * Solo actua cuando la operacion termino bien; nunca cambia su resultado.
 */
@Aspect
@Component
public class NotificacionEventosAspect {

    private final NotificacionService notificaciones;
    private final PedidoRepository pedidoRepository;

    public NotificacionEventosAspect(NotificacionService notificaciones, PedidoRepository pedidoRepository) {
        this.notificaciones = notificaciones;
        this.pedidoRepository = pedidoRepository;
    }

    /** Cotizacion nueva, con cuenta o anonima (T1): web y API pasan por aqui. */
    @AfterReturning(pointcut = "execution(* com.smartglass.service.VidrioSostenibleService.generarCotizacion(..))",
            returning = "creada")
    public void cotizacionCreada(Object creada) {
        if (creada instanceof VidrioSostenible v) notificaciones.cotizacionNueva(v);
    }

    /** Aprobacion o rechazo: solo si la maquina de estados acepto la transicion. */
    @AfterReturning(pointcut = "execution(* com.smartglass.service.VidrioSostenibleService.cambiarEstado(String, String)) && args(id, estado)",
            returning = "resultado", argNames = "id,estado,resultado")
    public void cotizacionCambio(String id, String estado, Object resultado) {
        if (resultado == VidrioSostenibleService.ResultadoCambio.OK) notificaciones.cotizacionEstado(id, estado);
    }

    @AfterReturning(pointcut = "execution(* com.smartglass.service.PedidoService.crearPedidoDesdeCarrito(..))",
            returning = "pedido")
    public void pedidoCreado(Object pedido) {
        if (pedido instanceof Pedido p) notificaciones.pedidoNuevo(p);
    }

    /** Cambio de estado de uno o varios pedidos desde el panel: avisa solo los que cambiaron. */
    @Around("execution(* com.smartglass.service.PedidoService.actualizarEstado(Long, String)) "
            + "|| execution(* com.smartglass.service.PedidoService.actualizarEstados(java.util.Map))")
    public Object pedidoEstado(ProceedingJoinPoint pjp) throws Throwable {
        Map<Long, String> pedidos = new HashMap<>();
        Object[] args = pjp.getArgs();
        if (args.length == 2) pedidos.put((Long) args[0], (String) args[1]);
        else if (args[0] instanceof Map<?, ?> m) m.forEach((k, v) -> pedidos.put((Long) k, (String) v));

        Map<Long, String> antes = new HashMap<>();
        pedidos.keySet().forEach(id -> pedidoRepository.findById(id).ifPresent(p -> antes.put(id, p.getEstado())));

        Object resultado = pjp.proceed();

        pedidos.forEach((id, nuevo) -> {
            if (!antes.containsKey(id) || Objects.equals(antes.get(id), nuevo)) return;
            pedidoRepository.findById(id)
                    .filter(p -> Objects.equals(p.getEstado(), nuevo))
                    .ifPresent(p -> notificaciones.pedidoEstado(p, nuevo));
        });
        return resultado;
    }
}
